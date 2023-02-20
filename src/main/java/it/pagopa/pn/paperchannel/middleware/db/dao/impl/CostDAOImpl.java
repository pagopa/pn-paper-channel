package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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


@Repository
@Slf4j
public class CostDAOImpl extends BaseDAO<PnCost> implements CostDAO {


    public CostDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                       DynamoDbAsyncClient dynamoDbAsyncClient,
                       AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbCostTable(), PnCost.class);
    }

    @Override
    public Flux<PnCost> findAllFromTenderCode(String tenderCode, String driverCode) {
        QueryConditional conditional = CONDITION_EQUAL_TO.apply(keyBuild(tenderCode, null));
        String filter = "";
        Map<String,AttributeValue> values = new HashMap<>();
        if (StringUtils.isNotBlank(driverCode)) {
            filter += ":driverCode=" + PnCost.COL_DELIVERY_DRIVER_CODE;
            values.put(":driverCode", AttributeValue.builder().s(driverCode).build());
        }
        return this.getByFilter(conditional, PnCost.TENDER_INDEX, values, filter);
    }

    @Override
    public Mono<List<PnCost>> findAllFromTenderAndProductTypeAndExcludedUUID(String tenderCode, String productType, String uuidExclude) {
        QueryConditional conditional = CONDITION_EQUAL_TO.apply(keyBuild(tenderCode, null));
        String filter = "";
        Map<String,AttributeValue> values = new HashMap<>();
        if (StringUtils.isNotBlank(productType)) {
            filter += ":productType=" + PnCost.COL_PRODUCT_TYPE;
            values.put(":productType", AttributeValue.builder().s(productType).build());
        }
        if (StringUtils.isNotBlank(uuidExclude)){
            filter += (StringUtils.isBlank(filter)) ? "" : " AND NOT ";
            filter += PnCost.COL_UUID + " IN (:uuidCost)";
            values.put(":uuidCost", AttributeValue.builder().s(uuidExclude).build());
        }
        log.info(filter);
        return this.getByFilter(conditional, PnCost.TENDER_INDEX, values, filter).collectList();
    }

    @Override
    public Mono<PnCost> createOrUpdate(PnCost entities) {
        return Mono.fromFuture(put(entities).thenApply(item -> entities));
    }

    @Override
    public Mono<PnCost> getByCapOrZoneAndProductType(String tenderCode, String cap, String zone, String productType) {
        QueryConditional conditionalKey = CONDITION_EQUAL_TO.apply(keyBuild(tenderCode, null));
        String filterExpression = PnCost.COL_PRODUCT_TYPE + " = :productType ";
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":productType", AttributeValue.builder().s(productType).build());

        if (StringUtils.isNotBlank(cap)){
            filterExpression += " AND (contains(" + PnCost.COL_CAP + ", :cap) OR contains("+ PnCost.COL_CAP + ", :defaultCap))";
            values.put(":cap", AttributeValue.builder().s(cap).build());
            values.put(":defaultCap", AttributeValue.builder().s("99999").build());
        } else {
            filterExpression += " AND :zoneAttr = " + PnCost.COL_ZONE;
            values.put(":zoneAttr", AttributeValue.builder().s(zone).build());
        }

        log.info("FILTER : {}", filterExpression);
        return this.getByFilter( conditionalKey, PnCost.TENDER_INDEX, values, filterExpression)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.empty();
                    }
                    List<PnCost> driverCost = items.stream().filter(cost -> !cost.getFsu()).toList();
                    if (!driverCost.isEmpty()){
                        return Mono.just(driverCost.get(0));
                    }
                    return Mono.just(items.get(0));
                });
    }
}
