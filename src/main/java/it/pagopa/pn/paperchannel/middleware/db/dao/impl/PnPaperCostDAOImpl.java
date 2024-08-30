package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnPaperCostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelCost;
import lombok.val;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;



@Repository
public class PnPaperCostDAOImpl extends BaseDAO<PnPaperChannelCost> implements PnPaperCostDAO {


    public PnPaperCostDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                              DynamoDbAsyncClient dynamoDbAsyncClient,
                              AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbPaperChannelCostTable(), PnPaperChannelCost.class);
    }


    @Override
    public Mono<PnPaperChannelCost> createOrUpdate(PnPaperChannelCost pnPaperChannelCost) {
        return Mono.fromFuture(put(pnPaperChannelCost).thenApply(item -> item));
    }

    @Override
    public Mono<PnPaperChannelCost> getCostFrom(String tenderId, String product, String lot, String zone) {

        val paperChannelCost = new PnPaperChannelCost(tenderId, product, lot, zone);

        return Mono.fromFuture(
                super.get(paperChannelCost.getTenderId(), paperChannelCost.getProductLotZone())
                        .thenApply(item -> item));
    }

}
