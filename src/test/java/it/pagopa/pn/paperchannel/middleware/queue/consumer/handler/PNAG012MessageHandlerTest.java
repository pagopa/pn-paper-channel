package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashSet;

import static it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.PNAG012MessageHandler.*;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PNAG012MessageHandlerTest {

    private EventDematDAO eventDematDAO;

    private EventMetaDAO eventMetaDAO;

    private SqsSender mockSqsSender;

    private PNAG012MessageHandler handler;

    @BeforeEach
    public void init() {
        long ttlDays = 365;
        eventDematDAO = mock(EventDematDAO.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        mockSqsSender = mock(SqsSender.class);

        // TODO: added new HashSet, implement test cases
        handler = new PNAG012MessageHandler(mockSqsSender, eventDematDAO, ttlDays, eventMetaDAO, ttlDays, new HashSet<>());
    }

    @Test
    void okTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG012")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        String dematRequestId = buildDematRequestId(paperRequest.getRequestId());
        PnEventDemat demat1 = new PnEventDemat();
        demat1.setDematRequestId(dematRequestId);
        demat1.setDocumentTypeStatusCode(DEMAT_23L_RECAG011B);

        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));


        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        PnEventMeta pnEventMetaRECAG012 = buildPnEventMeta(paperRequest);
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(pnEventMetaRECAG012));

        PnEventMeta pnEventMetaPNAG012 = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, 365L);
        when(eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)).thenReturn(Mono.just(pnEventMetaPNAG012));

        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        verify(mockSqsSender, times(1)).pushSendEvent(any());

    }

    @Test
    void blockedFlowBecauseRECAG012DoesNotExistTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG012")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        String dematRequestId = buildDematRequestId(paperRequest.getRequestId());
        PnEventDemat demat1 = new PnEventDemat();
        demat1.setDematRequestId(dematRequestId);
        demat1.setDocumentTypeStatusCode(DEMAT_23L_RECAG011B);

        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));


        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.empty());

        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che il flusso venga bloccato e quindi on invii l'evento a delivery-push
        verify(mockSqsSender, never()).pushSendEvent(any());

    }

    @Test
    void blockedFlowBecausePNAG012AlreadyInsertedTest() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG012")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant)
                .deliveryFailureCause("M02");

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());

        String dematRequestId = buildDematRequestId(paperRequest.getRequestId());
        PnEventDemat demat1 = new PnEventDemat();
        demat1.setDematRequestId(dematRequestId);
        demat1.setDocumentTypeStatusCode(DEMAT_23L_RECAG011B);

        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));


        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        PnEventMeta pnEventMetaRECAG012 = buildPnEventMeta(paperRequest);
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(pnEventMetaRECAG012));

        PnEventMeta pnEventMetaPNAG012 = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, 365L);
        when(eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)).thenReturn(Mono.empty());

        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        verify(mockSqsSender, never()).pushSendEvent(any());

    }

    private PnEventMeta buildPnEventMeta(PaperProgressStatusEventDto paperRequest) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(buildMetaRequestId(paperRequest.getRequestId()));
        pnEventMeta.setMetaStatusCode(buildMetaStatusCode(paperRequest.getStatusCode()));
        pnEventMeta.setTtl(paperRequest.getStatusDateTime().plusDays(365).toEpochSecond());

        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setStatusCode(paperRequest.getStatusCode());
        pnEventMeta.setDeliveryFailureCause(paperRequest.getDeliveryFailureCause());

        if (paperRequest.getDiscoveredAddress() != null)
        {
            PnDiscoveredAddress discoveredAddress = new BaseMapperImpl<>(DiscoveredAddressDto.class, PnDiscoveredAddress.class).toDTO(paperRequest.getDiscoveredAddress());
            pnEventMeta.setDiscoveredAddress(discoveredAddress);
        }

        pnEventMeta.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());
        return pnEventMeta;
    }


}
