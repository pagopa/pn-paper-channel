package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventError;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@SuperBuilder
public class RECRN005CMessageHandler extends RECRN00XCAbstractMessageHandler {

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return super.checkIfDuplicateEvent(entity, paperRequest)
                .flatMap(recrn011AndRecrn005 -> {
                    PnEventMeta eventrecrn011 = recrn011AndRecrn005.getT1(); // Inizio giacenza
                    PnEventMeta eventrecrn005C = recrn011AndRecrn005.getT2(); // Compiuta giacenza

                    // Se il tempo che intercorre tra RECRN0011 e RECRN005C è >= 30gg
                    // Allora genera PNRN012 con data RECRN0011.date + 10gg
                    if (isThenGreaterOrEquals30Days(eventrecrn005C.getStatusDateTime(), eventrecrn011.getStatusDateTime())) {
                        return sendPNRN012Event(eventrecrn011, entity, paperRequest);
                    }

                    // Altrimenti memorizza l'evento su PnPaperError
                    return buildAndSavePnEventError(paperRequest).then();
                });
    }

    private boolean isThenGreaterOrEquals30Days(Instant recrn005CTimestamp, Instant recrn011Timestamp){
        return Duration.between(recrn011Timestamp, recrn005CTimestamp)
                .compareTo(Duration.ofDays(30)) >= 0;
    }

    private Mono<PnEventError> buildAndSavePnEventError(PaperProgressStatusEventDto paperRequest){

        PnEventError pnEventError = new PnEventError();
        pnEventError.setRequestId(paperRequest.getRequestId());
        pnEventError.setStatusBusinessDateTime(paperRequest.getStatusDateTime().toInstant());
        pnEventError.setStatusCode(paperRequest.getStatusCode());
        pnEventError.setIun(paperRequest.getIun());
        pnEventError.setCreatedAt(Instant.now());
        //pnEventError.setFlowType(FlowTypeEnum.);
        // TODO
        //pnEventError.setOriginalMessageInfo(this.buildPaperProgressStatusEventOriginalMessageInfo(paperRequest));

        return pnEventErrorDAO.putItem(pnEventError);
    }
}
