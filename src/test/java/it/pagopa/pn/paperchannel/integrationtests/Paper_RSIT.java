package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.BeforeAll;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    private PaperProgressStatusEventDto createSimpleAnalogMail() {
        PaperProgressStatusEventDto analogMail = new PaperProgressStatusEventDto();
        analogMail.requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        analogMail.setClientRequestTimeStamp(OffsetDateTime.now());
        analogMail.setStatusDateTime(OffsetDateTime.now());
        analogMail.setStatusCode("RECRS002C");
        analogMail.setProductType("RS");
        analogMail.setStatusDescription("OK");

        return analogMail;
    }

    private PnDeliveryRequest createPnDeliveryRequest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_KREP-VHAD-TAQV-202302-P-1.RECINDEX_0.SENTATTEMPTMADE_1");
        pnDeliveryRequest.setCorrelationId("Self=1-63fe1166-09f74e174d4e13d26f7d08c0;Root=1-63fe1166-cdf14290b52666124be856be;Parent=a3bb560233ceb4ec;Sampled=1");
        pnDeliveryRequest.setFiscalCode("PF-a6c1350d-1d69-4209-8bf8-31de58c79d6e");
        pnDeliveryRequest.setHashedFiscalCode("81af12154dfaf8094715acadc8065fdde56c31fb52a9d1766f8f83470262c13a");
        pnDeliveryRequest.setHashOldAddress("60cba8d6dda57ac74ec15e5a4b78402672883ecdffdb01d1f19501cba176f7254b803f38a0359c42d8fe8459d0a6ecac8ca9e7539a64df346290c966dc9845444dee871c93f2d2d33a691daa7a5c75b10f504efc91a03dcb3882744f9");
        pnDeliveryRequest.setIun("KREP-VHAD-TAQV-202302-P-1");
        pnDeliveryRequest.setPrintType("BN_FRONTE_RETRO");
        pnDeliveryRequest.setProposalProductType("890");
        pnDeliveryRequest.setReceiverType("PF");
        pnDeliveryRequest.setRelatedRequestId("PREPARE_ANALOG_DOMICILE.IUN_KREP-VHAD-TAQV-202302-P-1.RECINDEX_0.SENTATTEMPTMADE_0");
        pnDeliveryRequest.setStartDate("2023-02-28T15:36:22.225");
        pnDeliveryRequest.setStatusCode("PROGRESS");
        pnDeliveryRequest.setStatusDate("2023-02-28T15:36:22.29");
        pnDeliveryRequest.setStatusDetail("In attesa di indirizzo da National Registry");

        return pnDeliveryRequest;
    }

    @DirtiesContext
    @Test
    void Test_RS_Delivered__RECRS001C(){
        // final only -> send to delivery push

        // RECRS001C
        PnDeliveryRequest pnDeliveryRequest = createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = createSimpleAnalogMail();
        analogMail.setStatusCode("RECRS001C");

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = createPnDeliveryRequest();
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

    @DirtiesContext
    @Test
    void Test_RS_NotDelivered__RECRS002A_RECRS002B_RECRS002C(){
        // meta, demat, final (send to delivery push)
        // ...
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push

        /*
        PnDeliveryRequest pnDeliveryRequest = createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = new PaperProgressStatusEventDto();
        analogMail.requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        analogMail.setClientRequestTimeStamp(OffsetDateTime.now());
        analogMail.setStatusDateTime(OffsetDateTime.now());
        analogMail.setStatusCode("RECRS002B");
        analogMail.setProductType("RS");
        analogMail.setStatusDescription("In progress");
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

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = createPnDeliveryRequest();
        afterSetForUpdate.setStatusCode(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDetail(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());


        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        System.out.println(caturedSendEvent.getAllValues());
         */
    }

    @DirtiesContext
    @Test
    void Test_RS_AbsoluteUntraceability__RECRS002D_RECRS002E_RECRS002F(){
        // meta, demat, final (send to delivery push)
        // ...
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_RS_DeliveredToStorage__RECRS003C(){
        // final only -> send to delivery push

        // RECRS003C
        PnDeliveryRequest pnDeliveryRequest = createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = createSimpleAnalogMail();
        analogMail.setStatusCode("RECRS003C");

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = createPnDeliveryRequest();
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

    @DirtiesContext
    @Test
    void Test_RS_RefusedToStorage__RECRS004A_RECRS004B_RECRS004C(){
        // meta, demat, final (send to delivery push)
        // ...
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_RS_CompletedStorage__RECRS005A_RECRS005B_RECRS005C(){
        // meta, demat, final (send to delivery push)
        // ...
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_RS_TheftLossDeterioration__RECRS006__RetryPC(){
        // retry paper channel
        // ...
    }
}
