package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COST_NOT_FOUND;

@Repository
@Slf4j
public class CostDAOImpl extends BaseDAO<PnCost> implements CostDAO {

    private final TransactWriterInitializer transactWriterInitializer;

    public CostDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                       DynamoDbAsyncClient dynamoDbAsyncClient,
                       AwsPropertiesConfig awsPropertiesConfig, TransactWriterInitializer transactWriterInitializer) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbCostTable(), PnCost.class);
        this.transactWriterInitializer = transactWriterInitializer;
    }

    @Override
    public Mono<List<PnCost>> retrievePrice(String tenderCode, String deliveryDriver) {
        QueryConditional conditional = CONDITION_EQUAL_TO.apply(keyBuild(tenderCode, null));
        if (StringUtils.isNotBlank(deliveryDriver)){
            String filter = ":deliveryDriver=" + PnCost.COL_ID_DELIVERY_DRIVER;
            Map<String,AttributeValue> values = new HashMap<>();
            values.put(":deliveryDriver", AttributeValue.builder().s(deliveryDriver).build());
            return this.getByFilter(conditional, PnCost.TENDER_INDEX, values, filter).collectList();
        }
        return this.getByFilter(conditional, PnCost.TENDER_INDEX, null, null).collectList();
    }

    @Override
    public Mono<List<PnCost>> retrievePrice(String tenderCode, String deliveryDriver, Boolean isNational) {
        QueryConditional conditional = CONDITION_EQUAL_TO.apply(keyBuild(tenderCode, null));

        String filter = "";
        Map<String,AttributeValue> values = new HashMap<>();
        if (StringUtils.isNotBlank(deliveryDriver)) {
            filter += ":deliveryDriver=" + PnCost.COL_ID_DELIVERY_DRIVER + " ";
            values.put(":deliveryDriver", AttributeValue.builder().s(deliveryDriver).build());
        }
        if (isNational != null){
            if (isNational) {
                filter += ":nullable <> " + PnCost.COL_CAP + " ";
            } else {
                filter += ":nullable <> " + PnCost.COL_ZONE + " ";
            }
            values.put(":nullable", AttributeValue.builder().nul(true).build());
        }

        return this.getByFilter(conditional, PnCost.TENDER_INDEX, values, filter).collectList();
    }

    @Override
    public Mono<List<PnCost>> createOrUpdate(List<PnCost> entities) {
        this.transactWriterInitializer.init();
        entities.forEach(cost ->
            transactWriterInitializer.addRequestTransaction(this.dynamoTable, cost, PnCost.class)
        );
        return Mono.fromFuture(putWithTransact(transactWriterInitializer.build()).thenApply(item -> entities));
    }

    @Override
    public Mono<PnCost> getByCapOrZoneAndProductType(String cap, String zone, String productType) {
        String value = "";
        String index = PnCost.CAP_INDEX;
        if (cap != null && zone == null){
            value = cap;
            index = PnCost.CAP_INDEX;
        }
        else if (zone != null && cap == null) {
            value = zone;
            index = PnCost.ZONE_INDEX; // remove. nuova variable col = zone/cap
        }
        String filterExpression = "(" + PnCost.COL_PRODUCT_TYPE + " = :productType)"; // adds filter cap or zone
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":productType", AttributeValue.builder().s(productType).build());
        return this.getByFilter( CONDITION_EQUAL_TO.apply( keyBuild(value, "") ), index, values, filterExpression)
                .collectList()
                .map(items -> {
                    if (items.isEmpty()) {
                        throw new PnGenericException(COST_NOT_FOUND, COST_NOT_FOUND.getMessage());
                    }
                    return items.get(0);
                });
    }
}
