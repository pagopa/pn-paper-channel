package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
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
    public Mono<PnRequestError> created(PnRequestError pnRequestError) {
        return Mono.fromFuture(this.put(pnRequestError).thenApply(item -> pnRequestError));
    }


    public Mono<List<PnRequestError>>  findAll(){
        QueryConditional conditional = CONDITION_EQUAL_TO.apply(keyBuild(Const.PN_PAPER_CHANNEL,null));
        return this.getByFilter(conditional, PnRequestError.AUTHOR_INDEX, null, null, Const.maxErrorsElements).collectList();
    }

}
