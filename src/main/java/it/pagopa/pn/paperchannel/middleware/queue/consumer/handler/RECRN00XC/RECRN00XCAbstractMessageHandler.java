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

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

@Slf4j
@SuperBuilder
public abstract class RECRN00XCAbstractMessageHandler extends SendToDeliveryPushHandler {
    protected final EventMetaDAO eventMetaDAO;
    protected final MetaDematCleaner metaDematCleaner;

    // TODO
/*    @PostConstruct
    private void postConstruct() {
        log.info("Refinement duration is {}", this.pnPaperChannelConfig.getRefinementDuration());
    }*/

    /**
     *
     * @param entity PnDeliveryRequest
     * @param paperRequest PaperProgressStatusEventDto
     * @return (RECRN011, RECRN00xC)
     */
    protected Mono<Tuple2<PnEventMeta, PnEventMeta>> checkIfDuplicateEvent(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        final String status = paperRequest.getStatusCode()
                .substring(0,paperRequest.getStatusCode().length()-1)
                .concat("A");

        final String metaRequestId = buildMetaRequestId(paperRequest.getRequestId());

        return this.eventMetaDAO.getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(RECRN011_STATUS_CODE))
                // Checks if it is a duplicate event
                .switchIfEmpty(Mono.defer(() -> {
                    throw InvalidEventOrderException.from(entity, paperRequest,
                            "[{" + paperRequest.getRequestId() +
                                    "}] Missing EventMeta RECRN011 for {" + paperRequest + "}");
                }))
                .zipWhen(n011 ->
                        this.eventMetaDAO.getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(status))
                                .switchIfEmpty(Mono.defer(() -> {
                                    throw InvalidEventOrderException.from(entity, paperRequest,
                                            "[{" + paperRequest.getRequestId() +
                                                    "}] Missing EventMeta RECRN011 for {" + paperRequest + "}");
                                }))
                );
    }

    protected Mono<Void> sendPNRN012Event(PnEventMeta eventrecrn011,
                                          PnDeliveryRequest entity,
                                          PaperProgressStatusEventDto paperRequest){
        // PNRN012.statusDateTime = RECRN011.statusDateTime + 10gg (RefinementDuration)
        var pnrn012StatusDatetime = eventrecrn011.getStatusDateTime()
                .plus(pnPaperChannelConfig.getRefinementDuration());
        PNRN012Wrapper pnrn012Wrapper = PNRN012Wrapper
                .buildPNRN012Wrapper(entity, paperRequest, pnrn012StatusDatetime);
        var pnrn012PaperRequest = pnrn012Wrapper.getPaperProgressStatusEventDtoPNRN012();
        var pnrn012DeliveryRequest = pnrn012Wrapper.getPnDeliveryRequestPNRN012();

        PnLogAudit pnLogAudit = new PnLogAudit();
        pnLogAudit.addsBeforeReceive(entity.getIun(),
                String.format("prepare requestId = %s Response from external-channel",pnrn012DeliveryRequest.getRequestId()));
        logSuccessAuditLog(pnrn012PaperRequest, pnrn012DeliveryRequest, pnLogAudit);

        var enrichEvent = enrichEvent(paperRequest, eventrecrn011);

        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        return super.handleMessage(pnrn012DeliveryRequest, pnrn012PaperRequest)
                .then(super.handleMessage(entity, enrichEvent))
                .then(metaDematCleaner.clean(paperRequest.getRequestId()));
    }

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
}
