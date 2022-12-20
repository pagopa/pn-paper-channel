package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
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
@Import(PnAuditLogBuilder.class)
public class RequestDeliveryDAOImpl extends BaseDAO<PnDeliveryRequest> implements RequestDeliveryDAO {


    private final DynamoDbAsyncTable<PnAddress> addressTable;
    private final TransactWriterInitializer transactWriterInitializer;
    public RequestDeliveryDAOImpl(PnAuditLogBuilder auditLogBuilder,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  AwsPropertiesConfig awsPropertiesConfig, TransactWriterInitializer transactWriterInitializer) {
        super(auditLogBuilder, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbRequestDeliveryTable(), PnDeliveryRequest.class);
        this.transactWriterInitializer = transactWriterInitializer;
        this.addressTable = dynamoDbEnhancedAsyncClient.table(awsPropertiesConfig.getDynamodbAddressTable(), TableSchema.fromBean(PnAddress.class));

    }

    @Override
    public Mono<PnDeliveryRequest> createWithAddress(PnDeliveryRequest request, PnAddress pnAddress) {

        String logMessage = String.format("create request delivery and address= %s", request);
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();

        return Mono.fromFuture(
                        countOccurrencesEntity(request.getRequestId())
                                .thenCompose( total -> {
                                    log.debug("Total elements : {}", total);
                                    if (total == 0){
                                        try {
                                            this.transactWriterInitializer.init();
                                            if(pnAddress != null) {
                                                transactWriterInitializer.addRequestTransaction(addressTable, pnAddress, PnAddress.class);
                                            }

                                            transactWriterInitializer.addRequestTransaction(this.dynamoTable, request, PnDeliveryRequest.class);
                                            return putWithTransact(transactWriterInitializer.build()).thenApply(item-> request);
                                        } catch (TransactionCanceledException tce) {
                                            log.error("Transaction Canceled" + tce.getMessage());
                                        }
                                        return null;

                                    } else {
                                        throw new PnHttpResponseException("Data already existed", HttpStatus.BAD_REQUEST.value());
                                    }
                                })
                )
                .onErrorResume(throwable -> {
                    throwable.printStackTrace();
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                })
                .map(entityCreated -> {
                    logEvent.generateSuccess(String.format("created request delivery and address = %s", entityCreated)).log();
                    return entityCreated;
                });
    }

    @Override
    public Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest) {
        String logMessage = String.format("Update request delivery = %s", pnDeliveryRequest);
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();
        return Mono.fromFuture(this.update(pnDeliveryRequest).thenApply(item -> {
            logEvent.generateSuccess("Update successfully").log();
            return item;
        }));
    }

    @Override
    public Mono<PnDeliveryRequest> getByRequestId(String requestId) {
        String logMessage = String.format("Find request delivery with %s", requestId);
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();
        return Mono.fromFuture(this.get(requestId, null).thenApply(item -> {
                    logEvent.generateSuccess(String.format("request delivery = %s", item)).log();
                    return item;
                }));
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