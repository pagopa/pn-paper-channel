package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.model.DeliveryDriverFilter;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import java.time.Instant;
import java.util.List;

@Repository
public class DeliveryDriverDAOImpl extends BaseDAO<PnPaperDeliveryDriver> implements DeliveryDriverDAO {


    public DeliveryDriverDAOImpl(PnAuditLogBuilder auditLogBuilder,
                                 KmsEncryption kmsEncryption,
                                 DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                 DynamoDbAsyncClient dynamoDbAsyncClient,
                                 AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbDeliveryDriverTable(), PnPaperDeliveryDriver.class);
    }

    @Override
    public Mono<List<PnPaperDeliveryDriver>> getDeliveryDriver(DeliveryDriverFilter filter) {
        Pair<Instant, Instant> startAndEndTimestamp = DateUtils.getStartAndEndTimestamp(filter.getStartDate(), filter.getEndDate());
        return this.getByFilter(CONDITION_BETWEEN.apply(new Keys(keyBuild("PN-PAPER-CHANNEL", startAndEndTimestamp.getFirst().toString()), keyBuild("PN-PAPER-CHANNEL", startAndEndTimestamp.getSecond().toString()) )),
                PnPaperDeliveryDriver.CREATED_INDEX, null, null)
                .collectList();
    }


}
