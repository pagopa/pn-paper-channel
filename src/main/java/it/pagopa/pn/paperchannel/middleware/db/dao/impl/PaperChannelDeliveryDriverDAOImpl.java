package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_DRIVER_NOT_FOUND;

@Repository
@Slf4j
public class PaperChannelDeliveryDriverDAOImpl extends BaseDAO<PaperChannelDeliveryDriver> implements PaperChannelDeliveryDriverDAO {


    protected PaperChannelDeliveryDriverDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, DynamoDbAsyncClient dynamoDbAsyncClient, AwsPropertiesConfig awsPropertiesConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, awsPropertiesConfig.getDynamodbPaperChannelDeliveryDriverTable(), PaperChannelDeliveryDriver.class);
    }

    @Override
    public Mono<PaperChannelDeliveryDriver> getByDeliveryDriverId(String deliveryDriverId) {
        return Mono.fromFuture(dynamoTable.getItem(Key.builder()
                        .partitionValue(deliveryDriverId)
                        .build()))
                .doOnNext(deliveryDriver -> log.info("DeliveryDriver with deliveryDriverId={} founded", deliveryDriverId))
                .switchIfEmpty(Mono.error(new PnInternalException(DELIVERY_DRIVER_NOT_FOUND.getMessage(), 404, DELIVERY_DRIVER_NOT_FOUND.getTitle())))
                .doOnError(e -> log.error("Error getting item with deliveryDriverId {}: {}", deliveryDriverId, e.getMessage()));
    }
}
