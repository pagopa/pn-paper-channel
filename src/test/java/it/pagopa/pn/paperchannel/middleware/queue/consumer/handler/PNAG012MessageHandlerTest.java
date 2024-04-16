package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DematDocumentTypeEnum;
import it.pagopa.pn.paperchannel.utils.MetaDematUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;

import static it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.PNAG012MessageHandler.*;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PNAG012MessageHandlerTest {

    private PNAG012MessageHandler handler;

    private EventDematDAO eventDematDAO;
    private EventMetaDAO eventMetaDAO;
    private SqsSender mockSqsSender;
    private RequestDeliveryDAO requestDeliveryDAO;

    private PnPaperChannelConfig mockConfig;

    @BeforeEach
    public void init() {
        long ttlDays = 365;
        Set<String> requiredDemats = Set.of(
                DematDocumentTypeEnum.DEMAT_23L.getDocumentType(),
                DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType()
        );

        eventDematDAO = mock(EventDematDAO.class);
        eventMetaDAO = mock(EventMetaDAO.class);
        mockSqsSender = mock(SqsSender.class);
        requestDeliveryDAO = mock(RequestDeliveryDAO.class);

        mockConfig = new PnPaperChannelConfig();
        mockConfig.setTtlExecutionDaysDemat(ttlDays);
        mockConfig.setTtlExecutionDaysMeta(ttlDays);
        mockConfig.setZipHandleActive(false);
        mockConfig.setRequiredDemats(requiredDemats);

        handler = PNAG012MessageHandler.builder()
                .sqsSender(mockSqsSender)
                .eventDematDAO(eventDematDAO)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(mockConfig)
                .build();
    }

    @Test
    void testFlowContainsExactlyRequiredDematsOK() {

        // Given
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
        demat1.setStatusCode(RECAG011B_STATUS_CODE);
        demat1.setDocumentType(DematDocumentTypeEnum.DEMAT_23L.getDocumentType());

        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        demat2.setStatusCode(RECAG011B_STATUS_CODE);
        demat2.setDocumentType(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType());

        // When
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));

        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        PnEventMeta pnEventMetaRECAG012 = buildPnEventMeta(paperRequest);
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(pnEventMetaRECAG012));

        PnEventMeta pnEventMetaPNAG012 = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, 365L);
        when(eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)).thenReturn(Mono.just(pnEventMetaPNAG012));

        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));

        // Then
        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(eventMetaDAO, times(1)).getDeliveryEventMeta(anyString(), anyString());
        verify(eventMetaDAO, times(1)).putIfAbsent(any(PnEventMeta.class));
        verify(mockSqsSender, times(1)).pushSendEvent(any(SendEvent.class));

        verify(requestDeliveryDAO, times(1)).updateData(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            return true;
        }), eq(true));
    }

    @Test
    void testFlowContainsExactlyRequiredDematsWithDifferentStatusCodesTwo() {

        // Given
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
        demat1.setStatusCode(RECAG011B_STATUS_CODE);
        demat1.setDocumentType(DematDocumentTypeEnum.DEMAT_23L.getDocumentType());

        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG008B);
        demat2.setStatusCode(RECAG011B_STATUS_CODE);
        demat2.setDocumentType(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType());

        // When
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));

        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        PnEventMeta pnEventMetaRECAG012 = buildPnEventMeta(paperRequest);
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(pnEventMetaRECAG012));

        PnEventMeta pnEventMetaPNAG012 = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, 365L);
        when(eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)).thenReturn(Mono.just(pnEventMetaPNAG012));

        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));

        // Then
        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(eventMetaDAO, times(1)).getDeliveryEventMeta(anyString(), anyString());
        verify(eventMetaDAO, times(1)).putIfAbsent(any(PnEventMeta.class));
        verify(mockSqsSender, times(1)).pushSendEvent(any(SendEvent.class));

        verify(requestDeliveryDAO, times(1)).updateData(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            return true;
        }), eq(true));
    }

    @Test
    void testFlowContainsAtLeastRequiredDematsOK() {

        // Given
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
        String notManagedDocumentType = "AnotherDocumentType";

        PnEventDemat demat1 = new PnEventDemat();
        demat1.setDematRequestId(dematRequestId);
        demat1.setDocumentTypeStatusCode(DEMAT_23L_RECAG011B);
        demat1.setStatusCode(RECAG011B_STATUS_CODE);
        demat1.setDocumentType(DematDocumentTypeEnum.DEMAT_23L.getDocumentType());

        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        demat2.setStatusCode(RECAG011B_STATUS_CODE);
        demat2.setDocumentType(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType());

        /* Not known Demat 3 must not interfere with required demats check */
        PnEventDemat demat3 = new PnEventDemat();
        demat3.setDematRequestId(dematRequestId);
        demat3.setDocumentTypeStatusCode(MetaDematUtils.buildDocumentTypeStatusCode(notManagedDocumentType, RECAG011B_STATUS_CODE));
        demat3.setStatusCode(RECAG011B_STATUS_CODE);
        demat3.setDocumentType(notManagedDocumentType);

        /* Demat with null document type must be skipped from required demats check */
        PnEventDemat demat4 = new PnEventDemat();
        demat4.setDematRequestId(dematRequestId);
        demat4.setDocumentTypeStatusCode(MetaDematUtils.buildDocumentTypeStatusCode(notManagedDocumentType, RECAG011B_STATUS_CODE));
        demat4.setStatusCode(RECAG011B_STATUS_CODE);
        demat4.setDocumentType(null);

        // When
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2, demat3, demat4));

        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        PnEventMeta pnEventMetaRECAG012 = buildPnEventMeta(paperRequest);
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(pnEventMetaRECAG012));

        PnEventMeta pnEventMetaPNAG012 = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, 365L);
        when(eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)).thenReturn(Mono.just(pnEventMetaPNAG012));

        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));

        // Then
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(eventMetaDAO, times(1)).getDeliveryEventMeta(anyString(), anyString());
        verify(eventMetaDAO, times(1)).putIfAbsent(any(PnEventMeta.class));
        verify(mockSqsSender, times(1)).pushSendEvent(any(SendEvent.class));

        verify(requestDeliveryDAO, times(1)).updateData(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            return true;
        }), eq(true));
    }

    @Test
    void blockedFlowBecauseRECAG012DoesNotExistTest() {

        // Given
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
        demat1.setStatusCode(RECAG011B_STATUS_CODE);
        demat1.setDocumentType(DematDocumentTypeEnum.DEMAT_23L.getDocumentType());

        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        demat2.setStatusCode(RECAG011B_STATUS_CODE);
        demat2.setDocumentType(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType());

        // When
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));

        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.empty());

        // Then
        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che il flusso venga bloccato e quindi on invii l'evento a delivery-push
        verify(mockSqsSender, never()).pushSendEvent(any());

        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }

    @Test
    void blockedFlowBecausePNAG012AlreadyInsertedTest() {

        // Given
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
        demat1.setStatusCode(RECAG011B_STATUS_CODE);
        demat1.setDocumentType(DematDocumentTypeEnum.DEMAT_23L.getDocumentType());

        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        demat2.setStatusCode(RECAG011B_STATUS_CODE);
        demat2.setDocumentType(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType());

        // When
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));


        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        PnEventMeta pnEventMetaRECAG012 = buildPnEventMeta(paperRequest);
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(pnEventMetaRECAG012));

        PnEventMeta pnEventMetaPNAG012 = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, 365L);
        when(eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)).thenReturn(Mono.empty());

        // Then
        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        verify(mockSqsSender, never()).pushSendEvent(any());

        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }

    @Test
    void blockedFlowBecauseRequestMissRequiredDemats() {
        /* This method tests flow interruption because of missing 23L demat required.
         * Test must be rejected because request does not have all required demats.
         * */

        // Given
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

        PnEventDemat demat = new PnEventDemat();
        demat.setDematRequestId(dematRequestId);
        demat.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        demat.setStatusCode(RECAG011B_STATUS_CODE);
        demat.setDocumentType(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType());

        // When
        /* Missing 23L demat must cause check failure */
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER)).thenReturn(Flux.just(demat));

        // Then
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(eventMetaDAO, never()).getDeliveryEventMeta(anyString(), anyString());
        verify(eventMetaDAO, never()).putIfAbsent(any(PnEventMeta.class));
        verify(mockSqsSender, never()).pushSendEvent(any(SendEvent.class));
        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
    }

    @Test
    void testFlowWithDocumentTypeAliasOk() {
        /* This method tests the interchangeability between CAD and ARCAD using alias.
         * Test must pass because CAD demat is present and ARCAD is required.
         * */

        // Given
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
        demat1.setStatusCode(RECAG011B_STATUS_CODE);
        demat1.setDocumentType(DematDocumentTypeEnum.DEMAT_23L.getDocumentType());

        /* CAD and ARCAD are interchangeable in PNAG012 flow using alias */
        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_CAD_RECAG011B);
        demat2.setStatusCode(RECAG011B_STATUS_CODE);
        demat2.setDocumentType(DematDocumentTypeEnum.DEMAT_CAD.getDocumentType());

        // When
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));

        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        PnEventMeta pnEventMetaRECAG012 = buildPnEventMeta(paperRequest);
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(pnEventMetaRECAG012));

        PnEventMeta pnEventMetaPNAG012 = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, 365L);
        when(eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)).thenReturn(Mono.just(pnEventMetaPNAG012));

        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));

        // Then
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        verify(mockSqsSender, times(1)).pushSendEvent(any());

        verify(requestDeliveryDAO, times(1)).updateData(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            return true;
        }), eq(true));
    }

    @Test
    void testFlowWithEmptyRequiredDematsOk() {
        /* This method tests the case in which required demats set is empty.
         * Test must pass because empty set means no mandatory items.
         * */

        // Given

        mockConfig.setRequiredDemats(Collections.emptySet());
        ReflectionTestUtils.setField(handler, "pnPaperChannelConfig", mockConfig);

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
        demat1.setStatusCode(RECAG011B_STATUS_CODE);
        demat1.setDocumentType(DematDocumentTypeEnum.DEMAT_23L.getDocumentType());

        /* CAD and ARCAD are interchangeable in PNAG012 flow using alias */
        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_CAD_RECAG011B);
        demat2.setStatusCode(RECAG011B_STATUS_CODE);
        demat2.setDocumentType(DematDocumentTypeEnum.DEMAT_CAD.getDocumentType());

        // When
        /* Missing 23L demat must cause check failure */
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat1, demat2));

        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        PnEventMeta pnEventMetaRECAG012 = buildPnEventMeta(paperRequest);
        when(eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, META_SORT_KEY_FILTER))
                .thenReturn(Mono.just(pnEventMetaRECAG012));

        PnEventMeta pnEventMetaPNAG012 = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, 365L);
        when(eventMetaDAO.putIfAbsent(pnEventMetaPNAG012)).thenReturn(Mono.just(pnEventMetaPNAG012));

        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(entity));

        // Then
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        verify(mockSqsSender, times(1)).pushSendEvent(any());

        verify(requestDeliveryDAO, times(1)).updateData(argThat(pnDeliveryRequest -> {
            assertThat(pnDeliveryRequest).isNotNull();
            assertThat(pnDeliveryRequest.getRefined()).isTrue();
            return true;
        }), eq(true));
    }

    @Test
    void filterNotPassedBecause23LMissedTest() {

        // Given
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


        PnEventDemat demat2 = new PnEventDemat();
        demat2.setDematRequestId(dematRequestId);
        demat2.setDocumentTypeStatusCode(DEMAT_ARCAD_RECAG011B);
        demat2.setStatusCode(RECAG011B_STATUS_CODE);
        demat2.setDocumentType(DematDocumentTypeEnum.DEMAT_ARCAD.getDocumentType());

        // When
        when(eventDematDAO.findAllByKeys(dematRequestId, DEMAT_SORT_KEYS_FILTER))
                .thenReturn(Flux.just(demat2));


        // Then
        // eseguo l'handler
        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        //mi aspetto che non arrivi nè a fare la query dei meta nè a maggior ragione inviare l'evento a delivery-push
        verify(eventMetaDAO, never()).getDeliveryEventMeta(any(), any());
        verify(mockSqsSender, never()).pushSendEvent(any());

        verify(requestDeliveryDAO, never()).updateData(any(PnDeliveryRequest.class));
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
