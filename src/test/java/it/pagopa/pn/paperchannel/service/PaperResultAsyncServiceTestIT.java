package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PaperResultAsyncServiceTestIT extends BaseTest {

    @Autowired
    private PaperResultAsyncService paperResultAsyncService;

    @Autowired
    private EventMetaDAO eventMetaDAO;

    @Autowired
    private EventDematDAO eventDematDAO;

    @MockBean
    private SqsSender sqsSender;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    @DirtiesContext
    @Test
    void testMetadataEvent() {
        final String metadataRequestExpected = "META##PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0";
        final String metadataStatusCodeExpected = "META##RECRS002A";
        PnEventMeta entity = eventMetaDAO.getDeliveryEventMeta(metadataRequestExpected, metadataStatusCodeExpected).block();
        assertThat(entity).isNull();
        PnDeliveryRequest pnDeliveryRequest = createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = new PaperProgressStatusEventDto();
        analogMail.requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        analogMail.setClientRequestTimeStamp(OffsetDateTime.now());
        analogMail.setStatusDateTime(OffsetDateTime.now());
        analogMail.setStatusCode("RECRS002A");
        analogMail.setProductType("RS");
        analogMail.setStatusDescription("In progress");

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

        // verifico che è stato inserito il record in DB
        entity = eventMetaDAO.getDeliveryEventMeta(metadataRequestExpected, metadataStatusCodeExpected).block();
        assertThat(entity).isNotNull();

    }

    @DirtiesContext
    @Test
    void testDematEvent() {
        final String metadataRequestExpected = "DEMAT##PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0";
        final String metadataStatusCodeCADExpected = "CAD##RECRS002B";
        final String metadataStatusCode23LExpected = "23L##RECRS002B";
        PnEventDemat entityCAD = eventDematDAO.getDeliveryEventDemat(metadataRequestExpected, metadataStatusCodeCADExpected).block();
        PnEventDemat entity23L = eventDematDAO.getDeliveryEventDemat(metadataRequestExpected, metadataStatusCode23LExpected).block();
        assertThat(entityCAD).isNull();
        assertThat(entity23L).isNull();
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

        // verifico che è stato inserito il record CAD in DB
        entityCAD = eventDematDAO.getDeliveryEventDemat(metadataRequestExpected, metadataStatusCodeCADExpected).block();
        assertThat(entityCAD).isNotNull();

        // verifico che è stato inserito il record 23L in DB
        entity23L = eventDematDAO.getDeliveryEventDemat(metadataRequestExpected, metadataStatusCode23LExpected).block();
        assertThat(entity23L).isNotNull();

        // verifico che è stato inviato 1 solo evento a delivery-push
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));

    }

    @DirtiesContext
    @Test
    void testEsitoFinalEvent() {
        PnDeliveryRequest pnDeliveryRequest = createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = createSimpleAnalogMail();

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = createPnDeliveryRequest();
        afterSetForUpdate.setStatusCode(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDetail(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // inserimento 1 meta
        PnEventMeta eventMeta = createPnEventMeta(analogMail);
        eventMetaDAO.createOrUpdate(eventMeta).block();

        PnEventMeta eventMetaFromDb = eventMetaDAO.getDeliveryEventMeta(eventMeta.getMetaRequestId(), eventMeta.getMetaStatusCode()).block();
        assertNotNull(eventMetaFromDb);

        // inserimento 1 demat
        PnEventDemat eventDemat = createPnEventDemat(analogMail);
        eventDematDAO.createOrUpdate(eventDemat).block();

        PnEventDemat eventDematFromDB = eventDematDAO.getDeliveryEventDemat(eventDemat.getDematRequestId(), eventDemat.getDocumentTypeStatusCode()).block();
        assertNotNull(eventDematFromDB);

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());

        // verifico che è stato inviato un evento a delivery-push
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));

        // verifica cancellazione evento meta
        eventMetaFromDb = eventMetaDAO.getDeliveryEventMeta(eventMeta.getMetaRequestId(), eventMeta.getMetaStatusCode()).block();
        assertNull(eventMetaFromDb);

        // verifica cancellazione evento demat
        eventDematFromDB = eventDematDAO.getDeliveryEventDemat(eventDemat.getDematRequestId(), eventDemat.getDocumentTypeStatusCode()).block();
        assertNull(eventDematFromDB);
    }

    @DirtiesContext
    @Test
    void testEsitoFinalEventSendWithoutFindingPreviousEvents() {
        PnDeliveryRequest pnDeliveryRequest = createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = createSimpleAnalogMail();

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
    void testEsitoFinalEvent_RECRS001C_RECRS003C() {
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

    private PnEventMeta createPnEventMeta(PaperProgressStatusEventDto analogMail) {
        int ttlOffsetDays = 365;

        PnEventMeta eventMeta = new PnEventMeta();
        PnDiscoveredAddress address1 = new PnDiscoveredAddress();
        address1.setAddress("discoveredAddress1");
        eventMeta.setMetaRequestId("META##" + analogMail.getRequestId());
        eventMeta.setMetaStatusCode("META##" + analogMail.getStatusCode());
        eventMeta.setRequestId(analogMail.getRequestId());
        eventMeta.setStatusCode(analogMail.getStatusCode());
        eventMeta.setDiscoveredAddress(address1);
        eventMeta.setDeliveryFailureCause("failureCause1");
        eventMeta.setStatusDateTime(analogMail.getStatusDateTime().toInstant());
        eventMeta.setTtl(analogMail.getStatusDateTime().toInstant().plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        return eventMeta;
    }

    private PnEventDemat createPnEventDemat(PaperProgressStatusEventDto analogMail) {
        int ttlOffsetDays = 365;

        PnEventDemat eventDemat = new PnEventDemat();
        eventDemat.setDematRequestId("DEMAT##" + analogMail.getRequestId());
        eventDemat.setDocumentTypeStatusCode(analogMail.getProductType() + "##" + analogMail.getStatusCode());
        eventDemat.setRequestId(analogMail.getRequestId());
        eventDemat.setDocumentType(analogMail.getProductType());
        eventDemat.setStatusCode(analogMail.getStatusCode());
        eventDemat.setDocumentDate(analogMail.getStatusDateTime().toInstant().plusSeconds(-5));
        eventDemat.setStatusDateTime(analogMail.getStatusDateTime().toInstant());
        eventDemat.setUri("uri1");
        eventDemat.setTtl(analogMail.getStatusDateTime().toInstant().plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());

        return eventDemat;
    }

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
}