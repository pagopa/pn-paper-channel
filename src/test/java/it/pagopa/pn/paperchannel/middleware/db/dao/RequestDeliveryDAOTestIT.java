package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.encryption.impl.DataVaultEncryptionImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RequestDeliveryDAOTestIT extends BaseTest {

    private static final String REQUEST_WITH_ADDRESS_ID = "requestWithAddressId";
    private static final String REQUEST_WITHOUT_ADDRESS_ID = "requestWithoutAddressId";

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private DataVaultEncryptionImpl dataVaultEncryption;

    @Test
    @Order(1)
    void createWithAddressTest(){

        // Given
        PnAddress address = new PnAddress();
        address.setAddress("Via Aldo Moro");
        address.setCap("21004");
        address.setRequestId("LOP-DF3-412");
        address.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());

        PnDeliveryRequest deliveryRequest = this.buildDeliveryRequest(REQUEST_WITH_ADDRESS_ID);

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest createRequest = this.requestDeliveryDAO.createWithAddress(deliveryRequest, address, null).block();

        // Then
        assertNotNull(createRequest);
        assertEquals(createRequest, deliveryRequest);
    }

    @Test
    @Order(2)
    void createWithoutAddressTest(){

        // Given
        PnDeliveryRequest deliveryRequest = this.buildDeliveryRequest(REQUEST_WITHOUT_ADDRESS_ID);

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest createRequest = this.requestDeliveryDAO.createWithAddress(deliveryRequest, null, null).block();

        // Then
        assertNotNull(createRequest);
        assertEquals(createRequest, deliveryRequest);
    }

    @Test
    void updateDataTestWithoutIgnoringNulls(){

        // Given
        PnDeliveryRequest partialDeliveryRequest = new PnDeliveryRequest();
        partialDeliveryRequest.setRequestId(REQUEST_WITH_ADDRESS_ID);
        partialDeliveryRequest.setStatusCode("TEST");

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest updateRequest = this.requestDeliveryDAO.updateData(partialDeliveryRequest).block();

        assertNotNull(updateRequest);
        assertEquals(updateRequest, partialDeliveryRequest);

        PnDeliveryRequest readRequestAfterUpdate = this.requestDeliveryDAO.getByRequestId(partialDeliveryRequest.getRequestId()).block();

        assertNotNull(readRequestAfterUpdate);
        assertEquals(REQUEST_WITH_ADDRESS_ID, readRequestAfterUpdate.getRequestId());
        assertNull(readRequestAfterUpdate.getProductType()); // was null during update
    }

    @Test
    void updateDataTestWithIgnoringNulls(){

        // Given
        PnDeliveryRequest partialDeliveryRequest = new PnDeliveryRequest();
        partialDeliveryRequest.setRequestId(REQUEST_WITHOUT_ADDRESS_ID);
        partialDeliveryRequest.setStatusCode("TEST");

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest updateRequest = this.requestDeliveryDAO.updateData(partialDeliveryRequest, true).block();

        assertNotNull(updateRequest);
        assertEquals(updateRequest, partialDeliveryRequest);

        PnDeliveryRequest readRequestAfterUpdate = this.requestDeliveryDAO.getByRequestId(partialDeliveryRequest.getRequestId()).block();

        assertNotNull(readRequestAfterUpdate);
        assertEquals(REQUEST_WITHOUT_ADDRESS_ID, readRequestAfterUpdate.getRequestId());
        assertNotNull(readRequestAfterUpdate.getProductType()); // was null during update
    }

    @Test
    void getByRequestIdTest(){
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByRequestId(REQUEST_WITH_ADDRESS_ID).block();
        assertNotNull(deliveryRequest);
    }

    @Test
    void getByRequestIdNotPresentTest(){
        String requestId = "id-123456";
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByRequestId(requestId).block();

        assertNull(deliveryRequest);
    }

    @Test
    void getByCorrelationIdTest(){
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByCorrelationId("idcor-aaddg89").block();
        assertNotNull(deliveryRequest);
    }

    @Test
    void getByCorrelationIdNotPresentTest(){
        String correlationId = "idCor";
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByCorrelationId(correlationId).block();

        assertNull(deliveryRequest);
    }

    private PnDeliveryRequest buildDeliveryRequest(String requestId) {
        PnDeliveryRequest request = new PnDeliveryRequest();

        request.setRequestId(requestId);
        request.setProductType("req");
        request.setCorrelationId("idcor-aaddg89");
        request.setStatusCode("VALIDATE");
        request.setProductType("type");
        request.setFiscalCode("FRMTTR76M06B715E");
        request.setReceiverType("PF");
        request.setRefined(false);
        request.setAarWithRadd(true);

        return request;
    }
}
