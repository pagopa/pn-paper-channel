package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COST_NOT_FOUND;

@Repository
@Slf4j
public class CostDAOImpl extends BaseDAO<PnCost> implements CostDAO {

    private final DynamoDbAsyncTable<PnDeliveryDriver> deliveryDriverTable;
    private final DynamoDbAsyncTable<PnTender> tenderTable;

    private final TransactWriterInitializer transactWriterInitializer;

    public CostDAOImpl(KmsEncryption kmsEncryption,
                       DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                       DynamoDbAsyncClient dynamoDbAsyncClient,
                       AwsPropertiesConfig awsPropertiesConfig, TransactWriterInitializer transactWriterInitializer) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbCostTable(), PnCost.class);
        this.deliveryDriverTable = dynamoDbEnhancedAsyncClient.table(awsPropertiesConfig.getDynamodbDeliveryDriverTable(), TableSchema.fromBean(PnDeliveryDriver.class));
        this.transactWriterInitializer = transactWriterInitializer;
        this.tenderTable = dynamoDbEnhancedAsyncClient.table(awsPropertiesConfig.getDynamodbTenderTable(), TableSchema.fromBean(PnTender.class));
    }

    public Mono<PnTender> createNewContract(Map<PnDeliveryDriver, List<PnCost>> deliveriesAndCost, PnTender tender) {
        this.transactWriterInitializer.init();
        transactWriterInitializer.addRequestTransaction(tenderTable, tender, PnTender.class);
        List<PnDeliveryDriver> deliveries = deliveriesAndCost.keySet().stream().toList();
        deliveries.forEach(delivery -> {
            delivery.setStartDate(Instant.now());
            delivery.setAuthor("PN-PAPER-CHANNEL");
            transactWriterInitializer.addRequestTransaction(deliveryDriverTable, delivery, PnDeliveryDriver.class);
            List<PnCost> costs = deliveriesAndCost.get(delivery);
            costs.forEach(cost -> transactWriterInitializer.addRequestTransaction(this.dynamoTable, cost, PnCost.class));
        });

        return Mono.fromFuture(putWithTransact(transactWriterInitializer.build()).thenApply(item -> tender));
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
