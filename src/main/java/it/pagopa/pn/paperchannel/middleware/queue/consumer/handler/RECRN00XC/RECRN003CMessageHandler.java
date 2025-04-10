package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * RECRN003C message handler.
 * <p>
 * Gestisce l'evento di tipo "Consegna presso punto di giacenza" (RECRN003C).
 * <p>
 * - Se il tempo che intercorre tra RECRN010 (inesito) e RECRN003A è inferiore alla durata configurata
 *   (`RefinementDuration`), genera un evento di feedback RECRN003C.
 * - Se la differenza di tempo (considerando solo la data, senza orario) è maggiore o uguale a `RefinementDuration`,
 *   genera un evento PNRN012 con data calcolata come: data di RECRN010 (troncata) + `RefinementDuration`.
 */
@Slf4j
@SuperBuilder
public class RECRN003CMessageHandler extends RECRN00XCAbstractMessageHandler {

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.info("{} handling statusCode={}", RECRN003CMessageHandler.class.getSimpleName(), paperRequest.getStatusCode());

        return super.checkIfDuplicateEvent(entity, paperRequest)
                .flatMap(recrn011AndRecrn00Xa -> {
                    PnEventMeta eventrecrn010 = recrn011AndRecrn00Xa.getT1();   // Inesito
                    PnEventMeta eventrecrn003a = recrn011AndRecrn00Xa.getT2();  // Consegnato presso Punti di Giacenza

                    // Se il tempo che intercorre tra RECRN0010 e RECRN003A è >= 10gg (troncando le ore)
                    // Allora genera PNRN012 con data RECRN0010.date + 10gg (troncando le ore)
                    if (super.isDifferenceGreaterOrEqualToRefinementDuration(
                            eventrecrn003a.getStatusDateTime(), eventrecrn010.getStatusDateTime())) {
                        return super.sendPNRN012Event(eventrecrn010, entity, paperRequest);
                    }

                    // Altrimenti invia eventrecrn003a a pn-delivery-push
                    return Mono.just(enrichEvent(paperRequest, eventrecrn003a))
                            .flatMap(enrichedRequest ->
                                    super.handleMessage(entity, enrichedRequest))
                            .then(super.metaDematCleaner.clean(paperRequest.getRequestId()));
                });
    }
}
