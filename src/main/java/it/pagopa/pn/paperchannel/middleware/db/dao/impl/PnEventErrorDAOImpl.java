package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsConfig;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;
import java.util.function.Function;

@Repository
@Slf4j
public class PnEventErrorDAOImpl extends BaseDAO<PnEventError> implements PnEventErrorDAO {

    public PnEventErrorDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                               DynamoDbAsyncClient dynamoDbAsyncClient,
                               AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbPaperEventErrorTable(), PnEventError.class);
    }

    @Override
    public Flux<PnEventError> findEventErrorsByRequestId(String requestId) {
        QueryConditional keyConditional = CONDITION_EQUAL_TO.apply(Key.builder().partitionValue(requestId).build());
        return getByFilter(keyConditional, null, null, null);
    }

    @Override
    public Mono<PnEventError> putItem(PnEventError pnEventError) {
        return Mono.fromFuture(super.put(pnEventError).thenApply(Function.identity()));
    }

    @Override
    public Mono<PnEventError> deleteItem(String requestId, Instant statusBusinessDateTime) {
        return Mono.fromFuture(super.delete(requestId, statusBusinessDateTime.toString()).thenApply(Function.identity()));
    }
}
