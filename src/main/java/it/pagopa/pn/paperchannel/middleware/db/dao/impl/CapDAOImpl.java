package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CapDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COUNTRY_NOT_FOUND;

@Repository
@Import(PnAuditLogBuilder.class)
public class CapDAOImpl extends BaseDAO<PnCap> implements CapDAO {

    public CapDAOImpl(KmsEncryption kmsEncryption,
                      DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                      DynamoDbAsyncClient dynamoDbAsyncClient,
                      AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbZoneTable(), PnCap.class);
    }

    @Override
    public Mono<List<PnCap>> getAllCap(String cap) {
        QueryConditional conditional = CONDITION_BEGINS_WITH.apply(keyBuild(cap, null));
        if (StringUtils.isNotBlank(cap)){
            String filter = ":cap=" + PnCap.COL_CAP;
            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":cap", AttributeValue.builder().s(cap).build());
            return this.getByFilter(conditional, PnCap.AUTHOR_INDEX, values, filter).collectList();
        }
        return this.getByFilter(conditional, PnCap.AUTHOR_INDEX, null, null).collectList();
    }
}
