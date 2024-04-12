package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.utils.DematDocumentTypeEnum;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

@Slf4j
@SuperBuilder
public class RECAGSimplifiedPostLogicHandler extends SendToDeliveryPushHandler {


    private final EventMetaDAO eventMetaDAO;
    private final EventDematDAO eventDematDAO;

    /**
     * Il metodo prevede di inviare VS deliverypush un evento di OK se:
     * - è arrivato l'evento di RECAG012
     * - sono presenti tutti gli attachment obbligatori
     * - la richiesta NON è già stata completata
     *
     * @param entity deliveryRequest dell'evento
     * @param paperRequest evento ricevuto da ext-channel
     * @return empty mono
     */
    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        return this.checkAllRequiredAttachments(paperRequest.getRequestId())
                .flatMap(x -> this.retrieveRECAG012(paperRequest.getRequestId(), paperRequest))
                .flatMap(pnEventMetaRECAG012 -> prepareDelayedRECAG012AndSendToDeliveryPush(entity, paperRequest, pnEventMetaRECAG012));
    }

    @NotNull
    private Mono<Void> prepareDelayedRECAG012AndSendToDeliveryPush(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMetaRECAG012) {
        if (Boolean.TRUE.equals(entity.getRefined()))
        {
            log.info("[{}] Request is already refined, nothing to do", entity.getRequestId());
            return Mono.empty();
        }
        else
        {
            log.info("[{}] Request isn't refined, proceeding with sending OK status to delivery-push", entity.getRequestId());
            PnLogAudit pnLogAudit = new PnLogAudit();

            // nelle nuova entità PnDeliveryRequest valorizzo solo i campi necessari per SendEvent (evento mandato a delivery-push)
            PnDeliveryRequest pnDeliveryRequestPNAG012 = preparePnDeliveryRequest(entity);

            // Costruisco un finto evento da inviare a delivery push
            PaperProgressStatusEventDto delayedRECAG012Event = prepareDelayedRECAG012PaperProgressStatusEventDto(paperRequest, pnEventMetaRECAG012);

            pnLogAudit.addsBeforeReceive(paperRequest.getIun(), String.format("prepare requestId = %s Response from external-channel", paperRequest.getRequestId()));
            return sendToDeliveryPush(pnDeliveryRequestPNAG012, delayedRECAG012Event)
                    .doOnSuccess(pnEventMetaRECAG012Updated -> logSuccessAuditLog(delayedRECAG012Event, pnDeliveryRequestPNAG012, pnLogAudit));
        }
    }

    @NotNull
    private static PaperProgressStatusEventDto prepareDelayedRECAG012PaperProgressStatusEventDto(PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMetaRECAG012) {
        PaperProgressStatusEventDto delayedRECAG012Event = new PaperProgressStatusEventDto();
        delayedRECAG012Event.setRequestId(paperRequest.getRequestId());
        delayedRECAG012Event.setRegisteredLetterCode(paperRequest.getRegisteredLetterCode());
        delayedRECAG012Event.setProductType(paperRequest.getProductType());
        delayedRECAG012Event.setIun(paperRequest.getIun());
        delayedRECAG012Event.setStatusDescription(PNAG012_STATUS_DESCRIPTION);
        delayedRECAG012Event.setStatusDateTime(pnEventMetaRECAG012.getStatusDateTime().atOffset(ZoneOffset.UTC));
        delayedRECAG012Event.setStatusCode(pnEventMetaRECAG012.getStatusCode());
        delayedRECAG012Event.setDeliveryFailureCause(pnEventMetaRECAG012.getDeliveryFailureCause());
        return delayedRECAG012Event;
    }

    @NotNull
    private static PnDeliveryRequest preparePnDeliveryRequest(PnDeliveryRequest entity) {
        PnDeliveryRequest pnDeliveryRequestPNAG012 = new PnDeliveryRequest();
        pnDeliveryRequestPNAG012.setStatusDetail(StatusCodeEnum.OK.getValue()); //evento finale OK
        pnDeliveryRequestPNAG012.setStatusCode(entity.getStatusDetail());
        pnDeliveryRequestPNAG012.setRequestId(entity.getRequestId());
        pnDeliveryRequestPNAG012.setRefined(true);
        return pnDeliveryRequestPNAG012;
    }

    /**
     * This method evaluates whether event RECAG012 already exists and returns it.
     *
     * @param requestId the requestId
     * @return PnEventMeta of event PnEventMeta if found
     * */
    private Mono<PnEventMeta> retrieveRECAG012(String requestId, PaperProgressStatusEventDto paperRequest) {
        if (paperRequest.getStatusCode().equals(ExternalChannelCodeEnum.RECAG012.name()))
        {
            // ok l'evento è proprio quello che cercavo, non serve nemmeno andare in dynamo
            PnEventMeta pnEventMeta = new PnEventMeta();
            pnEventMeta.setMetaStatusCode(buildMetaStatusCode(paperRequest.getStatusCode()));
            pnEventMeta.setStatusCode(paperRequest.getStatusCode());
            pnEventMeta.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());

            return Mono.just(pnEventMeta)
                    .doOnNext(pnEventMetaRECAG012 -> log.info("[{}] RECAG012 found event={}", requestId, pnEventMetaRECAG012));
        }
        else {
            String metadataRequestIdFilter = buildMetaRequestId(requestId);

            return eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, buildMetaStatusCode(ExternalChannelCodeEnum.RECAG012.name()), true)
                    .doOnDiscard(List.class, o -> log.info("RECAG012 filter not found"))
                    .doOnNext(pnEventMetaRECAG012 -> log.info("[{}] RECAG012 found from db event={}", requestId, pnEventMetaRECAG012));
        }
    }

    /**
     * This method evaluates checking if all required demats are included as subset of pnEventDemats.
     *
     * @param requestId the requestID
     * @return List of all required demats are included, empty otherwise
     */
    private Mono<List<PnEventDemat>> checkAllRequiredAttachments(String requestId) {
        String dematRequestId = buildDematRequestId(requestId);

        return eventDematDAO.findAllByRequestId(dematRequestId, true).collectList()
                .doOnNext(pnEventDematsFromDB -> log.debug("Result of findAllByKeys: {}", pnEventDematsFromDB))
                .filter(pnEventDematsFromDB -> {
                    Set<String> documentTypes = getDocumentTypesFromPnEventDemats(pnEventDematsFromDB);
                    return documentTypes.containsAll(pnPaperChannelConfig.getRequiredDemats());
                })
                .doOnDiscard(List.class, o -> log.info("Some required documents not found"))
                .doOnNext(pnEventDematsFromDB -> log.info("[{}] All required documents found",dematRequestId));

    }


    private Mono<Void> sendToDeliveryPush(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return super.handleMessage(entity, paperRequest);
    }



    private Set<String> getDocumentTypesFromPnEventDemats(List<PnEventDemat> pnEventDematsFromDB) {
        return pnEventDematsFromDB.stream()
                .filter(pnEventDemat -> pnEventDemat.getDocumentType() != null)
                .map(pnEventDemat -> DematDocumentTypeEnum.getAliasFromDocumentType(pnEventDemat.getDocumentType()))
                .collect(Collectors.toSet());
    }
}
