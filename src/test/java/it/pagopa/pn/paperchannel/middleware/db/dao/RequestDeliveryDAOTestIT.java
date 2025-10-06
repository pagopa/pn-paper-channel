package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.encryption.impl.DataVaultEncryptionImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RequestDeliveryDAOTestIT extends BaseTest {

    private static final String REQUEST_WITH_ADDRESS_ID = "requestWithAddressId";
    private static final String REQUEST_WITHOUT_ADDRESS_ID = "requestWithoutAddressId";
    private static final String RASTERIZATION_REQUESTID_WITH_FALSE_APPLY = "requestForRasterizationWithApplyFalse";
    private static final String RASTERIZATION_REQUESTID_WITH_NULL_APPLY = "requestForRasterizationWithApplyNull";

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private DataVaultEncryptionImpl dataVaultEncryption;

    @Test
    void createWithAddressesWithSenderPaIdTest(){

        // Given
        PnAddress address = new PnAddress();
        address.setAddress("Via Aldo Moro");
        address.setCap("21004");
        address.setRequestId("LOP-DF3-412");
        address.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());

        PnDeliveryRequest deliveryRequest = this.buildDeliveryRequest(REQUEST_WITH_ADDRESS_ID);

        deliveryRequest.setRequestId("testSenderPaId");

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        this.requestDeliveryDAO.createWithAddress(deliveryRequest, address, null).block();
        PnDeliveryRequest pnDeliveryRequest = this.requestDeliveryDAO.getByRequestId(deliveryRequest.getRequestId()).block();

        // Then
        assertNotNull(pnDeliveryRequest);
        assertEquals(pnDeliveryRequest.getSenderPaId(), deliveryRequest.getSenderPaId());
    }

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
        deliveryRequest.setApplyRasterization(true);

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest createdRequest = this.requestDeliveryDAO.createWithAddress(deliveryRequest, null, null).block();

        // Then
        assertNotNull(createdRequest);
        assertEquals(createdRequest, deliveryRequest);

        PnDeliveryRequest readRequestAfterUpdate = this.requestDeliveryDAO.getByRequestId(createdRequest.getRequestId()).block();
        assertNotNull(createdRequest);
        assertTrue(readRequestAfterUpdate.getApplyRasterization());

    }

    @Test
    @Order(3)
    void updateDataWithoutIgnoringNullsTest(){

        // Given
        PnDeliveryRequest partialDeliveryRequest = new PnDeliveryRequest();
        partialDeliveryRequest.setRequestId(REQUEST_WITH_ADDRESS_ID);
        partialDeliveryRequest.setStatusCode("TEST");
        partialDeliveryRequest.setFeedbackStatusCode(null);

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
    @Order(4)
    void updateDataWithIgnoringNullsTest(){

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
    @Order(5)
    void updateConditionalWhenFeedbackNotExistsStatusTest(){

        // Given
        PnDeliveryRequest partialDeliveryRequest = new PnDeliveryRequest();
        partialDeliveryRequest.setRequestId(REQUEST_WITHOUT_ADDRESS_ID);
        partialDeliveryRequest.setStatusCode("TEST");
        partialDeliveryRequest.setFeedbackStatusCode("TEST");

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest updateRequest = this.requestDeliveryDAO.updateConditionalOnFeedbackStatus(partialDeliveryRequest, true).block();

        assertNotNull(updateRequest);
        assertEquals(updateRequest, partialDeliveryRequest);

        PnDeliveryRequest readRequestAfterUpdate = this.requestDeliveryDAO.getByRequestId(partialDeliveryRequest.getRequestId()).block();

        assertNotNull(readRequestAfterUpdate);
        assertEquals(REQUEST_WITHOUT_ADDRESS_ID, readRequestAfterUpdate.getRequestId());
        assertEquals("TEST", readRequestAfterUpdate.getStatusCode());
        assertEquals("TEST", readRequestAfterUpdate.getFeedbackStatusCode());
        assertNotNull(readRequestAfterUpdate.getProductType()); // was null during update
    }

    @Test
    @Order(6)
    void updateConditionalWhenFeedbackIsNullStatusTest(){

        // Given
        PnDeliveryRequest partialDeliveryRequest = new PnDeliveryRequest();
        partialDeliveryRequest.setRequestId(REQUEST_WITH_ADDRESS_ID);
        partialDeliveryRequest.setFeedbackStatusCode("TEST");

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest updateRequest = this.requestDeliveryDAO.updateConditionalOnFeedbackStatus(partialDeliveryRequest, true).block();

        assertNotNull(updateRequest);
        assertEquals(updateRequest, partialDeliveryRequest);

        PnDeliveryRequest readRequestAfterUpdate = this.requestDeliveryDAO.getByRequestId(partialDeliveryRequest.getRequestId()).block();

        assertNotNull(readRequestAfterUpdate);
        assertEquals(REQUEST_WITH_ADDRESS_ID, readRequestAfterUpdate.getRequestId());
        assertEquals("TEST", readRequestAfterUpdate.getFeedbackStatusCode());
    }

    @Test
    @Order(7)
    void updateConditionalFailOnFeedbackStatusTest(){

        // Given
        PnDeliveryRequest partialDeliveryRequest = new PnDeliveryRequest();
        partialDeliveryRequest.setRequestId(REQUEST_WITHOUT_ADDRESS_ID); // same request with already set feedback
        partialDeliveryRequest.setStatusCode("TEST");
        partialDeliveryRequest.setFeedbackStatusCode("TEST");

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        PnDeliveryRequest updateRequest = this.requestDeliveryDAO.updateConditionalOnFeedbackStatus(partialDeliveryRequest, true).block();
        assertNull(updateRequest);
    }

    @Test
    void updateDataWithoutGetTest() {

        // Given
        PnDeliveryRequest deliveryRequest = this.buildDeliveryRequest("updateWithoutGetTestId");
        deliveryRequest.setStatusCode("INITIAL");
        this.requestDeliveryDAO.createWithAddress(deliveryRequest, null, null).block();

        // Modifica i dati da aggiornare
        PnDeliveryRequest updatedRequest = new PnDeliveryRequest();
        updatedRequest.setRequestId("updateWithoutGetTestId");
        updatedRequest.setStatusCode("UPDATED");

        // When
        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        this.requestDeliveryDAO.updateDataWithoutGet(updatedRequest, true).block();

        // Then
        PnDeliveryRequest result = this.requestDeliveryDAO.getByRequestId("updateWithoutGetTestId").block();
        assertNotNull(result);
        assertEquals("UPDATED", result.getStatusCode());
    }

    @Test
    void updateUpdateApplyRasterizationStartedValueNull(){
        // Given
        PnDeliveryRequest request = new PnDeliveryRequest();
        request.setRequestId(RASTERIZATION_REQUESTID_WITH_NULL_APPLY);
        request.setStatusCode("TEST");
        request.setFeedbackStatusCode("TEST");

        requestDeliveryDAO.createWithAddress(request, null, null).block();
        PnDeliveryRequest createdRequest = this.requestDeliveryDAO.getByRequestId(RASTERIZATION_REQUESTID_WITH_NULL_APPLY).block();
        assert createdRequest != null;
        Assertions.assertNull(createdRequest.getApplyRasterization());

        request.setApplyRasterization(true);
        UpdateItemResponse updateItemResponse = this.requestDeliveryDAO.updateApplyRasterization(request.getRequestId(), request.getApplyRasterization()).block();
        assertNotNull(updateItemResponse);
        PnDeliveryRequest updatedRequest = this.requestDeliveryDAO.getByRequestId(RASTERIZATION_REQUESTID_WITH_NULL_APPLY).block();
        assert updatedRequest != null;
        Assertions.assertTrue(updatedRequest.getApplyRasterization());
    }

    @Test
    void updateUpdateApplyRasterizationStartedValueFalse(){
        // Given
        PnDeliveryRequest request = new PnDeliveryRequest();
        request.setRequestId(RASTERIZATION_REQUESTID_WITH_FALSE_APPLY);
        request.setStatusCode("TEST");
        request.setFeedbackStatusCode("TEST");
        request.setApplyRasterization(false);

        requestDeliveryDAO.createWithAddress(request, null, null).block();
        PnDeliveryRequest createdRequest = this.requestDeliveryDAO.getByRequestId(RASTERIZATION_REQUESTID_WITH_FALSE_APPLY).block();
        assert createdRequest != null;
        Assertions.assertFalse(createdRequest.getApplyRasterization());

        request.setApplyRasterization(true);
        UpdateItemResponse updateItemResponse = this.requestDeliveryDAO.updateApplyRasterization(request.getRequestId(), request.getApplyRasterization()).block();
        assertNotNull(updateItemResponse);
        PnDeliveryRequest updatedRequest = this.requestDeliveryDAO.getByRequestId(RASTERIZATION_REQUESTID_WITH_FALSE_APPLY).block();
        assert updatedRequest != null;
        Assertions.assertTrue(updatedRequest.getApplyRasterization());
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

    @Test
    void getByRequestIdStrongConsistencyWithDecodeTest(){
        var requestId = "request1strongwithdecode";
        PnDeliveryRequest request = new PnDeliveryRequest();
        request.setRequestId(requestId);
        requestDeliveryDAO.createWithAddress(request, null, null).block();
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, true).block();
        assertNotNull(deliveryRequest);
        assertEquals(request.getRequestId(), deliveryRequest.getRequestId());
    }

    @Test
    void getByRequestIdStrongConsistencyWithoutDecodeTest(){
        var requestId = "request1strong";
        PnDeliveryRequest request = new PnDeliveryRequest();
        request.setRequestId(requestId);
        request = requestDeliveryDAO.createWithAddress(request, null, null).block();
        PnDeliveryRequest deliveryRequest = this.requestDeliveryDAO.getByRequestIdStrongConsistency(requestId, false).block();
        assertNotNull(deliveryRequest);
        assertEquals(request.getRequestId(), deliveryRequest.getRequestId());
    }

    @Test
    void updateStatusTest(){
        // Given
        var requestId = UUID.randomUUID().toString();
        PnDeliveryRequest deliveryRequest = this.buildDeliveryRequest(requestId);

        this.requestDeliveryDAO.createWithAddress(deliveryRequest, null, null).block();

        deliveryRequest = this.requestDeliveryDAO.getByRequestId(requestId).block();

        assertThat(deliveryRequest).isNotNull();
        assertThat(deliveryRequest.getStatusCode()).isEqualTo(StatusDeliveryEnum.IN_PROCESSING.getCode());
        assertThat(deliveryRequest.getStatusDescription()).isEqualTo(StatusDeliveryEnum.IN_PROCESSING.getDescription());
        assertThat(deliveryRequest.getStatusDetail()).isEqualTo(StatusDeliveryEnum.IN_PROCESSING.getDetail());

        var untraceable = StatusDeliveryEnum.UNTRACEABLE;
        var statusDate = DateUtils.formatDate(Instant.now());
        this.requestDeliveryDAO.updateStatus(requestId, untraceable.getCode(), untraceable.getDescription(), untraceable.getDetail(), statusDate).block();

        deliveryRequest = this.requestDeliveryDAO.getByRequestId(requestId).block();

        assertThat(deliveryRequest).isNotNull();
        assertThat(deliveryRequest.getStatusCode()).isEqualTo(StatusDeliveryEnum.UNTRACEABLE.getCode());
        assertThat(deliveryRequest.getStatusDescription()).isEqualTo(StatusDeliveryEnum.UNTRACEABLE.getDescription());
        assertThat(deliveryRequest.getStatusDetail()).isEqualTo(StatusDeliveryEnum.UNTRACEABLE.getDetail());

    }

    private PnDeliveryRequest buildDeliveryRequest(String requestId) {
        PnDeliveryRequest request = new PnDeliveryRequest();

        request.setRequestId(requestId);
        request.setProductType("req");
        request.setCorrelationId("idcor-aaddg89");
        request.setStatusCode(StatusDeliveryEnum.IN_PROCESSING.getCode());
        request.setStatusDescription(StatusDeliveryEnum.IN_PROCESSING.getDescription());
        request.setStatusDetail(StatusDeliveryEnum.IN_PROCESSING.getDetail());
        request.setStartDate(Instant.now().toString());
        request.setProductType("type");
        request.setFiscalCode("FRMTTR76M06B715E");
        request.setReceiverType("PF");
        request.setRefined(false);
        request.setAarWithRadd(true);
        request.setSenderPaId("senderPaId");

        return request;
    }
}
