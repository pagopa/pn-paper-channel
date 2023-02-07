package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DeliveryDriverDAOImpl extends BaseDAO<PnDeliveryDriver> implements DeliveryDriverDAO {


    public DeliveryDriverDAOImpl(KmsEncryption kmsEncryption,
                                 DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                 DynamoDbAsyncClient dynamoDbAsyncClient,
                                 AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbDeliveryDriverTable(), PnDeliveryDriver.class);
    }

    @Override
    public Mono<List<PnDeliveryDriver>> getDeliveryDriver(String tenderCode) {
        Pair<Instant, Instant> startAndEndTimestamp = DateUtils.getStartAndEndTimestamp(null, null);

        QueryConditional conditional = CONDITION_BETWEEN.apply(
                new Keys(keyBuild(Const.PN_PAPER_CHANNEL, startAndEndTimestamp.getFirst().toString()),
                        keyBuild(Const.PN_PAPER_CHANNEL, startAndEndTimestamp.getSecond().toString()) )
        );

        String filter = "( " + PnDeliveryDriver.COL_TENDER_CODE + " = :tenderCode )";
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":tenderCode", AttributeValue.builder().s(tenderCode).build());

        return this.getByFilter(conditional, PnDeliveryDriver.AUTHOR_INDEX, values, filter)
                .collectList();
    }


    @Override
    public Mono<PnDeliveryDriver> createOrUpdate(PnDeliveryDriver data) {
        data.setAuthor(Const.PN_PAPER_CHANNEL);
        data.setStartDate(Instant.now());
        return Mono.fromFuture(this.put(data).thenApply(i -> data));
    }
}
