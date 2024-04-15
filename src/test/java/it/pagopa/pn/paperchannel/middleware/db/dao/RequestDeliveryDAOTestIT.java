package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.encryption.impl.DataVaultEncryptionImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

class RequestDeliveryDAOTestIT extends BaseTest {

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @MockBean
    private DataVaultEncryptionImpl dataVaultEncryption;
    private final PnDeliveryRequest request = new PnDeliveryRequest();
    private final PnDeliveryRequest request1 = new PnDeliveryRequest();
    private final PnDeliveryRequest request2 = new PnDeliveryRequest();
    private final PnDeliveryRequest duplicateRequest = new PnDeliveryRequest();
    private final PnDeliveryRequest requestWithEmptyFields = new PnDeliveryRequest();
    private final PnAddress address = new PnAddress();

    @BeforeEach
    public void setUp(){
        initialize();
    }

    @Test
    void createWithAddressTest(){
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");
        PnDeliveryRequest createRequest = this.requestDeliveryDAO.createWithAddress(request, address, null).block();

        assertNotNull(createRequest);
        assertEquals(createRequest, request);
    }

    @Test
    void createWithoutAddressTest(){
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");
        PnDeliveryRequest createRequest2 = this.requestDeliveryDAO.createWithAddress(request1, null, null).block();

        assertNotNull(createRequest2);
        assertEquals(createRequest2, request1);
    }

    @Test
    void updateDataTest(){
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");
        PnDeliveryRequest createRequest = this.requestDeliveryDAO.createWithAddress(request2, address, null).block();
        PnDeliveryRequest updateRequest = this.requestDeliveryDAO.updateData(duplicateRequest).block();

        assertNotNull(updateRequest);
        assertEquals(createRequest, request2);
        assertEquals(updateRequest, duplicateRequest);
    }

    @Test
    void updateDataTestWithoutIgnoringNulls(){
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest createRequest = this.requestDeliveryDAO.createWithAddress(request, address, null).block();
        PnDeliveryRequest updateRequest = this.requestDeliveryDAO.updateData(requestWithEmptyFields).block();

        assertNotNull(createRequest);
        assertNotNull(updateRequest);
        assertEquals(createRequest, request);
        assertEquals(updateRequest, requestWithEmptyFields);

        PnDeliveryRequest readRequestAfterUpdate = this.requestDeliveryDAO.getByRequestId(createRequest.getRequestId()).block();

        assertNotNull(readRequestAfterUpdate);
        assertEquals(createRequest.getRequestId(), readRequestAfterUpdate.getRequestId());
        assertNotEquals(createRequest.getStatusCode(), readRequestAfterUpdate.getStatusCode());
        assertNull(readRequestAfterUpdate.getProductType()); // was null during update
    }

    @Test
    void updateDataTestWithIgnoringNulls(){
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest createRequest = this.requestDeliveryDAO.createWithAddress(request, address, null).block();
        PnDeliveryRequest updateRequest = this.requestDeliveryDAO.updateData(requestWithEmptyFields, true).block();

        assertNotNull(createRequest);
        assertNotNull(updateRequest);
        assertEquals(createRequest, request);
        assertEquals(updateRequest, requestWithEmptyFields);

        PnDeliveryRequest readRequestAfterUpdate = this.requestDeliveryDAO.getByRequestId(createRequest.getRequestId()).block();

        assertNotNull(readRequestAfterUpdate);
        assertEquals(createRequest.getRequestId(), readRequestAfterUpdate.getRequestId());
        assertNotEquals(createRequest.getStatusCode(), readRequestAfterUpdate.getStatusCode());
        assertEquals(readRequestAfterUpdate.getProductType(), createRequest.getProductType()); // was null during update
    }

    @Test
    void getByRequestIdTest(){
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByRequestId(request.getRequestId()).block();
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
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByCorrelationId(duplicateRequest.getCorrelationId()).block();
        assertNotNull(deliveryRequest);
    }

    @Test
    void getByCorrelationIdNotPresentTest(){
        String correlationId = "idCor";
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByCorrelationId(correlationId).block();

        assertNull(deliveryRequest);
    }

    private void initialize(){

        request.setRequestId("id-12345");
        request.setProductType("req");
        request.setCorrelationId("idcor-aaddg89");
        request.setStatusCode("VALIDATE");
        request.setProductType("type");
        request.setFiscalCode("FRMTTR76M06B715E");
        request.setReceiverType("PF");
        request.setRefined(false);

        request1.setRequestId("id-1212");
        request1.setProductType("reqeeww");
        request1.setCorrelationId("idcor-78867");
        request1.setReceiverType("PF");
        request1.setFiscalCode("FRMTTR76M06B715E");
        request1.setRefined(false);

        request2.setRequestId("id-54321");
        request2.setProductType("req");
        request2.setCorrelationId("idcor-aaddg89");
        request2.setStatusCode("VALIDATE");
        request2.setProductType("type");
        request2.setFiscalCode("FRMTTR76M06B715E");
        request2.setReceiverType("PF");
        request2.setRefined(false);

        duplicateRequest.setRequestId("id-54321");
        duplicateRequest.setProductType("req");
        duplicateRequest.setCorrelationId("idcor-12345");
        duplicateRequest.setStatusCode("VALIDATE");
        duplicateRequest.setProductType("type");
        duplicateRequest.setFiscalCode("FRMTTR76M06B715E");
        duplicateRequest.setReceiverType("PF");
        duplicateRequest.setRefined(false);
        duplicateRequest.setDriverCode("driverCode");
        duplicateRequest.setTenderCode("tenderCode");

        requestWithEmptyFields.setRequestId("id-12345");
        requestWithEmptyFields.setStatusCode("TEST");

        address.setAddress("Via Aldo Moro");
        address.setCap("21004");
        address.setRequestId("LOP-DF3-412");
        address.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
    }
}
