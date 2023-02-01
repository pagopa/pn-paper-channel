package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperRequestError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;

@Repository
@Slf4j
public class PaperRequestErrorDAOImpl extends BaseDAO<PnPaperRequestError> implements PaperRequestErrorDAO {


    public PaperRequestErrorDAOImpl(KmsEncryption kmsEncryption,
                       DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                       DynamoDbAsyncClient dynamoDbAsyncClient,
                       AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbPaperRequestErrorTable(), PnPaperRequestError.class);}

    @Override
    public Mono<PnPaperRequestError> created(String requestId, String error, String flowThrow) {
        PnPaperRequestError requestError = new PnPaperRequestError();
        requestError.setRequestId(requestId);
        requestError.setError(error);
        requestError.setCreated(Instant.now());
        requestError.setFlowThrow(flowThrow);

        return Mono.fromFuture(this.put(requestError).thenApply(item -> requestError));
    }

}
