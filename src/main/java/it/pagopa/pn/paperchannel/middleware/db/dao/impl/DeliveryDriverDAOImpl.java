package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.model.DeliveryDriverFilter;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DeliveryDriverDAOImpl extends BaseDAO<PnPaperDeliveryDriver> implements DeliveryDriverDAO {


    public DeliveryDriverDAOImpl(PnAuditLogBuilder auditLogBuilder,
                                 KmsEncryption kmsEncryption,
                                 DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                 DynamoDbAsyncClient dynamoDbAsyncClient,
                                 AwsPropertiesConfig awsPropertiesConfig) {
        super(auditLogBuilder, kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbDeliveryDriverTable(), PnPaperDeliveryDriver.class);
    }

    @Override
    public Mono<PnPaperDeliveryDriver> addDeliveryDriver(PnPaperDeliveryDriver pnDeliveryDriver) {
        pnDeliveryDriver.setCreated("PN-PAPER-CHANNEL");
        pnDeliveryDriver.setStartDate(Instant.now());
        return Mono.fromFuture(this.put(pnDeliveryDriver));
    }

    @Override
    public Mono<List<PnPaperDeliveryDriver>> getDeliveryDriver(DeliveryDriverFilter filter) {
        Pair<Instant, Instant> startAndEndTimestamp = DateUtils.getStartAndEndTimestamp(DateUtils.formatDate(filter.getStartDate()), DateUtils.formatDate(filter.getEndDate()));
        return this.getByFilter(CONDITION_BETWEEN.apply(new Keys(keyBuild("PN-PAPER-CHANNEL", startAndEndTimestamp.getFirst().toString()), keyBuild("PN-PAPER-CHANNEL", startAndEndTimestamp.getSecond().toString()) )),
                PnPaperDeliveryDriver.CREATED_INDEX, null, null)
                .collectList();
    }


}
