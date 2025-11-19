package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnAttachmentsConfigDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
@Slf4j
public class PnAttachmentsConfigDAOImpl extends BaseDAO<PnAttachmentsConfig> implements PnAttachmentsConfigDAO {

    public PnAttachmentsConfigDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                      DynamoDbAsyncClient dynamoDbAsyncClient,
                                      AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbAttachmentsConfigTable(), PnAttachmentsConfig.class);
    }

    /**
     * @param configKey the partitionKey
     * @param dateValidity startValidity <=  dateValidity < endValidity
     * @return the found configuration, or Mono.empty
     */
    @Override
    public Mono<PnAttachmentsConfig> findConfigInInterval(String configKey, Instant dateValidity) {
        Key.Builder keyBuilder = Key.builder().partitionValue(configKey).sortValue(dateValidity.toString());

        String filter = PnAttachmentsConfig.COL_END_VALIDITY + " > :dateValidity" +
                " OR attribute_not_exists(endValidity)";

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":dateValidity", AttributeValue.builder().s(dateValidity.toString()).build());

        final QueryConditional queryConditional = QueryConditional.sortLessThanOrEqualTo(keyBuilder.build());

        return getByFilter(queryConditional, null, values, filter).next();
    }

    /**
     * @param pnAttachmentsConfig the entity to be saved
     * @return the saved entity
     */
    @Override
    public Mono<PnAttachmentsConfig> putItem(PnAttachmentsConfig pnAttachmentsConfig) {
        return Mono.fromFuture(super.put(pnAttachmentsConfig));
    }

    /**
     *
     * @param configKey the partitionKey that is used to retrieve all the records with that key and then delete them
     * @return the found configurations, or Flux.empty
     */
    @Override
    public Flux<PnAttachmentsConfig> findAllByConfigKey(String configKey) {
        Key.Builder keyBuilder = Key.builder().partitionValue(configKey);
        return Flux.from(dynamoTable.query(QueryConditional.keyEqualTo(keyBuilder.build()))
                .flatMapIterable(Page::items))
                .doOnNext(items -> log.info("retrieved attachmentconfig={}", items));
    }


}
