package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Repository
public class EventDematDAOImpl extends BaseDAO<PnEventDemat> implements EventDematDAO {
    public EventDematDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                            DynamoDbAsyncClient dynamoDbAsyncClient,
                            AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbPaperEventsTable(), PnEventDemat.class);
    }

    @Override
    public Mono<PnEventDemat> createOrUpdate(PnEventDemat pnEventDemat) {
        return Mono.fromFuture(put(pnEventDemat).thenApply(item -> item));
    }

    @Override
    public Mono<PnEventDemat> getDeliveryEventDemat(String dematRequestId, String documentTypeStatusCode) {
        return Mono.fromFuture(this.get(dematRequestId, documentTypeStatusCode).thenApply(item -> item));
    }

    @Override
    public Flux<PnEventDemat> findAllByRequestId(String dematRequestId) {
        QueryConditional keyConditional = CONDITION_EQUAL_TO.apply(Key.builder().partitionValue(dematRequestId).build());
        return getByFilter(keyConditional, null, null, null);
    }

    @Override
    public Mono<PnEventDemat> deleteEventDemat(String dematRequestId, String documentTypeStatusCode) {
        return Mono.fromFuture(this.delete(dematRequestId, documentTypeStatusCode).thenApply(item -> item));
    }

    @Override
    public Flux<PnEventDemat> findAllByKeys(String dematRequestId, String... documentTypeStatusCode) {
        return super.findAllByKeys(dematRequestId, documentTypeStatusCode);
    }

}
