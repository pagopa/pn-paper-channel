package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnPaperTenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelTender;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.time.Instant;
import java.util.Comparator;


@Repository
public class PnPaperTenderDAOImpl extends BaseDAO<PnPaperChannelTender> implements PnPaperTenderDAO {

    public PnPaperTenderDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                DynamoDbAsyncClient dynamoDbAsyncClient, AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbPaperChannelTenderTable(), PnPaperChannelTender.class);
    }


    @Override
    public Mono<PnPaperChannelTender> createOrUpdate(PnPaperChannelTender pnTender) {
        return Mono.fromFuture(put(pnTender).thenApply(item -> item));
    }


    /**
     * Retrieve the active tender
     *
     * @return  the entity of tender
     **/
    @Override
    public Mono<PnPaperChannelTender> getActiveTender() {
        Expression filterExpression = Expression.builder()
                .expression("activationDate < :now")
                .putExpressionValue(":now", AttributeValue.builder().s(Instant.now().toString()).build())
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return Flux.from(super.dynamoTable.scan(scanRequest).flatMapIterable(Page::items))
                .sort(Comparator.comparing(PnPaperChannelTender::getActivationDate).reversed())
                .next();
    }


    /**
     * Retrieve a specific tender
     *
     * @param tenderId  the id of a tender
     *
     * @return          the entity of tender
     **/
    @Override
    public Mono<PnPaperChannelTender> getTenderById(String tenderId) {
        QueryConditional keyConditional = CONDITION_BETWEEN.apply(
                new Keys(
                        Key.builder()
                                .partitionValue(tenderId)
                                .sortValue(Instant.EPOCH.toString())  // La data minima possibile
                                .build(),
                        Key.builder()
                                .partitionValue(tenderId)
                                .sortValue(Instant.now().toString())  // La data corrente
                                .build()
                )
        );


        return super.getByFilter(keyConditional, null, null, null, null, false)
                .sort(Comparator.comparing(PnPaperChannelTender::getActivationDate).reversed())
                .next();
    }
}
