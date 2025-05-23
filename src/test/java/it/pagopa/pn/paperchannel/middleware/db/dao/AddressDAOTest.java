package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.impl.AddressDAOImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AddressDAOTest {
    private AddressDAO addressDAO;

    @Mock
    private DynamoDbAsyncTable<PnAddress> dynamoTable;

    @Mock
    DataEncryption kmsEncryption;
    @Mock
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock
    AwsPropertiesConfig awsPropertiesConfig;

    @BeforeEach
    void setup(){
        when(awsPropertiesConfig.getDynamodbAddressTable()).thenReturn("pn-address");
        when(dynamoDbEnhancedAsyncClient.table(eq("pn-address"), any(BeanTableSchema.class))).thenReturn(dynamoTable);
        this.addressDAO = new AddressDAOImpl(
                kmsEncryption,
                dynamoDbEnhancedAsyncClient,
                dynamoDbAsyncClient,
                awsPropertiesConfig);
    }

    @Test
    void findByRequestIdEmptyTest(){
        // Arrange
        when(dynamoTable.getItem((GetItemEnhancedRequest) any())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        PnAddress pnAddress = this.addressDAO.findByRequestId("INVALID_REQUEST_ID").block();

        // Assert
        Assertions.assertNull(pnAddress);

        ArgumentCaptor<GetItemEnhancedRequest> captor = ArgumentCaptor.forClass(GetItemEnhancedRequest.class);
        verify(dynamoTable, times(2)).getItem(captor.capture());
        var requests = captor.getAllValues();
        assertEquals(false, requests.get(0).consistentRead());
        assertEquals(true, requests.get(1).consistentRead());
    }
}
