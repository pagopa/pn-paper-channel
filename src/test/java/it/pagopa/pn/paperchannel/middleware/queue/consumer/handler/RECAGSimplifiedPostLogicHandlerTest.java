package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DematDocumentTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class RECAGSimplifiedPostLogicHandlerTest {


    private EventMetaDAO eventMetaDAO;

    private EventDematDAO eventDematDAO;

    private SqsSender mockSqsSender;

    private RequestDeliveryDAO requestDeliveryDAO;
    private PnPaperChannelConfig pnPaperChannelConfig;

    private RECAGSimplifiedPostLogicHandler recagSimplifiedPostLogicHandler;

    @BeforeEach
    public void init(){
        long ttlDays = 365;
        Set<String> requiredDemats = Set.of(
                DematDocumentTypeEnum.DEMAT_23L.getDocumentType(),
                DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType()
        );

        eventDematDAO = mock(EventDematDAO.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        mockSqsSender = mock(SqsSender.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);

        pnPaperChannelConfig = new PnPaperChannelConfig();
        pnPaperChannelConfig.setTtlExecutionDaysDemat(ttlDays);
        pnPaperChannelConfig.setTtlExecutionDaysMeta(ttlDays);
        pnPaperChannelConfig.setZipHandleActive(false);
        pnPaperChannelConfig.setRequiredDemats(requiredDemats);

        recagSimplifiedPostLogicHandler = RECAGSimplifiedPostLogicHandler.builder()
                .sqsSender(mockSqsSender)
                .eventDematDAO(eventDematDAO)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
    }

    @Test
    void handleMessage_missingdemat() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("req1234");

        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setStatusCode("RECAG012");

        List<PnEventDemat> resdemat = new ArrayList<>();
        PnEventDemat pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("23L");
        resdemat.add(pnEventDemat);


        Mockito.when(eventDematDAO.findAllByRequestId(Mockito.anyString())).thenReturn(Flux.fromIterable(resdemat));


        Mono<Void> mono = recagSimplifiedPostLogicHandler.handleMessage(pnDeliveryRequest, paperProgressStatusEventDto);

        StepVerifier.create(mono)
                .verifyComplete();

        Mockito.verify(eventMetaDAO, never()).getDeliveryEventMeta(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockSqsSender, never()).pushSendEvent(Mockito.any());

    }


    @Test
    void handleMessage_missing_recag12() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("req1234");

        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setStatusCode("RECAG008B");

        List<PnEventDemat> resdemat = new ArrayList<>();
        PnEventDemat pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("23L");
        pnEventDemat.setStatusCode("RECAG008B");
        resdemat.add(pnEventDemat);
        pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("ARCAD");
        pnEventDemat.setStatusCode("RECAG008B");
        resdemat.add(pnEventDemat);




        Mockito.when(eventDematDAO.findAllByRequestId(Mockito.anyString())).thenReturn(Flux.fromIterable(resdemat));
        Mockito.when(eventMetaDAO.getDeliveryEventMeta(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());


        Mono<Void> mono = recagSimplifiedPostLogicHandler.handleMessage(pnDeliveryRequest, paperProgressStatusEventDto);

        StepVerifier.create(mono)
                .verifyComplete();

        Mockito.verify(mockSqsSender, never()).pushSendEvent(Mockito.any());

    }


    @Test
    void handleMessage_alreadyrefined() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("req1234");
        pnDeliveryRequest.setRefined(true);

        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setStatusCode("RECAG012");
        paperProgressStatusEventDto.setStatusDateTime(OffsetDateTime.now());

        List<PnEventDemat> resdemat = new ArrayList<>();
        PnEventDemat pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("23L");
        pnEventDemat.setStatusCode("RECAG008B");
        resdemat.add(pnEventDemat);
        pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("ARCAD");
        pnEventDemat.setStatusCode("RECAG008B");
        resdemat.add(pnEventDemat);




        Mockito.when(eventDematDAO.findAllByRequestId(Mockito.anyString())).thenReturn(Flux.fromIterable(resdemat));
        Mockito.when(eventMetaDAO.getDeliveryEventMeta(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());


        Mono<Void> mono = recagSimplifiedPostLogicHandler.handleMessage(pnDeliveryRequest, paperProgressStatusEventDto);

        StepVerifier.create(mono)
                .verifyComplete();

        Mockito.verify(eventMetaDAO, never()).getDeliveryEventMeta(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockSqsSender, never()).pushSendEvent(Mockito.any());

    }


    @Test
    void handleMessage_sendevent() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("req1234");
        pnDeliveryRequest.setRefined(false);

        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setStatusCode("RECAG012");
        paperProgressStatusEventDto.setStatusDateTime(OffsetDateTime.now());
        paperProgressStatusEventDto.setRequestId("req1234");

        List<PnEventDemat> resdemat = new ArrayList<>();
        PnEventDemat pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("23L");
        pnEventDemat.setStatusCode("RECAG008B");
        resdemat.add(pnEventDemat);
        pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("ARCAD");
        pnEventDemat.setStatusCode("RECAG008B");
        resdemat.add(pnEventDemat);




        Mockito.when(eventDematDAO.findAllByRequestId(Mockito.anyString())).thenReturn(Flux.fromIterable(resdemat));
        Mockito.when(eventMetaDAO.getDeliveryEventMeta(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest));


        Mono<Void> mono = recagSimplifiedPostLogicHandler.handleMessage(pnDeliveryRequest, paperProgressStatusEventDto);

        StepVerifier.create(mono)
                .verifyComplete();

        Mockito.verify(eventMetaDAO, never()).getDeliveryEventMeta(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockSqsSender).pushSendEvent(Mockito.any());

    }

    @Test
    void handleMessage_sendevent2() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("req1234");
        pnDeliveryRequest.setRefined(false);

        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setStatusCode("RECAG008B");
        paperProgressStatusEventDto.setStatusDateTime(OffsetDateTime.now());
        paperProgressStatusEventDto.setRequestId("req1234");

        List<PnEventDemat> resdemat = new ArrayList<>();
        PnEventDemat pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("23L");
        pnEventDemat.setStatusCode("RECAG008B");
        resdemat.add(pnEventDemat);
        pnEventDemat = new PnEventDemat();
        pnEventDemat.setDocumentType("ARCAD");
        pnEventDemat.setStatusCode("RECAG008B");
        resdemat.add(pnEventDemat);

        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaStatusCode("RECAG012");
        pnEventMeta.setStatusDateTime(Instant.now());



        Mockito.when(eventDematDAO.findAllByRequestId(Mockito.anyString())).thenReturn(Flux.fromIterable(resdemat));
        Mockito.when(eventMetaDAO.getDeliveryEventMeta(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(pnEventMeta));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest));


        Mono<Void> mono = recagSimplifiedPostLogicHandler.handleMessage(pnDeliveryRequest, paperProgressStatusEventDto);

        StepVerifier.create(mono)
                .verifyComplete();

        Mockito.verify(mockSqsSender).pushSendEvent(Mockito.any());

    }
}