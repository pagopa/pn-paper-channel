package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
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


    public RequestDeliveryDAOImpl(DataEncryption dataVaultEncryption,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  AwsPropertiesConfig awsPropertiesConfig) {
        super(dataVaultEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbRequestDeliveryTable(), PnDeliveryRequest.class);
    }


    @Override
    public Mono<PnDeliveryRequest> createWithAddress(PnDeliveryRequest request, PnAddress pnAddress, PnAddress discoveredAddress) {
        String fiscalCode = request.getFiscalCode();
        return Mono.fromFuture(countOccurrencesEntity(request.getRequestId())
                        .thenCompose( total -> {
                            log.debug("Delivery request with same request id: {} and with reworkNeeded: {}", total, request.getReworkNeeded());
                            if ( total == 0 || Boolean.TRUE.equals(request.getReworkNeeded())) {
                                try {
                                    TransactWriteItemsEnhancedRequest.Builder builder =
                                            TransactWriteItemsEnhancedRequest.builder();

                                    if(pnAddress != null) {
                                        addressDAO.createTransaction(builder, pnAddress);
                                    }

                                    if (discoveredAddress != null) {
                                        discoveredAddress.setTypology(AddressTypeEnum.DISCOVERED_ADDRESS.name());
                                        addressDAO.createTransaction(builder, discoveredAddress);
                                    }

                                    request.setHashedFiscalCode(Utility.convertToHash(request.getFiscalCode()));
                                    TransactPutItemEnhancedRequest<PnDeliveryRequest> requestEntity =
                                            TransactPutItemEnhancedRequest.builder(PnDeliveryRequest.class)
                                                    .item(encode(request))
                                                    .build();

                                    builder.addPutItem(this.dynamoTable, requestEntity);
                                    return putWithTransact(builder.build()).thenApply(item-> {
                                        request.setFiscalCode(fiscalCode);
                                        return request;
                                    });
                                } catch (TransactionCanceledException tce) {
                                    log.error("Transaction Canceled {}", tce.getMessage());
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
                });
    }

    @Override
    public Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest) {
        return updateData(pnDeliveryRequest, false);
    }

    @Override
    public Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest, boolean ignorableNulls) {
        return Mono.fromFuture(this.dynamoTable.getItem(pnDeliveryRequest).thenApply(item -> item))
            .flatMap(entityDB -> {
                pnDeliveryRequest.setFiscalCode(entityDB.getFiscalCode());
                return Mono.fromFuture(this.update(pnDeliveryRequest, ignorableNulls).thenApply(saved -> pnDeliveryRequest));
            });
    }

    @Override
    public Mono<PnDeliveryRequest> getByRequestId(String requestId) {
        return Mono.fromFuture(this.dynamoTable.getItem(keyBuild(requestId, null)).thenApply(item -> item));
    }

    @Override
    public Mono<PnDeliveryRequest> getByRequestId(String requestId, boolean decode) {
        return this.getByRequestId(requestId).map(entity -> {
            if (decode) return this.decode(entity);
            return entity;
        });
    }

    @Override
    public Mono<PnDeliveryRequest> getByCorrelationId(String requestId, boolean decode) {
        return this.getByCorrelationId(requestId).map(entity -> {
            if (decode) return this.decode(entity);
            return entity;
        });
    }

    @Override
    public Mono<PnDeliveryRequest> getByCorrelationId(String correlationId) {
        return this.getBySecondaryIndex(PnDeliveryRequest.CORRELATION_INDEX, correlationId, null)
                .collectList()
                .flatMap(item -> {
                    if (item.isEmpty()) return Mono.empty();
                    return Mono.just(item.get(0));
                });
    }



    private CompletableFuture<Integer> countOccurrencesEntity(String requestId){
        String keyConditionExpression = PnDeliveryRequest.COL_REQUEST_ID + " = :requestId";
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":requestId",  AttributeValue.builder().s(requestId).build());
        return this.getCounterQuery(expressionValues, "", keyConditionExpression);
    }

}