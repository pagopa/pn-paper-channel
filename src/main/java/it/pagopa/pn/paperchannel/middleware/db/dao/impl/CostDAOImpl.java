package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COST_NOT_FOUND;

public class CostDAOImpl extends BaseDAO<PnPaperCost> implements CostDAO {
    public CostDAOImpl(PnAuditLogBuilder auditLogBuilder,
                       KmsEncryption kmsEncryption,
                       DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                       DynamoDbAsyncClient dynamoDbAsyncClient,
                       AwsPropertiesConfig awsPropertiesConfig) {
        super(auditLogBuilder, kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbCostTable(), PnPaperCost.class);
    }

    @Override
    public Mono<PnPaperCost> getByCapOrZoneAndProductType(String cap, PnZone zone, String productType) {
        String value = cap;
        String index = PnPaperCost.COL_CAP_INDEX;
        if (zone != null){
            value = zone.getZone();
            index = PnPaperCost.COL_ZONE_INDEX;
        }
        String filterExpression = "("+ PnPaperCost.COL_PRODUCT_TYPE + " = :productType)";
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":productType", AttributeValue.builder().s(productType).build());
        return this.getByFilter(value,null, index, values, filterExpression)
                .collectList()
                .map(items->{
                    if (items.isEmpty()){
                        throw new PnGenericException(COST_NOT_FOUND,COST_NOT_FOUND.getMessage());
                    }
                    return items.get(0);
                });
    }
}
