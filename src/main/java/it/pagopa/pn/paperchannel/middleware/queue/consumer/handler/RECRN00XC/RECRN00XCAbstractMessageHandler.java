package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC;

import it.pagopa.pn.paperchannel.exception.InvalidEventOrderException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.SendToDeliveryPushHandler;
import it.pagopa.pn.paperchannel.middleware.queue.model.PNRN012Wrapper;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.*;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

/**
 * Abstract handler for processing RECRN00XC messages.
 * This class provides common functionalities for checking duplicate events,
 * enriching event data, and handling message processing logic.
 */
@Slf4j
@SuperBuilder
public abstract class RECRN00XCAbstractMessageHandler extends SendToDeliveryPushHandler {
    protected final EventMetaDAO eventMetaDAO;
    protected final MetaDematCleaner metaDematCleaner;

    /**
     * Checks if the event is a duplicate and retrieves the corresponding RECRN010 and RECRN00xA events.
     *
     * @param entity       The delivery request entity.
     * @param paperRequest The paper progress status event.
     * @return A tuple containing RECRN010 and RECRN00xA event metadata (RECRN010, RECRN00xC).
     */
    protected Mono<Tuple2<PnEventMeta, PnEventMeta>> checkIfDuplicateEvent(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        // Remove suffix 'C' and replace with 'A'
        // RECRN00xC -> RECRN00xA
        final String status = paperRequest.getStatusCode()
                .substring(0,paperRequest.getStatusCode().length()-1)
                .concat("A");

        final String metaRequestId = buildMetaRequestId(paperRequest.getRequestId());

        return this.eventMetaDAO.getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(RECRN010_STATUS_CODE))
                // Checks if it is a duplicate event
                .switchIfEmpty(Mono.defer(() -> {
                    // DLQ
                    throw InvalidEventOrderException.from(entity, paperRequest,
                            "[{" + paperRequest.getRequestId() +
                                    "}] Missing EventMeta RECRN010 for {" + paperRequest + "}");
                }))
                .zipWhen(recrn010 ->
                        this.eventMetaDAO.getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(status))
                                .switchIfEmpty(Mono.defer(() -> {
                                    throw InvalidEventOrderException.from(entity, paperRequest,
                                            "[{" + paperRequest.getRequestId() +
                                                    "}] Missing EventMeta RECRN00xA for {" + paperRequest + "}");
                                }))
                );
    }

    /**
     * Sends the PNRN012 event based on the RECRN010 event.
     *
     * @param eventrecrn010 The RECRN010 event metadata.
     * @param entity        The delivery request entity.
     * @param paperRequest  The paper progress status event.
     * @return A Mono representing the asynchronous operation.
     */
    protected Mono<Void> sendPNRN012Event(PnEventMeta eventrecrn010,
                                          PnDeliveryRequest entity,
                                          PaperProgressStatusEventDto paperRequest) {
        // PNRN012.statusDateTime = RECRN010.statusDateTime + 10gg (RefinementDuration)
        var pnrn012StatusDatetime = eventrecrn010.getStatusDateTime()
                .atZone(ZoneId.of("UTC"))                       // converto da Instant a ZonedDateTime
                .toLocalDate()                                         // prendo solo la data
                .atStartOfDay(ZoneId.of("UTC"))                 // riporto a inizio giornata
                .plus(pnPaperChannelConfig.getRefinementDuration())    // + Refinement
                .toInstant() ;                                         // torno a Instant

        PNRN012Wrapper pnrn012Wrapper = PNRN012Wrapper
                .buildPNRN012Wrapper(entity, paperRequest, pnrn012StatusDatetime);
        var pnrn012PaperRequest = pnrn012Wrapper.getPaperProgressStatusEventDtoPNRN012();
        var pnrn012DeliveryRequest = pnrn012Wrapper.getPnDeliveryRequestPNRN012();

        PnLogAudit pnLogAudit = new PnLogAudit();
        pnLogAudit.addsBeforeReceive(entity.getIun(),
                String.format("prepare requestId = %s Response from external-channel",pnrn012DeliveryRequest.getRequestId()));
        logSuccessAuditLog(pnrn012PaperRequest, pnrn012DeliveryRequest, pnLogAudit);

        var enrichEvent = enrichEvent(paperRequest, eventrecrn010);

        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        return super.handleMessage(pnrn012DeliveryRequest, pnrn012PaperRequest)
                .then(super.handleMessage(entity, enrichEvent))
                .then(metaDematCleaner.clean(paperRequest.getRequestId()));
    }

    /**
     * Enriches the event with additional information from the event metadata.
     *
     * @param paperRequest The paper progress status event.
     * @param pnEventMeta  The event metadata containing additional details.
     * @return The enriched PaperProgressStatusEventDto.
     */
    protected PaperProgressStatusEventDto enrichEvent(PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMeta) {
        if (pnEventMeta.getDiscoveredAddress() != null) {
            DiscoveredAddressDto discoveredAddressDto = new BaseMapperImpl<>(PnDiscoveredAddress.class, DiscoveredAddressDto.class)
                    .toDTO(pnEventMeta.getDiscoveredAddress());
            paperRequest.setDiscoveredAddress(discoveredAddressDto);

            log.info("[{}] Discovered Address in EventMeta for {}", paperRequest.getRequestId(), pnEventMeta);
        }
        paperRequest.setDeliveryFailureCause(pnEventMeta.getDeliveryFailureCause());

        return paperRequest;
    }

    protected Duration getDurationBetweenDates(Instant instant1, Instant instant2){
        return pnPaperChannelConfig.isEnableTruncatedDateForRefinementCheck() ?
                Duration.between(truncateToStartOfDay(instant1), truncateToStartOfDay(instant2)) :
                Duration.between(instant1, instant2);
    }

    protected boolean isDifferenceGreaterOrEqualToRefinementDuration (
            Instant recrn00xATimestamp,
            Instant recrn010Timestamp) {
        return getDurationBetweenDates(recrn010Timestamp, recrn00xATimestamp)
                .compareTo(pnPaperChannelConfig.getRefinementDuration()) >= 0;
    }

    protected boolean isDifferenceGreaterOrEqualToStockDuration (
            Instant recrn005ATimestamp,
            Instant recrn010Timestamp) {
        return getDurationBetweenDates(recrn010Timestamp, recrn005ATimestamp)
                .compareTo(pnPaperChannelConfig.getCompiutaGiacenzaArDuration()) >= 0;
    }

    private LocalDateTime truncateToStartOfDay(Instant instant) {
        return LocalDate.ofInstant(instant, ZoneId.of("UTC")).atStartOfDay();
    }
}
