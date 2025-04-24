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
        var recrn010Datetime = eventrecrn010.getStatusDateTime();
        var pnrn012StatusDatetime =  addDurationToInstant(recrn010Datetime, pnPaperChannelConfig.getRefinementDuration());

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

    /**
     * Calculates the temporal distance between two {@link Instant}s according to the
     * “truncate‑to‑date” settings.
     *
     * <p><strong>Behaviour</strong></p>
     * <ul>
     *   <li><b>Date‑based mode</b> –&nbsp;If
     *       {@link it.pagopa.pn.paperchannel.config.PnPaperChannelConfig#isEnableTruncatedDateForRefinementCheck()}
     *       returns {@code true}, each instant is first converted to the civil date in the {@code Europe/Rome}
     *       time‑zone (see {@link #toRomeDate(Instant)}).
     *       The method then returns a {@link Duration} equal to the number of <em>calendar
     *       days</em> between those two dates:
     *       {@code Duration.ofDays(…)}. Sub‑day information and daylight‑saving gaps/overlaps are
     *       deliberately ignored.</li>
     *   <li><b>Time‑based mode</b> –&nbsp;If the flag is {@code false}, the method falls back to
     *       {@link Duration#between( java.time.temporal.Temporal, java.time.temporal.Temporal)}
     *       and preserves the full time‑of‑day component (hours, minutes, seconds, nanoseconds).</li>
     * </ul>
     *
     * @param instant1 the starting instant (inclusive), must not be {@code null}
     * @param instant2 the ending instant   (exclusive), must not be {@code null}
     * @return a {@link Duration} representing either
     *         <ul>
     *           <li>the exact time‑based interval between the two instants, or</li>
     *           <li>a whole‑days interval measured on the local calendar in Europe/Rome,</li>
     *         </ul>
     *         depending on the configuration flag
     *
     * @see #toRomeDate(Instant)
     * @see Duration#between(java.time.temporal.Temporal, java.time.temporal.Temporal)
     * @see it.pagopa.pn.paperchannel.config.PnPaperChannelConfig#isEnableTruncatedDateForRefinementCheck()
     */
    protected Duration getDurationBetweenDates(Instant instant1, Instant instant2) {
        return pnPaperChannelConfig.isEnableTruncatedDateForRefinementCheck()
                ? Duration.ofDays(
                        Period.between(toRomeDate(instant1), toRomeDate(instant2)).getDays())
                : Duration.between(instant1, instant2);
    }

    /**
     * Adds a {@link Duration} to an {@link Instant}, respecting the
     * “truncate‑to‑date” setting.
     *
     * <p>
     * If {@link it.pagopa.pn.paperchannel.config.PnPaperChannelConfig#isEnableTruncatedDateForRefinementCheck()}
     * is {@code true}, the instant is first converted to its calendar date in {@code Europe/Rome};
     * only the whole‑days part of the duration ({@link Duration#toDays()}) is applied,
     * and the result is returned as the start‑of‑day {@link Instant}.
     * Otherwise, the full duration is added in the usual way ({@link Instant#plus(java.time.temporal.TemporalAmount)}).
     * </p>
     *
     * @param instant  the base moment
     * @param duration the amount to add
     * @return the adjusted {@link Instant}
     */
    protected Instant addDurationToInstant(Instant instant, Duration duration) {
        return pnPaperChannelConfig.isEnableTruncatedDateForRefinementCheck()
                ? romeDateToInstant(
                        toRomeDate(instant).plusDays(duration.toDays()))
                : instant.plus(duration);
    }

    /**
     * Checks whether the time difference between RECRN010 and RECRN00xA is
     * greater than or equal to {@link it.pagopa.pn.paperchannel.config.PnPaperChannelConfig#getRefinementDuration()}.
     *
     * @param recrn010Timestamp  The {@link Instant} of RECRN010.
     * @param recrn00xATimestamp The {@link Instant} of RECRN00xA (e.g., RECRN003A).
     * @return {@code true} if the difference is &ge; the configured refinement duration; otherwise, {@code false}.
     */
    protected boolean isDifferenceGreaterOrEqualToRefinementDuration (
            Instant recrn010Timestamp,
            Instant recrn00xATimestamp) {
        log.debug("recrn010Timestamp={}, recrn00xATimestamp={}, refinementDuration={}",
                recrn010Timestamp, recrn00xATimestamp, pnPaperChannelConfig.getRefinementDuration());
        return getDurationBetweenDates(recrn010Timestamp, recrn00xATimestamp)
                .compareTo(pnPaperChannelConfig.getRefinementDuration()) >= 0;
    }

    /**
     * Checks whether the time difference between RECRN010 and RECRN005A is
     * greater than or equal to {@link it.pagopa.pn.paperchannel.config.PnPaperChannelConfig#getCompiutaGiacenzaArDuration()}.
     *
     * @param recrn010Timestamp  The {@link Instant} of RECRN010.
     * @param recrn005ATimestamp The {@link Instant} of RECRN005A.
     * @return {@code true} if the difference is &ge; the compiuta giacenza AR duration; otherwise, {@code false}.
     */
    protected boolean isDifferenceGreaterOrEqualToStockDuration (
            Instant recrn010Timestamp,
            Instant recrn005ATimestamp) {
        log.debug("recrn010Timestamp={}, recrn005ATimestamp={}, refinementDuration={}",
                recrn010Timestamp, recrn005ATimestamp, pnPaperChannelConfig.getCompiutaGiacenzaArDuration());
        return getDurationBetweenDates(recrn010Timestamp, recrn005ATimestamp)
                .compareTo(pnPaperChannelConfig.getCompiutaGiacenzaArDuration()) >= 0;
    }

    /**
     * Converts the supplied {@link Instant} to a calendar date in the
     * {@code Europe/Rome} time zone.
     *
     * @param instant the moment in time to convert; must not be {@code null}
     * @return the corresponding {@link LocalDate} in Europe/Rome
     */
    private LocalDate toRomeDate(Instant instant) {
        return instant.atZone(ZoneId.of("Europe/Rome"))
                .toLocalDate();
    }

    /**
     * Converts the supplied {@link LocalDate} to an {@link Instant} that marks
     * the start of that day (00:00) in the {@code Europe/Rome} time zone.
     *
     * @param date the calendar day to convert; must not be {@code null}
     * @return the {@link Instant} at the start of the given day in Europe/Rome
     */
    private Instant romeDateToInstant(LocalDate date) {
        ZoneId rome = ZoneId.of("Europe/Rome");
        return date.atStartOfDay(rome)
                .toInstant();
    }
}
