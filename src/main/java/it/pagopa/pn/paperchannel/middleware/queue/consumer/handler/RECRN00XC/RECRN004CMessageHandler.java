package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.model.FlowTypeEnum;
import it.pagopa.pn.paperchannel.model.RequestErrorCategoryEnum;
import it.pagopa.pn.paperchannel.model.RequestErrorCauseEnum;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * RECRN004C message handler.
 * <p>
 * Gestisce l'evento di tipo "Mancata consegna presso punto di giacenza" (RECRN004C).
 * <p>
 * - Se il tempo che intercorre tra RECRN010 (inesito) e RECRN004A è inferiore alla durata configurata
 *   (`RefinementDuration`), non viene generato un perfezionamento nel futuro: l'evento viene scartato e salvato
 *   come errore nella tabella `pn-PaperEventError` (non è consentito generare elementi di feedback con data futura).
 * - Se la differenza di tempo (considerando solo la data, senza orario) è maggiore o uguale a `RefinementDuration`,
 *   genera un evento PNRN012 con data calcolata come: data di RECRN010 (troncata) + `RefinementDuration`.
 */
@Slf4j
@SuperBuilder
public class RECRN004CMessageHandler extends RECRN00XCAbstractMessageHandler {
    protected final PaperRequestErrorDAO paperRequestErrorDAO;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.info("{} handling statusCode={}", RECRN004CMessageHandler.class.getSimpleName(), paperRequest.getStatusCode());

        return super.checkIfDuplicateEvent(entity, paperRequest)
                .flatMap(recrn010AndRecrn004a -> {
                    PnEventMeta eventrecrn010 = recrn010AndRecrn004a.getT1();   // Inesito
                    PnEventMeta eventrecrn004a = recrn010AndRecrn004a.getT2();  // Mancata consegna presso Punti di Giacenza

                    // Se il tempo che intercorre tra RECRN010 e RECRN004A è >= 10gg (troncando le ore)
                    // Allora genera PNRN012 con data RECRN0010.date + 10gg (troncando le ore)
                    if(super.isDifferenceGreaterOrEqualToRefinementDuration
                            (eventrecrn010.getStatusDateTime(), eventrecrn004a.getStatusDateTime())) {
                        return super.sendPNRN012Event(eventrecrn010, entity, paperRequest);
                    }

                    // Vecchio flusso
                    if(pnPaperChannelConfig.isEnableOldFlowRECRN004C()) {
                        // Invia RECRN004C a pn-delivery-push
                        return Mono.just(enrichEvent(paperRequest, eventrecrn004a))
                                .flatMap(enrichedRequest ->
                                        super.handleMessage(entity, enrichedRequest))
                                .then(super.metaDematCleaner.clean(paperRequest.getRequestId()));
                    }

                    // Altrimenti memorizza l'evento su pn-PaperEventError
                    return buildAndSavePnEventError(entity, paperRequest, eventrecrn010, eventrecrn004a).then();
                });
    }

    private Mono<PnRequestError> buildAndSavePnEventError(
            PnDeliveryRequest entity,
            PaperProgressStatusEventDto paperRequest,
            PnEventMeta recrn010,
            PnEventMeta recrn004a){
        PnRequestError requestError = PnRequestError.builder()
                .requestId(paperRequest.getRequestId())
                .paId(entity.getRequestPaId())
                .error(String.format("RECRN004A statusDateTime: %s, RECRN010 statusDateTime: %s",
                        recrn004a.getStatusDateTime(), recrn010.getStatusDateTime()))
                .flowThrow(FlowTypeEnum.RECRN004C.name())
                .category(RequestErrorCategoryEnum.RENDICONTAZIONE_SCARTATA)
                .cause(RequestErrorCauseEnum.REFINEMENT_DATE_ERROR)
                .build();
        return paperRequestErrorDAO.created(requestError);
    }
}
