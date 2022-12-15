package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;

@Repository
@Slf4j
@Import(PnAuditLogBuilder.class)
public class RequestDeliveryDAOImpl extends BaseDAO<RequestDeliveryEntity> implements RequestDeliveryDAO {

    public RequestDeliveryDAOImpl(PnAuditLogBuilder auditLogBuilder,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  AwsPropertiesConfig awsPropertiesConfig) {
        super(auditLogBuilder, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbRequestDeliveryTable(), RequestDeliveryEntity.class);
    }

    @Override
    public Mono<RequestDeliveryEntity> create(RequestDeliveryEntity requestDeliveryEntity) {
        String logMessage = String.format("create request delivery = %s", requestDeliveryEntity);
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();

        return Mono.fromFuture(
                countOccurrencesEntity(requestDeliveryEntity.getRequestId())
                        .thenCompose( total -> {
                                log.info("Total elements : {}", total);
                                if (total == 0){
                                    return put(requestDeliveryEntity);
                                } else {
                                    throw new PnHttpResponseException("Data already existed", HttpStatus.BAD_REQUEST.value());
                                }
                            })
                )
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                })
                .map(entityCreated -> {
                    logEvent.generateSuccess(String.format("created request delivery = %s", entityCreated)).log();
                    return entityCreated;
                });
    }

    @Override
    public Mono<RequestDeliveryEntity> updateData(RequestDeliveryEntity requestDeliveryEntity) {
        String logMessage = String.format("Update request delivery = %s", requestDeliveryEntity);
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();
        return Mono.fromFuture(this.update(requestDeliveryEntity).thenApply(item -> {
            logEvent.generateSuccess("Update successfully").log();
            return item;
        }));
    }

    @Override
    public Mono<RequestDeliveryEntity> getByRequestId(String requestId) {
        String logMessage = String.format("Find request delivery with %s", requestId);
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();
        return Mono.fromFuture(this.get(requestId, null).thenApply(item -> {
                    logEvent.generateSuccess(String.format("request delivery = %s", item)).log();
                    if (item == null) throw new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage(),HttpStatus.NOT_FOUND);
                    return item;
                }));
    }

    @Override
    public Mono<RequestDeliveryEntity> getByCorrelationId(String correlationId) {
        return this.getBySecondaryIndex(RequestDeliveryEntity.CORRELATION_INDEX, correlationId, null)
                .collectList()
                .map(item -> item.get(0));
    }

    @Override
    public Flux<RequestDeliveryEntity> getByFiscalCode(String fiscalCode) {
        return this.getBySecondaryIndex(RequestDeliveryEntity.FISCAL_CODE_INDEX, fiscalCode, null);
    }

    private CompletableFuture<Integer> countOccurrencesEntity(String requestId){
        String keyConditionExpression = RequestDeliveryEntity.COL_REQUEST_ID + " = :requestId";
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":requestId",  AttributeValue.builder().s(requestId).build());
        return this.getCounterQuery(expressionValues, "", keyConditionExpression);
    }
}