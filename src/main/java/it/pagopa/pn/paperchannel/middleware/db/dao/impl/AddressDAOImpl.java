package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.encryption.impl.KmsEncryptionImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Repository
@Slf4j
public class AddressDAOImpl extends BaseDAO <PnAddress> implements AddressDAO {


    public AddressDAOImpl(KmsEncryptionImpl kmsEncryption,
                          DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                          DynamoDbAsyncClient dynamoDbAsyncClient,
                          AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbAddressTable(), PnAddress.class);
    }

    @Override
    public Mono<PnAddress> create(PnAddress pnAddress) {
        return Mono.fromFuture(this.decode(put(pnAddress)).thenApply(i-> i));
    }

    @Override
    public void createTransaction(TransactWriterInitializer transactWriterInitializer, PnAddress pnAddress) {
        transactWriterInitializer.addRequestTransaction(this.dynamoTable, encode(pnAddress, PnAddress.class), PnAddress.class);
    }

    @Override
    public Mono<PnAddress> findByRequestId(String requestId) {
        return Mono.fromFuture(this.get(requestId, AddressTypeEnum.RECEIVER_ADDRESS.toString()).thenApply(item -> item));
    }

    @Override
    public Mono<List<PnAddress>> findAllByRequestId(String requestId) {
        return null;
        //TODO get all elements by partitinkey
    }

    private CompletableFuture<Integer> countOccurrencesEntity(String requestId) {
        String keyConditionExpression = PnDeliveryRequest.COL_REQUEST_ID + " = :requestId";
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":requestId",  AttributeValue.builder().s(requestId).build());
        return this.getCounterQuery(expressionValues, "", keyConditionExpression);
    }

}
