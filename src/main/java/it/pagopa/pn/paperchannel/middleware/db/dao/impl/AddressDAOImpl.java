package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import java.util.List;

@Repository
@Slf4j
public class AddressDAOImpl extends BaseDAO <PnAddress> implements AddressDAO {

    public AddressDAOImpl(DataEncryption kmsEncryption,
                          DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                          DynamoDbAsyncClient dynamoDbAsyncClient,
                          AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbAddressTable(), PnAddress.class);
    }

    @Override
    public Mono<PnAddress> create(PnAddress pnAddress) {
        return Mono.fromFuture(put(pnAddress).thenApply(this::decode));
    }

    @Override
    public void createTransaction(TransactWriterInitializer transactWriterInitializer, PnAddress pnAddress) {
        transactWriterInitializer.addRequestTransaction(this.dynamoTable, encode(pnAddress), PnAddress.class);
    }

    @Override
    public void createTransaction(TransactWriteItemsEnhancedRequest.Builder builder, PnAddress address) {
        TransactPutItemEnhancedRequest<PnAddress> addressRequest =
                TransactPutItemEnhancedRequest.builder(PnAddress.class)
                        .item(encode(address))
                        .build();
        builder.addPutItem(this.dynamoTable, addressRequest );
    }

    @Override
    public Mono<PnAddress> findByRequestId(String requestId) {
        return this.findByRequestId(requestId, AddressTypeEnum.RECEIVER_ADDRESS);
    }

    @Override
    public Mono<PnAddress> findByRequestId(String requestId, AddressTypeEnum addressTypeEnum) {
        return getPnAddress(requestId, addressTypeEnum, false)
                .switchIfEmpty(getPnAddress(requestId, addressTypeEnum, true));
    }

    private Mono<PnAddress> getPnAddress(String requestId, AddressTypeEnum addressTypeEnum, boolean consistentRead) {
        return Mono.fromFuture(this.get(requestId, addressTypeEnum.toString(), consistentRead));
    }

    @Override
    public Mono<List<PnAddress>> findAllByRequestId(String requestId) {
        QueryConditional keyConditional = CONDITION_EQUAL_TO.apply(Key.builder().partitionValue(requestId).build());
        return getByFilter(keyConditional, null, null, null)
                .map(this::decode)
                .collectList();
    }

}
