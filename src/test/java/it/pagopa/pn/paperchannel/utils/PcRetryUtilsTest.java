package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PcRetryResponse;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.PaperTrackerClient;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PcRetryUtilsTest {

    private PcRetryUtils pcRetryUtils;
    private PnPaperChannelConfig config;
    private ExternalChannelClient externalChannelClient;
    private AddressDAO addressDAO;
    private PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO;
    private PaperTrackerClient paperTrackerClient;

    @BeforeEach
    void setup() {
        config = mock(PnPaperChannelConfig.class);
        externalChannelClient = mock(ExternalChannelClient.class);
        addressDAO = mock(AddressDAO.class);
        paperChannelDeliveryDriverDAO = mock(PaperChannelDeliveryDriverDAO.class);
        paperTrackerClient = mock(PaperTrackerClient.class);
        pcRetryUtils = new PcRetryUtils(config, externalChannelClient, addressDAO, paperChannelDeliveryDriverDAO, paperTrackerClient);
    }


    @Test
    void testHasOtherAttempt_True() {
        String requestId = "ABC_RETRY_1";
        when(config.getMaxPcRetry()).thenReturn(5);
        Assertions.assertTrue(pcRetryUtils.hasOtherAttempt(requestId));
    }

    @Test
    void testHasOtherAttempt_False() {
        String requestId = "ABC_.PCRETRY_3";
        when(config.getMaxPcRetry()).thenReturn(2);
        Assertions.assertFalse(pcRetryUtils.hasOtherAttempt(requestId));
    }

    @Test
    void testSetRetryRequestId_WhenRetryPresent() {
        String requestId = "XYZ_.PCRETRY_2";
        String result = pcRetryUtils.setRetryRequestId(requestId);
        Assertions.assertEquals("XYZ_.PCRETRY_3", result);
    }

    @Test
    void testSetRetryRequestId_WhenRetryNotPresent() {
        String requestId = "XYZ";
        String result = pcRetryUtils.setRetryRequestId(requestId);
        Assertions.assertEquals("XYZ", result);
    }

    @Test
    void testCheckHasOtherAttemptAndMapPcRetryResponse_WithRetry() {
        PnDeliveryRequest pnDeliveryRequest = getPnDeliveryRequest();
        pnDeliveryRequest.setStatusCode("RECRN006");
        when(config.getMaxPcRetry()).thenReturn(10);
        when(addressDAO.findAllByRequestId(pnDeliveryRequest.getRequestId())).thenReturn(Mono.just(new ArrayList<>()));
        when(externalChannelClient.sendEngageRequest(any(), any(), any())).thenReturn(Mono.empty());

        PcRetryResponse response = pcRetryUtils.checkHasOtherAttemptAndMapPcRetryResponse("IUN.ATTEMPT_0.PCRETRY_0", "driver-1", pnDeliveryRequest).block();

        Assertions.assertEquals("IUN.ATTEMPT_0.PCRETRY_0", response.getParentRequestId());
        Assertions.assertEquals("driver-1", response.getDeliveryDriverId());
        Assertions.assertTrue(response.getRetryFound());
        Assertions.assertEquals("PCRETRY_1", response.getPcRetry());
        Assertions.assertEquals("IUN.ATTEMPT_0.PCRETRY_1", response.getRequestId());
        verify(addressDAO).findAllByRequestId(pnDeliveryRequest.getRequestId());
        verify(externalChannelClient).sendEngageRequest(any(), any(), any());
    }

    @Test
    void testCheckHasOtherAttemptAndMapPcRetryResponse_WithoutRetry() {
        PnDeliveryRequest pnDeliveryRequest = getPnDeliveryRequest();
        when(config.getMaxPcRetry()).thenReturn(1);
        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.toString());
        when(addressDAO.findAllByRequestId(anyString())).thenReturn(Mono.just(List.of(pnAddress)));
        when(externalChannelClient.sendEngageRequest(any(), any(), any())).thenReturn(Mono.empty());
        PcRetryResponse response = pcRetryUtils.checkHasOtherAttemptAndMapPcRetryResponse("IUN.ATTEMPT_0.PCRETRY_2", "driver-1", pnDeliveryRequest).block();

        Assertions.assertEquals("IUN.ATTEMPT_0.PCRETRY_2", response.getParentRequestId());
        Assertions.assertEquals("driver-1", response.getDeliveryDriverId());
        Assertions.assertFalse(response.getRetryFound());
        Assertions.assertNull(response.getPcRetry());
        Assertions.assertNull(response.getRequestId());
        verifyNoInteractions(addressDAO);
        verifyNoInteractions(externalChannelClient);
    }

    @Test
    void callInitTrackingAndEcSendEngage_WhenProductTracked_ShouldCallInitTrackingAndSendEngage() {
        String requestId = "REQ123";
        String pcRetry = "1";
        PnDeliveryRequest pnDeliveryRequest = getPnDeliveryRequest();
        SendRequest sendRequest = new SendRequest();
        List<AttachmentInfo> attachmentInfos = new ArrayList<>();
        PaperChannelDeliveryDriver driver = new PaperChannelDeliveryDriver();
        driver.setUnifiedDeliveryDriver("poste");

        when(config.getPaperTrackerProductList()).thenReturn(List.of("AR"));
        when(paperChannelDeliveryDriverDAO.getByDeliveryDriverId("driver1"))
                .thenReturn(Mono.just(driver));
        when(paperTrackerClient.initPaperTracking(any(), any(), any(), any())).thenReturn(Mono.empty());
        when(externalChannelClient.sendEngageRequest(any(), any(), any())).thenReturn(Mono.empty());

        pcRetryUtils.callInitTrackingAndEcSendEngage(requestId, sendRequest, attachmentInfos, pnDeliveryRequest, pcRetry).block();

        verify(paperChannelDeliveryDriverDAO).getByDeliveryDriverId("driver1");
        verify(paperTrackerClient).initPaperTracking(requestId, "PCRETRY_1", "AR", "poste");
        verify(externalChannelClient).sendEngageRequest(sendRequest, attachmentInfos, pnDeliveryRequest.getApplyRasterization());
    }

    @Test
    void callInitTrackingAndEcSendEngage_WhenProductNotTracked_ShouldCallOnlySendEngage() {
        String requestId = "REQ123";
        String pcRetry = "1";
        PnDeliveryRequest pnDeliveryRequest = getPnDeliveryRequest();
        pnDeliveryRequest.setProductType("RIR");
        SendRequest sendRequest = new SendRequest();
        List<AttachmentInfo> attachmentInfos = new ArrayList<>();

        when(config.getPaperTrackerProductList()).thenReturn(List.of("AR"));
        when(externalChannelClient.sendEngageRequest(any(), any(), any())).thenReturn(Mono.empty());

        pcRetryUtils.callInitTrackingAndEcSendEngage(requestId, sendRequest, attachmentInfos, pnDeliveryRequest, pcRetry).block();

        verifyNoInteractions(paperChannelDeliveryDriverDAO);
        verifyNoInteractions(paperTrackerClient);
        verify(externalChannelClient).sendEngageRequest(sendRequest, attachmentInfos, pnDeliveryRequest.getApplyRasterization());
    }

    @Test
    void callInitTrackingAndEcSendEngage_WhenInitTrackingFails_ShouldLogErrorAndThrowException() {
        String requestId = "REQ123";
        String pcRetry = "1";
        PnDeliveryRequest pnDeliveryRequest = getPnDeliveryRequest();
        SendRequest sendRequest = new SendRequest();
        List<AttachmentInfo> attachmentInfos = new ArrayList<>();
        PaperChannelDeliveryDriver driver = new PaperChannelDeliveryDriver();
        driver.setUnifiedDeliveryDriver("poste");

        when(config.getPaperTrackerProductList()).thenReturn(List.of("AR"));
        when(paperChannelDeliveryDriverDAO.getByDeliveryDriverId("driver1"))
                .thenReturn(Mono.just(driver));
        when(paperTrackerClient.initPaperTracking(any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Init tracking failed")));

        StepVerifier.create(pcRetryUtils.callInitTrackingAndEcSendEngage(requestId, sendRequest, attachmentInfos, pnDeliveryRequest, pcRetry))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Init tracking failed"))
                .verify();

        verify(paperChannelDeliveryDriverDAO).getByDeliveryDriverId("driver1");
        verify(paperTrackerClient).initPaperTracking(requestId, "PCRETRY_1", "AR", "poste");
        verifyNoInteractions(externalChannelClient);
    }

    private PnDeliveryRequest getPnDeliveryRequest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId1");
        pnDeliveryRequest.setDriverCode("driver1");
        pnDeliveryRequest.setProductType("AR");
        pnDeliveryRequest.setAttachments(new ArrayList<>());
        return pnDeliveryRequest;
    }
}
