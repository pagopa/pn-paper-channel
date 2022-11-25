package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Repository
@Slf4j
@Import(PnAuditLogBuilder.class)
public class RequestDeliveryDAOImpl implements RequestDeliveryDAO {

    private final PnAuditLogBuilder auditLogBuilder;
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;

    private final DynamoDbAsyncTable<RequestDeliveryEntity> requestDeliveryTable;
    private final String table;

    public RequestDeliveryDAOImpl(PnAuditLogBuilder auditLogBuilder,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  AwsPropertiesConfig awsPropertiesConfig) {
        this.requestDeliveryTable = dynamoDbEnhancedAsyncClient.table(awsPropertiesConfig.getDynamodbRequestDeliveryTable(), TableSchema.fromBean(RequestDeliveryEntity.class));
        this.table = awsPropertiesConfig.getDynamodbRequestDeliveryTable();
        this.auditLogBuilder = auditLogBuilder;
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
    }

    @Override
    public Mono<RequestDeliveryEntity> create(RequestDeliveryEntity requestDeliveryEntity) {
        return null;
    }

    @Override
    public Mono<RequestDeliveryEntity> getByRequestId(String requestId) {
      // QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional().build()

        return null;
    }

    @Override
    public Flux<RequestDeliveryEntity> getByFiscalCode(String fiscalCode) {
        return null;
    }
}