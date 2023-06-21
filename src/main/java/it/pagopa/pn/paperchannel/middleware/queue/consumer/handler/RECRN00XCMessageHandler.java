package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.middleware.queue.model.PNRN012Wrapper;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

@Slf4j
public class RECRN00XCMessageHandler extends SendToDeliveryPushHandler {
    private final EventMetaDAO eventMetaDAO;
    private final MetaDematCleaner metaDematCleaner;
    private final Duration refinementDuration;

    public RECRN00XCMessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, MetaDematCleaner metaDematCleaner, Duration refinementDuration) {
        super(sqsSender);
        this.eventMetaDAO = eventMetaDAO;
        this.metaDematCleaner = metaDematCleaner;
        this.refinementDuration = refinementDuration;

        log.info("Refinement duration is {}", this.refinementDuration);
    }


    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        final String status = paperRequest.getStatusCode()
                .substring(0,paperRequest.getStatusCode().length()-1)
                .concat("A");

        final String metaRequestId = buildMetaRequestId(paperRequest.getRequestId());

        return this.eventMetaDAO.getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(RECRN011_STATUS_CODE))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[{}] Missing EventMeta RECRN011 for {}", paperRequest.getRequestId(), paperRequest);
                    return Mono.empty();
                }))
                .zipWhen(n011 ->
                        this.eventMetaDAO.getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(status))
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.warn("[{}] Missing EventMeta RECRN011 for {}", paperRequest.getRequestId(), paperRequest);
                                    return Mono.empty();
                                }))
                )
                .doOnNext(recrn011AndRecrn00X -> {
                    PnEventMeta eventrecrn011 = recrn011AndRecrn00X.getT1();
                    PnEventMeta eventrecrn00X = recrn011AndRecrn00X.getT2();

                    if (isThenGratherOrEquals10Days(eventrecrn00X.getStatusDateTime(), eventrecrn011.getStatusDateTime())) {
                        PNRN012Wrapper pnrn012Wrapper = PNRN012Wrapper.buildPNRN012Wrapper(entity, paperRequest, eventrecrn011.getStatusDateTime().plus(refinementDuration));
                        var pnrn012PaperRequest = pnrn012Wrapper.getPaperProgressStatusEventDtoPNRN012();
                        var pnrn012DeliveryRequest = pnrn012Wrapper.getPnDeliveryRequestPNRN012();

                        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
                        PnLogAudit pnLogAudit = new PnLogAudit(auditLogBuilder);
                        pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel",pnrn012DeliveryRequest.getRequestId()));
                        logSuccessAuditLog(pnrn012PaperRequest, pnrn012DeliveryRequest, pnLogAudit);

                        super.handleMessage(pnrn012DeliveryRequest, pnrn012PaperRequest);
                    }
                })
                .flatMap(recrn011AndRecrn00X -> {
                    PnEventMeta eventrecrn011 = recrn011AndRecrn00X.getT1();
                    return Mono.just(enrichEvent(paperRequest, eventrecrn011))
                            .flatMap(enrichedRequest -> {
                                entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
                                return super.handleMessage(entity, enrichedRequest);
                            })
                            .then(metaDematCleaner.clean(paperRequest.getRequestId()));
                });
    }

    private PaperProgressStatusEventDto enrichEvent(PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMeta) {

        if (pnEventMeta.getDiscoveredAddress() != null) {
            DiscoveredAddressDto discoveredAddressDto = new BaseMapperImpl<>(PnDiscoveredAddress.class, DiscoveredAddressDto.class)
                    .toDTO(pnEventMeta.getDiscoveredAddress());
            paperRequest.setDiscoveredAddress(discoveredAddressDto);

            log.info("[{}] Discovered Address in EventMeta for {}", paperRequest.getRequestId(), pnEventMeta);
        }
        paperRequest.setDeliveryFailureCause(pnEventMeta.getDeliveryFailureCause());

        return paperRequest;
    }



    private boolean isThenGratherOrEquals10Days(Instant recrn00XTimestamp, Instant recrn011Timestamp){
        // sebbene 10gg sia il termine di esercizio, per collaudo fa comodo avere un tempo più contenuto
        return Duration.between(recrn011Timestamp, recrn00XTimestamp).compareTo(refinementDuration) >= 0;
    }

}
