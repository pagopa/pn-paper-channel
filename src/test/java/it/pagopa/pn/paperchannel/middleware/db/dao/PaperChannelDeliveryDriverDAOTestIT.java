package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

class PaperChannelDeliveryDriverDAOTestIT extends BaseTest {

    @Autowired
    PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO;

    @Autowired
    DynamoDbAsyncClient dynamoDbAsyncClient;

    @Autowired
    AwsPropertiesConfig awsPropertiesConfig;

    @Test
    void getPaperChannelDeliveryDriverTest() {

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("deliveryDriverId", AttributeValue.builder().s("test").build());
        itemMap.put("unifiedDeliveryDriver", AttributeValue.builder().s("unifiedDeliveryDriver").build());

        dynamoDbAsyncClient.putItem(PutItemRequest.builder()
                .item(itemMap)
                .tableName(awsPropertiesConfig.getDynamodbPaperChannelDeliveryDriverTable())
                .build()).join();

        String deliveryDriverId = "test";

        PaperChannelDeliveryDriver result = paperChannelDeliveryDriverDAO.getByDeliveryDriverId(deliveryDriverId).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("test", result.getDeliveryDriverId());
        Assertions.assertEquals("unifiedDeliveryDriver", result.getUnifiedDeliveryDriver());
    }

    @Test
    void getById_NotFoundTest() {

        String deliveryDriverId = "nonExistentId";

        StepVerifier.create(paperChannelDeliveryDriverDAO.getByDeliveryDriverId(deliveryDriverId))
                .expectErrorMatches(throwable -> throwable instanceof PnInternalException pnInternalException &&
                        pnInternalException.getStatus() == 404)
                .verify();

    }
}
