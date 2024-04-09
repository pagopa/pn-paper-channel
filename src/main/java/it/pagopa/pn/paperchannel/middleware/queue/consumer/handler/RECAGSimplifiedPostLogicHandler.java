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
    private final Set<String> requiredDemats;


    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        return handleRECAGXXXBPostLogic(entity, paperRequest);
    }

    public Mono<Void> handleRECAGXXXBPostLogic(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        String metadataRequestIdFilter = buildMetaRequestId(paperRequest.getRequestId());
        String dematRequestId = buildDematRequestId(paperRequest.getRequestId());

        return this.checkAllRequiredAttachments(dematRequestId)
                .flatMap(x -> this.retrieveRECAG012(metadataRequestIdFilter))
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
            PnDeliveryRequest pnDeliveryRequestPNAG012 = preparePnDeliveryRequest(paperRequest, pnEventMetaRECAG012);

            // Costruisco un finto evento da inviare a delivery push
            PaperProgressStatusEventDto delayedRECAG012Event = prepareDelayedRECAG012PaperProgressStatusEventDto(paperRequest, pnEventMetaRECAG012);

            pnLogAudit.addsBeforeReceive(paperRequest.getIun(), String.format("prepare requestId = %s Response from external-channel", paperRequest.getRequestId()));
            return sendToDeliveryPush(pnDeliveryRequestPNAG012, delayedRECAG012Event)
                    .doOnNext(pnEventMetaRECAG012Updated -> logSuccessAuditLog(delayedRECAG012Event, pnDeliveryRequestPNAG012, pnLogAudit));
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
        return delayedRECAG012Event;
    }

    @NotNull
    private static PnDeliveryRequest preparePnDeliveryRequest(PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMetaRECAG012) {
        PnDeliveryRequest pnDeliveryRequestPNAG012 = new PnDeliveryRequest();
        pnDeliveryRequestPNAG012.setStatusDetail(StatusCodeEnum.OK.getValue()); //evento finale OK
        pnDeliveryRequestPNAG012.setStatusCode(pnEventMetaRECAG012.getStatusCode());
        pnDeliveryRequestPNAG012.setRequestId(paperRequest.getRequestId());
        pnDeliveryRequestPNAG012.setRefined(true);
        return pnDeliveryRequestPNAG012;
    }

    /**
     * This method evaluates whether event RECAG012 already exists and returns it.
     *
     * @param metadataRequestIdFilter the metas to check
     * @return PnEventMeta of event PnEventMeta if found
     * */
    private Mono<PnEventMeta> retrieveRECAG012(String metadataRequestIdFilter) {

        return  eventMetaDAO.getDeliveryEventMeta(metadataRequestIdFilter, ExternalChannelCodeEnum.RECAG012.name())
                .doOnDiscard(List.class, o -> log.info("RECAG012 filter not found"))
                .doOnNext(pnEventMetaRECAG012 -> log.info("[{}] RECAG012 found event={}",metadataRequestIdFilter, pnEventMetaRECAG012));
    }

    /**
     * This method evaluates checking if all required demats are included as subset of pnEventDemats.
     *
     * @param dematRequestId the demats to check
     * @return List of all required demats are included, empty otherwise
     */
    private Mono<List<PnEventDemat>> checkAllRequiredAttachments(String dematRequestId) {

        return eventDematDAO.findAllByKeys(dematRequestId, String.valueOf(requiredDemats.stream().toList())).collectList()
                .doOnNext(pnEventDematsFromDB -> log.debug("Result of findAllByKeys: {}", pnEventDematsFromDB))
                .filter(pnEventDematsFromDB -> {
                    Set<String> documentTypes = getDocumentTypesFromPnEventDemats(pnEventDematsFromDB);
                    return documentTypes.containsAll(requiredDemats);
                })
                .doOnDiscard(List.class, o -> log.info("Some required documents not found"))
                .doOnNext(pnEventDematsFromDB -> log.info("[{}] All required documents found",dematRequestId));

    }


    protected Mono<Void> sendToDeliveryPush(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return super.handleMessage(entity, paperRequest);
    }



    private Set<String> getDocumentTypesFromPnEventDemats(List<PnEventDemat> pnEventDematsFromDB) {
        return pnEventDematsFromDB.stream()
                .filter(pnEventDemat -> pnEventDemat.getDocumentType() != null)
                .map(pnEventDemat -> DematDocumentTypeEnum.getAliasFromDocumentType(pnEventDemat.getDocumentType()))
                .collect(Collectors.toSet());
    }
}
