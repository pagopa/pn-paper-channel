package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.CapDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.utils.Const;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.util.List;

@Repository
public class CapDAOImpl extends BaseDAO<PnCap> implements CapDAO {

    public CapDAOImpl(KmsEncryption kmsEncryption,
                      DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                      DynamoDbAsyncClient dynamoDbAsyncClient,
                      AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbCapTable(), PnCap.class);
    }

    @Override
    public Mono<List<PnCap>> getAllCap(String cap) {
        QueryConditional conditional = CONDITION_EQUAL_TO.apply(keyBuild(Const.PN_PAPER_CHANNEL, cap));
        if (StringUtils.isNotEmpty(cap)) {
            conditional = CONDITION_BEGINS_WITH.apply(keyBuild(Const.PN_PAPER_CHANNEL, cap));
        }
        return this.getByFilter(conditional, null, null, null, Const.maxElements).collectList();
    }
}
