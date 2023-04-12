package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class Paper_RS_AR_IT extends BaseTest {

    @Autowired
    private PaperResultAsyncService paperResultAsyncService;

    @MockBean
    private SqsSender sqsSender;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    private void CommonFinalOnlyRSSequenceTest(String event) {
        // event (final only)
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(event);

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusCode(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDetail(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        // verifico che è stato inviato un evento a delivery-push
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
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
        afterSetForUpdate.setStatusCode(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDetail(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));

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
                        .url("https://safestorage.it"))
        );

        SingleStatusUpdateDto extChannelMessage2 = new SingleStatusUpdateDto();
        extChannelMessage2.setAnalogMail(analogMail);

        afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusCode(ExternalChannelCodeEnum.getStatusCode(extChannelMessage2.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDetail(extChannelMessage2.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage2.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage2.getAnalogMail().getStatusDateTime().toInstant())));

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
        afterSetForUpdate.setStatusCode(ExternalChannelCodeEnum.getStatusCode(extChannelMessage3.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDetail(extChannelMessage3.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage3.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage3.getAnalogMail().getStatusDateTime().toInstant())));

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
    void Test_RS_Delivered__RECRS001C(){
        // final only -> send to delivery push

        CommonFinalOnlyRSSequenceTest("RECRS001C");
    }

    @Test
    void Test_RS_NotDelivered__RECRS002A_RECRS002B_RECRS002C(){
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRS002A", "RECRS002B", "RECRS002C", StatusCodeEnum.KO, true, false);
    }

    @Test
    void Test_RS_AbsoluteUntraceability__RECRS002D_RECRS002E_RECRS002F(){
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRS002D", "RECRS002E", "RECRS002F", StatusCodeEnum.KO, true, false);
    }

    @Test
    void Test_RS_DeliveredToStorage__RECRS003C(){
        // final only -> send to delivery push

        CommonFinalOnlyRSSequenceTest("RECRS003C");
    }

    @Test
    void Test_RS_RefusedToStorage__RECRS004A_RECRS004B_RECRS004C(){
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRS004A", "RECRS004B", "RECRS004C", StatusCodeEnum.OK,false, false);
    }

    @Test
    void Test_RS_CompletedStorage__RECRS005A_RECRS005B_RECRS005C(){
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRS005A", "RECRS005B", "RECRS005C", StatusCodeEnum.OK,false, false);
    }

    @Test
    void Test_RS_TheftLossDeterioration__RECRS006__RetryPC(){
        // retry paper channel
        // ...
    }

    // ******** AR ********

    @Test
    void Test_AR_Delivered__RECRN001A_RECRN001B_RECRN001C(){
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN001A", "RECRN001B", "RECRN001C", StatusCodeEnum.OK,false, false);
    }

    @Test
    void Test_AR_NotDelivered__RECRN002A_RECRN002B_RECRN002C(){
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN002A", "RECRN002B", "RECRN002C", StatusCodeEnum.KO, true, false);
    }

    @Test
    void Test_AR_AbsoluteUntraceability__RECRN002D_RECRN002E_RECRN002F(){
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        // optional discoveredAddress
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN002D", "RECRN002E", "RECRN002F", StatusCodeEnum.KO, true, true);
    }

    @Test
    void Test_AR_DeliveredToStorage__RECRN003A_RECRN003B_RECRN003C(){
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN003A", "RECRN003B", "RECRN003C", StatusCodeEnum.OK, false, false);
    }

    @Test
    void Test_AR_RefusedToStorage__RECRN004A_RECRN004B_RECRN004C(){
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN004A", "RECRN004B", "RECRN004C", StatusCodeEnum.KO, false, false);
    }

    @Test
    void Test_AR_CompletedStorage__RECRN005A_RECRN005B_RECRN005C(){
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateSequenceTest("RECRN005A", "RECRN005B", "RECRN005C", StatusCodeEnum.OK,false, false);
    }

    @Test
    void Test_AR_TheftLossDeterioration__RECRN006__RetryPC(){
        // retry paper channel
        // ...
    }
}
