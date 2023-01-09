package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COST_NOT_FOUND;

@Repository
@Slf4j
@Import(PnAuditLogBuilder.class)
public class CostDAOImpl extends BaseDAO<PnPaperCost> implements CostDAO {

    private final DynamoDbAsyncTable<PnPaperDeliveryDriver> deliveryDriverTable;
    private final TransactWriterInitializer transactWriterInitializer;

    public CostDAOImpl(PnAuditLogBuilder auditLogBuilder,
                       KmsEncryption kmsEncryption,
                       DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                       DynamoDbAsyncClient dynamoDbAsyncClient,
                       AwsPropertiesConfig awsPropertiesConfig, TransactWriterInitializer transactWriterInitializer) {
        super(auditLogBuilder, kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbCostTable(), PnPaperCost.class);
        this.deliveryDriverTable = dynamoDbEnhancedAsyncClient.table(awsPropertiesConfig.getDynamodbDeliveryDriverTable(), TableSchema.fromBean(PnPaperDeliveryDriver.class));
        this.transactWriterInitializer = transactWriterInitializer;
    }

    public Mono<List<PnPaperCost>> createNewContract(PnPaperDeliveryDriver pnDeliveryDriver, List<PnPaperCost> pnListCosts) {
        String logMessage = "create contract";
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                .build();
        logEvent.log();

        this.transactWriterInitializer.init();
        if (pnDeliveryDriver != null) {
            transactWriterInitializer.addRequestTransaction(deliveryDriverTable, pnDeliveryDriver, PnPaperDeliveryDriver.class);
        }
        pnListCosts.forEach(cost -> transactWriterInitializer.addRequestTransaction(this.dynamoTable, cost, PnPaperCost.class));
        return Mono.fromFuture(putWithTransact(transactWriterInitializer.build()).thenApply(item -> pnListCosts));

    }

    @Override
    public Mono<PnPaperCost> getByCapOrZoneAndProductType(String cap, PnZone zone, String productType) {
        String value = cap;
        String index = PnPaperCost.COL_CAP_INDEX;
        if (zone != null) {
            value = zone.getZone();
            index = PnPaperCost.COL_ZONE_INDEX;
        }
        String filterExpression = "(" + PnPaperCost.COL_PRODUCT_TYPE + " = :productType)";
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":productType", AttributeValue.builder().s(productType).build());
        return this.getByFilter(value, null, index, values, filterExpression)
                .collectList()
                .map(items -> {
                    if (items.isEmpty()) {
                        throw new PnGenericException(COST_NOT_FOUND, COST_NOT_FOUND.getMessage());
                    }
                    return items.get(0);
                });
    }



}
