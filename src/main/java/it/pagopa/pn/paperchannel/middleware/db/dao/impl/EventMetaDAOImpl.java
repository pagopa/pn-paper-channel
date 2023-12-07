package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import static it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore.ATTRIBUTE_NOT_EXISTS;

@Repository
@Slf4j
public class EventMetaDAOImpl extends BaseDAO<PnEventMeta> implements EventMetaDAO {
    public EventMetaDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                       DynamoDbAsyncClient dynamoDbAsyncClient,
                       AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbPaperEventsTable(), PnEventMeta.class);
    }

    @Override
    public Mono<PnEventMeta> createOrUpdate(PnEventMeta pnEventMeta) {
        return Mono.fromFuture(put(pnEventMeta).thenApply(item -> item));
    }

    @Override
    public Mono<PnEventMeta> putIfAbsent(PnEventMeta pnEventMeta) {
        String expression = String.format(
                "%s(%s) AND %s(%s)",
                ATTRIBUTE_NOT_EXISTS,
                PnEventMeta.COL_PK,
                ATTRIBUTE_NOT_EXISTS,
                PnEventMeta.COL_SK
        );

        Expression conditionExpressionPut = Expression.builder()
                .expression(expression)
                .build();

        PutItemEnhancedRequest<PnEventMeta> request = PutItemEnhancedRequest.builder( PnEventMeta.class )
                .item(pnEventMeta )
                .conditionExpression( conditionExpressionPut )
                .build();

        return Mono.fromFuture(put(request)).thenReturn(pnEventMeta)
                .onErrorResume(ConditionalCheckFailedException.class, e -> {
                            log.warn("ConditionalCheckFailed for putting entity: {}", pnEventMeta);
                            return Mono.empty();
                        }
                );
    }

    @Override
    public Mono<PnEventMeta> getDeliveryEventMeta(String metaRequestId, String metaStatusCode) {
        return Mono.fromFuture(this.get(metaRequestId, metaStatusCode).thenApply(item -> item));
    }

    @Override
    public Flux<PnEventMeta> findAllByRequestId(String metaRequestId) {
        QueryConditional keyConditional = CONDITION_EQUAL_TO.apply(Key.builder().partitionValue(metaRequestId).build());
        return getByFilter(keyConditional, null, null, null);
    }

    @Override
    public Mono<PnEventMeta> deleteEventMeta(String metaRequestId, String metaStatusCode) {
        return Mono.fromFuture(this.delete(metaRequestId, metaStatusCode).thenApply(item -> item));
    }

    @Override
    public Mono<Void> deleteBatch(String metaRequestId, String... metaStatusCodes) {
        return super.deleteBatch(metaRequestId, metaStatusCodes);
    }

}
