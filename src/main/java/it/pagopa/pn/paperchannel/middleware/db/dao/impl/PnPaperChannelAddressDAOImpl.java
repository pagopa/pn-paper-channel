package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnPaperChannelAddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelAddress;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.util.List;

@Repository
@Slf4j
public class PnPaperChannelAddressDAOImpl extends BaseDAO <PnPaperChannelAddress> implements PnPaperChannelAddressDAO {

    public PnPaperChannelAddressDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                        DynamoDbAsyncClient dynamoDbAsyncClient,
                                        AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, awsPropertiesConfig.getDynamodbPaperChannelAddressTable(), PnPaperChannelAddress.class);
    }

    @Override
    public Mono<PnPaperChannelAddress> create(PnPaperChannelAddress pnPaperChannelAddress) {
        return Mono.fromFuture(put(pnPaperChannelAddress));
    }

    @Override
    public Mono<PnPaperChannelAddress> findByRequestId(String requestId) {
        return this.findByRequestId(requestId, AddressTypeEnum.RECEIVER_ADDRESS);
    }

    @Override
    public Mono<PnPaperChannelAddress> findByRequestId(String requestId, AddressTypeEnum addressTypeEnum) {
        return getPaperChannelAddress(requestId, addressTypeEnum, false)
                .switchIfEmpty(getPaperChannelAddress(requestId, addressTypeEnum, true));
    }

    @Override
    public Mono<PnPaperChannelAddress> getPaperChannelAddress(String requestId, AddressTypeEnum addressTypeEnum, boolean consistentRead) {
        return Mono.fromFuture(this.get(requestId, addressTypeEnum.toString(), consistentRead));
    }

    @Override
    public Mono<List<PnPaperChannelAddress>> findAllByRequestId(String requestId) {
        QueryConditional keyConditional = CONDITION_EQUAL_TO.apply(Key.builder().partitionValue(requestId).build());
        return getByFilter(keyConditional, null, null, null)
                .map(this::decode)
                .collectList();
    }
}
