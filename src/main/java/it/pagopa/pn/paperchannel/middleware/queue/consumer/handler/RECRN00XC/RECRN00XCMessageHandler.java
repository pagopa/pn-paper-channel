package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@SuperBuilder
public class RECRN00XCMessageHandler extends RECRN00XCAbstractMessageHandler {

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return super.checkIfDuplicateEvent(entity, paperRequest)
                .flatMap(recrn011AndRecrn00X -> {
                    PnEventMeta eventrecrn011 = recrn011AndRecrn00X.getT1(); // Inizio giacenza
                    PnEventMeta eventrecrn00X = recrn011AndRecrn00X.getT2(); // Pre-esito fine giacenza
                    // eventrecrn00X ? é C?

                    // Se il tempo che intercorre tra RECRN0011 e RECRN00X è >= 10gg
                    // Allora genera PNRN012 con data RECRN0011.date + 10gg
                    if (isThenGratherOrEquals10Days(eventrecrn00X.getStatusDateTime(), eventrecrn011.getStatusDateTime())) {
                        return super.sendPNRN012Event(eventrecrn011, entity, paperRequest);
                    }

                    // Altrimenti invia eventrecrn00X a pn-delivery-push
                    return Mono.just(enrichEvent(paperRequest, eventrecrn00X))
                            .flatMap(enrichedRequest ->
                                    super.handleMessage(entity, enrichedRequest))
                            .then(super.metaDematCleaner.clean(paperRequest.getRequestId()));
                });
    }

    private boolean isThenGratherOrEquals10Days(Instant recrn00XTimestamp, Instant recrn011Timestamp){
        // sebbene 10gg sia il termine di esercizio, per collaudo fa comodo avere un tempo più contenuto
        return Duration.between(recrn011Timestamp, recrn00XTimestamp)
                .compareTo(pnPaperChannelConfig.getRefinementDuration()) >= 0;
    }
}
