package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Repository
@Slf4j
public class RequestDeliveryDAOImpl extends BaseDAO<PnDeliveryRequest> implements RequestDeliveryDAO {

    @Autowired
    @Qualifier("dataVaultEncryption")
    private DataEncryption dataVaultEncryption;
    @Autowired
    private AddressDAO addressDAO;
    private final TransactWriterInitializer transactWriterInitializer;


    public RequestDeliveryDAOImpl(DataEncryption dataVaultEncryption,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  AwsPropertiesConfig awsPropertiesConfig, TransactWriterInitializer transactWriterInitializer) {
        super(dataVaultEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbRequestDeliveryTable(), PnDeliveryRequest.class);
        this.transactWriterInitializer = transactWriterInitializer;
    }

    @Override
    public Mono<PnDeliveryRequest> createWithAddress(PnDeliveryRequest request, PnAddress pnAddress) {
            return Mono.fromFuture(
                        countOccurrencesEntity(request.getRequestId())
                                .thenCompose( total -> {
                                    log.debug("Delivery request with same request id : {}", total);
                                    if (total == 0){
                                        try {
                                            this.transactWriterInitializer.init();
                                            if(pnAddress != null) {
                                                addressDAO.createTransaction(this.transactWriterInitializer, pnAddress);
                                            }
                                            request.setFiscalCode(encode(request, PnDeliveryRequest.class).getFiscalCode());
                                            transactWriterInitializer.addRequestTransaction(this.dynamoTable, request, PnDeliveryRequest.class);
                                            return putWithTransact(transactWriterInitializer.build()).thenApply(item-> request);
                                        } catch (TransactionCanceledException tce) {
                                            log.error("Transaction Canceled" + tce.getMessage());
                                            return null;
                                        }
                                    } else {
                                        throw new PnHttpResponseException("Data already existed", HttpStatus.BAD_REQUEST.value());
                                    }
                                })
                )
                .onErrorResume(throwable -> {
                    throwable.printStackTrace();
                    return Mono.error(throwable);
                })
                .map(entityCreated -> entityCreated);
    }

    @Override
    public Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest) {
        return Mono.fromFuture(this.update(pnDeliveryRequest).thenApply(item -> item));
    }

    @Override
    public Mono<PnDeliveryRequest> getByRequestId(String requestId) {
        return Mono.fromFuture(this.get(requestId, null).thenApply(item -> item));
    }

    @Override
    public Mono<PnDeliveryRequest> getByCorrelationId(String correlationId) {
        return this.getBySecondaryIndex(PnDeliveryRequest.CORRELATION_INDEX, correlationId, null)
                .collectList()
                .map(item -> item.get(0));
    }

    @Override
    public Flux<PnDeliveryRequest> getByFiscalCode(String fiscalCode) {
        return this.getBySecondaryIndex(PnDeliveryRequest.FISCAL_CODE_INDEX, fiscalCode, null);
    }

    private CompletableFuture<Integer> countOccurrencesEntity(String requestId){
        String keyConditionExpression = PnDeliveryRequest.COL_REQUEST_ID + " = :requestId";
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":requestId",  AttributeValue.builder().s(requestId).build());
        return this.getCounterQuery(expressionValues, "", keyConditionExpression);
    }

}