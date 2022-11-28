package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
                                log.debug("Total elements : {}", total);
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
    public Mono<RequestDeliveryEntity> getByRequestId(String requestId) {
      // QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional().build()

        return null;
    }

    @Override
    public Flux<RequestDeliveryEntity> getByFiscalCode(String fiscalCode) {
        return null;
    }

    private CompletableFuture<Integer> countOccurrencesEntity(String requestId){
        String keyConditionExpression = RequestDeliveryEntity.COL_REQUEST_ID + " = :requestId";
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":requestId",  AttributeValue.builder().s(requestId).build());
        return this.getCounterQuery(expressionValues, "", keyConditionExpression);
    }
}