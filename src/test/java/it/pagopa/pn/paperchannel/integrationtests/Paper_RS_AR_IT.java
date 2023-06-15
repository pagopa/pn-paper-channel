package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.RECRN011_STATUS_CODE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
class Paper_RS_AR_IT extends BaseTest {

    @Autowired
    private PaperResultAsyncService paperResultAsyncService;

    @MockBean
    private SqsSender sqsSender;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private ExternalChannelClient mockExtChannel;

    @MockBean
    private AddressDAO mockAddressDAO;

    private void CommonFinalOnlySequenceTest(String event, StatusCodeEnum statusToCheck) {
        // event (final only)
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(event);

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDescription(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));
        afterSetForUpdate.setStatusCode(extChannelMessage.getAnalogMail().getStatusCode());
        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        // verifico che è stato inviato un evento a delivery-push
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(statusToCheck, caturedSendEvent.getValue().getStatusCode());
    }

    private void CommonFinalOnlyRetrySequenceTest(String event, boolean checkDeliveryFailureCauseEnrichment) {
        final String deliveryFailureCause = "F04"; // robbed

        // event (final only)
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(event);

        if (checkDeliveryFailureCauseEnrichment) {
            analogMail.setDeliveryFailureCause(deliveryFailureCause);
        }

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();

        afterSetForUpdate.setProductType("RS"); // product type

        var attachment = new PnAttachmentInfo();
        attachment.setDocumentType("Plico");
        attachment.setDate(OffsetDateTime.now().toString());
        attachment.setUrl("https://safestorage.it");
        afterSetForUpdate.setAttachments(List.of(attachment));

        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDescription(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));
        afterSetForUpdate.setStatusCode(extChannelMessage.getAnalogMail().getStatusCode());
        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");

        when(mockAddressDAO.findAllByRequestId(anyString())).thenReturn(Mono.just(List.of(pnAddress)));
        when(mockExtChannel.sendEngageRequest(any(SendRequest.class), anyList())).thenReturn(Mono.empty());

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        // verifico che viene invocato ext-channels
        verify(mockExtChannel, timeout(2000).times(1)).sendEngageRequest(any(SendRequest.class), anyList());

        // verifico che è stato inviato un evento a delivery-push
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

        if (checkDeliveryFailureCauseEnrichment) {
            assertEquals(deliveryFailureCause, caturedSendEvent.getValue().getDeliveryFailureCause());
        } else {
            assertNull(caturedSendEvent.getValue().getDeliveryFailureCause());
        }
    }

    private void SaveMetaRECRN011Status(){
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(RECRN011_STATUS_CODE);
        analogMail.setProductType("RS");
        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);


        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDescription(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));
        afterSetForUpdate.setStatusCode(extChannelMessage.getAnalogMail().getStatusCode());
        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso della Giacenza RECRN011 è terminato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender).pushSendEvent(caturedSendEvent.capture());

        assertNotNull(caturedSendEvent.getValue());
        assertEquals(RECRN011_STATUS_CODE, caturedSendEvent.getValue().getStatusDetail());
    }

    private void CommonMetaDematAggregateSequenceTest(String event1, String event2, String event3, StatusCodeEnum expectedStatusCode, boolean checkDeliveryFailureCauseEnrichment, boolean checkDiscoveredAddress) {
        final String deliveryFailureCause = "M06"; // wrong address

        // 1. event1 - save meta
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(event1);
        analogMail.setProductType("RS");

        if (checkDeliveryFailureCauseEnrichment) {
            analogMail.setDeliveryFailureCause(deliveryFailureCause);
        }
        String addressLine = "discoveredAddress";
        if (checkDiscoveredAddress) {
            PnDiscoveredAddress address = new PnDiscoveredAddress();
            address.setAddress(addressLine);

            DiscoveredAddressDto discoveredAddressDto =
                    new BaseMapperImpl<>(PnDiscoveredAddress.class, DiscoveredAddressDto.class)
                            .toDTO(address);

            analogMail.setDiscoveredAddress(discoveredAddressDto);
        }

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDescription(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));
        afterSetForUpdate.setStatusCode(extChannelMessage.getAnalogMail().getStatusCode());
        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());


        // 2. event2 - save demat
        pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(event2);
        analogMail.setProductType("RS");
        analogMail.setAttachments(List.of(
                new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(OffsetDateTime.now())
                        .uri("https://safestorage.it"))
        );

        SingleStatusUpdateDto extChannelMessage2 = new SingleStatusUpdateDto();
        extChannelMessage2.setAnalogMail(analogMail);

        afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage2.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDescription(extChannelMessage2.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage2.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage2.getAnalogMail().getStatusDateTime().toInstant())));
        afterSetForUpdate.setStatusCode(extChannelMessage2.getAnalogMail().getStatusCode());
        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage2, 0).block());

        // check PROGRESS
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture()); // 1 send for each attachment of the correct type

        assertEquals(pnDeliveryRequest.getRequestId(), caturedSendEvent.getValue().getRequestId());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

        // sent attachments
        var caturedAttachments = caturedSendEvent.getValue().getAttachments();
        assertNotNull(caturedAttachments);
        assertEquals(1, caturedAttachments.size());
        assertEquals("Plico", caturedAttachments.get(0).getDocumentType());
        assertEquals("https://safestorage.it", caturedAttachments.get(0).getUrl());

        Mockito.reset(sqsSender);


        // 3. event3 - send to push, enriched
        pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(event3);
        analogMail.setProductType("RS");
        analogMail.setDeliveryFailureCause(deliveryFailureCause);

        SingleStatusUpdateDto extChannelMessage3 = new SingleStatusUpdateDto();
        extChannelMessage3.setAnalogMail(analogMail);

        afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage3.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDescription(extChannelMessage3.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage3.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage3.getAnalogMail().getStatusDateTime().toInstant())));
        afterSetForUpdate.setStatusCode(extChannelMessage3.getAnalogMail().getStatusCode());
        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage3, 0).block());

        caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(pnDeliveryRequest.getRequestId(), caturedSendEvent.getValue().getRequestId());

        assertEquals(expectedStatusCode, caturedSendEvent.getValue().getStatusCode());

        if (checkDeliveryFailureCauseEnrichment) {
            assertEquals(deliveryFailureCause, caturedSendEvent.getValue().getDeliveryFailureCause());
        } else {
            assertNull(caturedSendEvent.getValue().getDeliveryFailureCause());
        }

        if (checkDiscoveredAddress) {
            assertNotNull(caturedSendEvent.getValue().getDiscoveredAddress());
            assertEquals(addressLine, caturedSendEvent.getValue().getDiscoveredAddress().getAddress());
        }
    }

    // ******** RS ********

    @Test
    void Test_RS_Delivered__RECRS001C() {
        // final only -> send to delivery push

        CommonFinalOnlySequenceTest("RECRS001C", StatusCodeEnum.OK);
    }

    @Test
    void Test_RS_NotDelivered__RECRS002A_RECRS002B_RECRS002C() {
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRS002A", "RECRS002B", "RECRS002C", StatusCodeEnum.KO, true, false);
    }

    @Test
    void Test_RS_AbsoluteUntraceability__RECRS002D_RECRS002E_RECRS002F() {
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRS002D", "RECRS002E", "RECRS002F", StatusCodeEnum.KO, true, false);
    }

    @Test
    void Test_RS_DeliveredToStorage__RECRS003C() {
        // final only -> send to delivery push

        CommonFinalOnlySequenceTest("RECRS003C", StatusCodeEnum.OK);
    }

    @Test
    void Test_RS_RefusedToStorage__RECRS004A_RECRS004B_RECRS004C() {
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRS004A", "RECRS004B", "RECRS004C", StatusCodeEnum.KO,false, false);
    }

    @Test
    void Test_RS_CompletedStorage__RECRS005A_RECRS005B_RECRS005C() {
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRS005A", "RECRS005B", "RECRS005C", StatusCodeEnum.KO,false, false);
    }

    @Test
    void Test_RS_TheftLossDeteriorationRobbed__RECRS006__RetryPC() {
        // retry paper channel
        //
        // deliveryFailureCause
        //
        // progress + retry

        CommonFinalOnlyRetrySequenceTest("RECRS006", true);
    }

    @Test
    void Test_RS_NotAccountable__RECRS013__RetryPC() {
        // retry paper channel
        //
        // progress + retry

        CommonFinalOnlyRetrySequenceTest("RECRS013", false);
    }

    @Test
    void Test_RS_MajorCause__RECRS015() {
        // final only -> send to delivery push
        //
        // progress

        CommonFinalOnlySequenceTest("RECRS015", StatusCodeEnum.PROGRESS);
    }

    // ******** AR ********

    @Test
    void Test_AR_Delivered__RECRN001A_RECRN001B_RECRN001C() {
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN001A", "RECRN001B", "RECRN001C", StatusCodeEnum.OK,false, false);
    }

    @Test
    void Test_AR_NotDelivered__RECRN002A_RECRN002B_RECRN002C() {
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN002A", "RECRN002B", "RECRN002C", StatusCodeEnum.KO, true, false);
    }

    @Test
    void Test_AR_AbsoluteUntraceability__RECRN002D_RECRN002E_RECRN002F() {
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        // optional discoveredAddress
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN002D", "RECRN002E", "RECRN002F", StatusCodeEnum.KO, true, true);
    }

    @Test
    void Test_AR_DeliveredToStorage__RECRN003A_RECRN003B_RECRN003C() {
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push
        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent(RECRN011_STATUS_CODE, null, null, null, Instant.now().minus(20, ChronoUnit.DAYS));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());
        log.info("Event: \n"+capturedSendEvent.getAllValues());
        assertNotNull(capturedSendEvent.getValue());
        assertEquals(StatusCodeEnum.PROGRESS, capturedSendEvent.getValue().getStatusCode());
        assertEquals(RECRN011_STATUS_CODE, capturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECRN003A", null, null, null, Instant.now().minus(5, ChronoUnit.DAYS));
        generateEvent("RECRN003B", null, null, List.of("safe-storage://123ABCDOC"), Instant.now().minus(5, ChronoUnit.DAYS));
        generateEvent("RECRN003C", null, null, null, Instant.now().minus(5, ChronoUnit.DAYS));

        ArgumentCaptor<SendEvent> captureSecond = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, times(2)).pushSendEvent(captureSecond.capture());
        assertNotNull(captureSecond.getValue());
        assertEquals(StatusCodeEnum.PROGRESS, captureSecond.getValue().getStatusCode());
        assertEquals("PNRN012", captureSecond.getValue().getStatusDetail());
    }

    @Test
    void Test_AR_RefusedToStorage__RECRN004A_RECRN004B_RECRN004C() {
        // meta RECRN011
        //
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push
        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent(RECRN011_STATUS_CODE, null, null, null, Instant.now().minus(20, ChronoUnit.DAYS));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());
        log.info("Event: \n"+capturedSendEvent.getAllValues());
        assertNotNull(capturedSendEvent.getValue());
        assertEquals(StatusCodeEnum.PROGRESS, capturedSendEvent.getValue().getStatusCode());
        assertEquals(RECRN011_STATUS_CODE, capturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECRN004A", null, null, null, Instant.now().minus(5, ChronoUnit.DAYS));
        generateEvent("RECRN004B", null, null, List.of("safe-storage://123ABCDOC"), Instant.now().minus(5, ChronoUnit.DAYS));
        generateEvent("RECRN004C", null, null, null, Instant.now().minus(5, ChronoUnit.DAYS));

        ArgumentCaptor<SendEvent> captureSecond = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, times(2)).pushSendEvent(captureSecond.capture());
        assertNotNull(captureSecond.getValue());
        assertEquals(StatusCodeEnum.PROGRESS, captureSecond.getValue().getStatusCode());
        assertEquals("PNRN012", captureSecond.getValue().getStatusDetail());

    }


    @Test
    void Test_AR_CompletedStorage__RECRN005A_RECRN005B_RECRN005C() {
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push
        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent(RECRN011_STATUS_CODE, null, null, null, Instant.now().minus(20, ChronoUnit.DAYS));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());
        log.info("Event: \n"+capturedSendEvent.getAllValues());
        assertNotNull(capturedSendEvent.getValue());
        assertEquals(StatusCodeEnum.PROGRESS, capturedSendEvent.getValue().getStatusCode());
        assertEquals(RECRN011_STATUS_CODE, capturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECRN005A", "MA", null, null, Instant.now().minus(5, ChronoUnit.DAYS));
        generateEvent("RECRN005B", "MA", null, List.of("safe-storage://123ABCDOC"), Instant.now().minus(5, ChronoUnit.DAYS));
        generateEvent("RECRN005C", "MA", null, null, Instant.now().minus(5, ChronoUnit.DAYS));

        ArgumentCaptor<SendEvent> captureSecond = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, times(2)).pushSendEvent(captureSecond.capture());
        assertNotNull(captureSecond.getValue());
        assertEquals(StatusCodeEnum.PROGRESS, captureSecond.getValue().getStatusCode());
        assertEquals("PNRN012", captureSecond.getValue().getStatusDetail());

    }

    @Test
    void Test_AR_TheftLossDeteriorationRobbed__RECRN006__RetryPC() {
        // retry paper channel
        //
        // deliveryFailureCause
        //
        // progress + retry

        CommonFinalOnlyRetrySequenceTest("RECRN006", true);
    }

    @Test
    void Test_AR_NotAccountable__RECRN013__RetryPC() {
        // retry paper channel
        //
        // progress + retry

        CommonFinalOnlyRetrySequenceTest("RECRN013", false);
    }

    @Test
    void Test_RS_MajorCause__RECRN015() {
        // final only -> send to delivery push
        //
        // progress

        CommonFinalOnlySequenceTest("RECRN015", StatusCodeEnum.PROGRESS);
    }



    private void generateEvent(String statusCode, String deliveryFailureCause, String discoveredAddress, List<String> attach, Instant statusDateTimeToSet){
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();
        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setRequestId(pnDeliveryRequest.getRequestId());
        analogMail.setStatusCode(statusCode);
        analogMail.setStatusDescription(ExternalChannelCodeEnum.getStatusCode(statusCode));
        analogMail.setProductType("AR");

        if (statusDateTimeToSet != null) {
            analogMail.setStatusDateTime(OffsetDateTime.ofInstant(statusDateTimeToSet, ZoneOffset.UTC));
        }

        if(deliveryFailureCause != null && !deliveryFailureCause.trim().equalsIgnoreCase("")){
            analogMail.setDeliveryFailureCause(deliveryFailureCause);
        }

        if(attach != null && attach.size() > 0){
            List<AttachmentDetailsDto> attachments = new LinkedList<>();
            for(String elem: attach){
                attachments.add(
                        new AttachmentDetailsDto()
                                .documentType(elem)
                                .date(OffsetDateTime.now())
                                .uri("https://safestorage.it"));
            }
            analogMail.setAttachments(attachments);
        }

        if (discoveredAddress != null && !discoveredAddress.trim().equalsIgnoreCase("")) {
            PnDiscoveredAddress address = new PnDiscoveredAddress();
            address.setAddress(discoveredAddress);

            DiscoveredAddressDto discoveredAddressDto =
                    new BaseMapperImpl<>(PnDiscoveredAddress.class, DiscoveredAddressDto.class)
                            .toDTO(address);

            analogMail.setDiscoveredAddress(discoveredAddressDto);
        }



        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDescription(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));

        afterSetForUpdate.setStatusCode(extChannelMessage.getAnalogMail().getStatusCode());

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());


    }


}
