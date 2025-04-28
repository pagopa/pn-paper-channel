package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * RECRN004C message handler.
 * <p>
 * Gestisce l'evento di tipo "Mancata consegna presso punto di giacenza" (RECRN004C).
 * <p>
 * - Se il tempo che intercorre tra RECRN010 (inesito) e RECRN004A è inferiore o uguale alla durata configurata
 *   (`RefinementDuration`), genera un evento di feedback RECRN004C.
 * - Se la differenza di tempo (considerando solo la data, senza orario) a `RefinementDuration`,
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

                    // Se il tempo che intercorre tra RECRN010 e RECRN004A è > 10gg (troncando le ore)
                    // Allora genera PNRN012 con data RECRN0010.date + 10gg (troncando le ore)
                    if (super.isDifferenceGreaterRefinementDuration
                            (eventrecrn010.getStatusDateTime(), eventrecrn004a.getStatusDateTime())) {
                        return super.sendPNRN012Event(eventrecrn010, entity, paperRequest);
                    }

                    // Invia RECRN004C a pn-delivery-push
                    return Mono.just(enrichEvent(paperRequest, eventrecrn004a))
                            .flatMap(enrichedRequest ->
                                    super.handleMessage(entity, enrichedRequest))
                            .then(super.metaDematCleaner.clean(paperRequest.getRequestId()));
                });
    }
}
