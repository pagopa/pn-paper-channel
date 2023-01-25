package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Repository
public class FileDownloadDAOImpl extends BaseDAO<PnDeliveryFile> implements FileDownloadDAO {

    public FileDownloadDAOImpl(KmsEncryption kmsEncryption,
                         DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                         DynamoDbAsyncClient dynamoDbAsyncClient,
                         AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbDeliveryFileTable(), PnDeliveryFile.class);
    }

    @Override
    public Mono<PnDeliveryFile> getUuid(String uuid) {
        return Mono.fromFuture(this.get(uuid, null).thenApply(item -> item));
    }

    @Override
    public Mono<PnDeliveryFile> create(PnDeliveryFile pnDeliveryFile) {
        return Mono.fromFuture(put(pnDeliveryFile).thenApply(i-> i));
    }
}
