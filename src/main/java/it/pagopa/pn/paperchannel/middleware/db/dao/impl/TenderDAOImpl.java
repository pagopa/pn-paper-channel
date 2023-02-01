package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;
import java.util.List;

@Repository
public class TenderDAOImpl extends BaseDAO<PnTender> implements TenderDAO {

    public TenderDAOImpl(PnAuditLogBuilder auditLogBuilder,
                                 KmsEncryption kmsEncryption,
                                 DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                 DynamoDbAsyncClient dynamoDbAsyncClient,
                                 AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbTenderTable(), PnTender.class);
    }

    @Override
    public Mono<List<PnTender>> getTenders() {
        Pair<Instant, Instant> startAndEndTimestamp = DateUtils.getStartAndEndTimestamp(null, null);

        QueryConditional conditional = CONDITION_BETWEEN.apply(
                new Keys(keyBuild("PN-PAPER-CHANNEL", startAndEndTimestamp.getFirst().toString()),
                        keyBuild("PN-PAPER-CHANNEL", startAndEndTimestamp.getSecond().toString()) )
        );

        return this.getByFilter(conditional, PnTender.AUTHOR_INDEX, null, null)
                .collectList();
    }
}
