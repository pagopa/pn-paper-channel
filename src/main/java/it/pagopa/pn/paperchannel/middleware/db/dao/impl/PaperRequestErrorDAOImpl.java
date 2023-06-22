package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InfoDownloadDTO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;
import java.util.List;

@Repository
@Slf4j
public class PaperRequestErrorDAOImpl extends BaseDAO<PnRequestError> implements PaperRequestErrorDAO {


    public PaperRequestErrorDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                    DynamoDbAsyncClient dynamoDbAsyncClient,
                                    AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbPaperRequestErrorTable(), PnRequestError.class);}

    @Override
    public Mono<PnRequestError> created(String requestId, String error, String flowThrow) {
        PnRequestError requestError = new PnRequestError();
        requestError.setRequestId(requestId);
        requestError.setError(error);
        requestError.setCreated(Instant.now());
        requestError.setAuthor(Const.PN_PAPER_CHANNEL);
        requestError.setFlowThrow(flowThrow);

        return Mono.fromFuture(this.put(requestError).thenApply(item -> requestError));
    }


    public Mono<List<PnRequestError>>  findAll(){
        QueryConditional conditional = CONDITION_EQUAL_TO.apply(keyBuild(Const.PN_PAPER_CHANNEL,null));
        return this.getByFilter(conditional, null, null, null, null).collectList();
    }

}
