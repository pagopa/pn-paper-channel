package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import lombok.extern.slf4j.Slf4j;
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
public class CostDAOImpl extends BaseDAO<PnPaperCost> implements CostDAO {

    private final DynamoDbAsyncTable<PnPaperDeliveryDriver> deliveryDriverTable;
    private final TransactWriterInitializer transactWriterInitializer;

    public CostDAOImpl(KmsEncryption kmsEncryption,
                       DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                       DynamoDbAsyncClient dynamoDbAsyncClient,
                       AwsPropertiesConfig awsPropertiesConfig, TransactWriterInitializer transactWriterInitializer) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbCostTable(), PnPaperCost.class);
        this.deliveryDriverTable = dynamoDbEnhancedAsyncClient.table(awsPropertiesConfig.getDynamodbDeliveryDriverTable(), TableSchema.fromBean(PnPaperDeliveryDriver.class));
        this.transactWriterInitializer = transactWriterInitializer;
    }

    public Mono<PnPaperDeliveryDriver> createNewContract(PnPaperDeliveryDriver pnDeliveryDriver, List<PnPaperCost> pnListCosts) {
        this.transactWriterInitializer.init();
        if (pnDeliveryDriver != null) {
            pnDeliveryDriver.setStartDate(Instant.now());
            transactWriterInitializer.addRequestTransaction(deliveryDriverTable, pnDeliveryDriver, PnPaperDeliveryDriver.class);
        }
        pnListCosts.forEach(cost -> transactWriterInitializer.addRequestTransaction(this.dynamoTable, cost, PnPaperCost.class));
        return Mono.fromFuture(putWithTransact(transactWriterInitializer.build()).thenApply(item -> pnDeliveryDriver));

    }

    @Override
    public Mono<List<PnPaperCost>> retrievePrice(String tenderCode, String deliveryDriver) {
        QueryConditional conditional = CONDITION_EQUAL_TO.apply(keyBuild(tenderCode, null));
        String filter = ":deliveryDriver=" + PnPaperCost.COL_ID_DELIVERY_DRIVER;
        Map<String,AttributeValue> values = new HashMap<>();
        values.put(":deliveryDriver", AttributeValue.builder().s(deliveryDriver).build());
        return this.getByFilter(conditional, PnPaperCost.TENDER_INDEX, values, filter).collectList();
    }

    @Override
    public Mono<PnPaperCost> getByCapOrZoneAndProductType(String cap, PnZone zone, String productType) {
        String value = cap;
        String index = PnPaperCost.CAP_INDEX;
        if (zone != null) {
            value = zone.getZone();
            index = PnPaperCost.ZONE_INDEX; // remove. nuova variable col = zone/cap
        }
        String filterExpression = "(" + PnPaperCost.COL_PRODUCT_TYPE + " = :productType)"; // adds filter cap or zone
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
