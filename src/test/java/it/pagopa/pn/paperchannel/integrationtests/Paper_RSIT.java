package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
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
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class Paper_RSIT extends BaseTest {

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

        // verifico che è stato inviato un evento a delivery-push
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));
    }

    private void CommonMetaDematAggregateRSSequenceTest(String event1, String event2, String event3, boolean checkDeliveryFailureCauseEnrichment) {
        final String deliveryFailureCause = "M06"; // wrong address

        // 1. event1 - save meta
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(event1);
        analogMail.setProductType("RS");
        if (checkDeliveryFailureCauseEnrichment) {
            analogMail.setDeliveryFailureCause(deliveryFailureCause);
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
                        .documentType("CAD")
                        .date(OffsetDateTime.now())
                        .url("https://safestorage.it"),
                new AttachmentDetailsDto()
                        .documentType("23L")
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

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

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

        if (checkDeliveryFailureCauseEnrichment) {
            assertEquals(deliveryFailureCause, caturedSendEvent.getValue().getDeliveryFailureCause());
        } else {
            assertNull(caturedSendEvent.getValue().getDeliveryFailureCause());
        }
    }

    @DirtiesContext
    @Test
    void Test_RS_Delivered__RECRS001C(){
        // final only -> send to delivery push

        CommonFinalOnlyRSSequenceTest("RECRS001C");
    }

    @DirtiesContext
    @Test
    void Test_RS_NotDelivered__RECRS002A_RECRS002B_RECRS002C(){
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateRSSequenceTest("RECRS002A", "RECRS002B", "RECRS002C", true);
    }

    @DirtiesContext
    @Test
    void Test_RS_AbsoluteUntraceability__RECRS002D_RECRS002E_RECRS002F(){
        // meta, demat, final (send to delivery push)
        //
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateRSSequenceTest("RECRS002D", "RECRS002E", "RECRS002F", true);
    }

    @DirtiesContext
    @Test
    void Test_RS_DeliveredToStorage__RECRS003C(){
        // final only -> send to delivery push

        CommonFinalOnlyRSSequenceTest("RECRS003C");
    }

    @DirtiesContext
    @Test
    void Test_RS_RefusedToStorage__RECRS004A_RECRS004B_RECRS004C(){
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateRSSequenceTest("RECRS004A", "RECRS004B", "RECRS004C", false);
    }

    @DirtiesContext
    @Test
    void Test_RS_CompletedStorage__RECRS005A_RECRS005B_RECRS005C(){
        // meta, demat, final (send to delivery push)
        //
        //
        // demat PROGRESS -> send to delivery push

        CommonMetaDematAggregateRSSequenceTest("RECRS004A", "RECRS004B", "RECRS004C", false);
    }

    @DirtiesContext
    @Test
    void Test_RS_TheftLossDeterioration__RECRS006__RetryPC(){
        // retry paper channel
        // ...
    }
}
