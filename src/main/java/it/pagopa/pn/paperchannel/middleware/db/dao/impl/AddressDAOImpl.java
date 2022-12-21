package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
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

public class AddressDAOImpl extends BaseDAO <PnAddress> implements AddressDAO {

    public AddressDAOImpl(PnAuditLogBuilder auditLogBuilder,
                                  KmsEncryption kmsEncryption,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  AwsPropertiesConfig awsPropertiesConfig) {
        super(auditLogBuilder, kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbAddressTable(), PnAddress.class);
    }

    @Override
    public Mono<PnAddress> create(PnAddress pnAddress) {
        String logMessage = String.format("create request delivery = %s", pnAddress);
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();

        return Mono.fromFuture(
                countOccurrencesEntity(pnAddress.getRequestId())
                        .thenCompose( total -> {
                            log.debug("Total elements : {}", total);
                            if (total == 0){
                                return put(pnAddress);
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
    public Mono<PnAddress> findByRequestId(String requestId) {
        String logMessage = String.format("Find request delivery with %s", requestId);
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();
        return Mono.fromFuture(this.get(requestId, null).thenApply(item -> {
            logEvent.generateSuccess(String.format("address = %s", item)).log();
            return item;
        }));
    }

    private CompletableFuture<Integer> countOccurrencesEntity(String requestId) {
        String keyConditionExpression = PnDeliveryRequest.COL_REQUEST_ID + " = :requestId";
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":requestId",  AttributeValue.builder().s(requestId).build());
        return this.getCounterQuery(expressionValues, "", keyConditionExpression);
    }

}
