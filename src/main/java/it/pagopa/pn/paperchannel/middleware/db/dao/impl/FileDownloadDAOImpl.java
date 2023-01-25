package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnFile;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FileDownloadDAOImpl extends BaseDAO<PnFile> implements FileDownloadDAO {


    public FileDownloadDAOImpl(PnAuditLogBuilder auditLogBuilder,
                         KmsEncryption kmsEncryption,
                         DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                         DynamoDbAsyncClient dynamoDbAsyncClient,
                         AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbTenderTable(), PnFile.class);
    }

    @Override
    public Mono<PnFile> getUuid(String uuid) {

        return Mono.fromFuture(this.get(uuid, null).thenApply(item -> item));
    }

    @Override
    public Mono<PnFile> create(PnFile pnFile) {
        return Mono.fromFuture(put(pnFile).thenApply(i-> i));
    }
}
