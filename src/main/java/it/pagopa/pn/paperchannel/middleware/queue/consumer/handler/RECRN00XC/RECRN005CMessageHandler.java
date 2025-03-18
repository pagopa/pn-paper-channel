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

import java.time.*;

/**
 * RECRN005C message handler.
 * This class handles the storage duration verification between RECRN011 (start of storage) and RECRN005A (storage completion) events.
 * If the duration is equal to or greater than 30 days, it generates a PNRN012 event.
 * Otherwise, it creates and saves an error record.
 */
@Slf4j
@SuperBuilder
public class RECRN005CMessageHandler extends RECRN00XCAbstractMessageHandler {
    protected final PaperRequestErrorDAO paperRequestErrorDAO;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.info("{} handling statusCode={}, compiutaGiacenzaArDuration={}",
                RECRN005CMessageHandler.class.getSimpleName(),
                paperRequest.getStatusCode(),
                pnPaperChannelConfig.getCompiutaGiacenzaArDuration());

        return super.checkIfDuplicateEvent(entity, paperRequest)
                .flatMap(recrn011AndRecrn005a -> {
                    PnEventMeta eventRecrn011 = recrn011AndRecrn005a.getT1(); // Inizio giacenza
                    PnEventMeta eventRecrn005A = recrn011AndRecrn005a.getT2(); // Pre-esito fine giacenza

                    // Se il tempo che intercorre tra RECRN0011 e RECRN005A è >= 30gg (troncando ore, minuti, secondo, etc)
                    // Allora genera PNRN012 con data RECRN0011.date + 10gg
                    if (isAValidStockInterval(eventRecrn011.getStatusDateTime(), eventRecrn005A.getStatusDateTime())) {
                        return super.sendPNRN012Event(eventRecrn011, entity, paperRequest);
                    }

                    // Altrimenti memorizza l'evento su PnPaperError
                    return buildAndSavePnEventError(entity, paperRequest, eventRecrn011, eventRecrn005A).then();
                });
    }

    private boolean isAValidStockInterval(Instant recrn011, Instant recrn005A){
        var recrn011StartOfDay = LocalDate.ofInstant(recrn011, ZoneId.of("UTC")).atStartOfDay();
        var recrn005AStartOfDay = LocalDate.ofInstant(recrn005A, ZoneId.of("UTC")).atStartOfDay();
        return Duration.between(recrn011StartOfDay, recrn005AStartOfDay)
                .compareTo(this.pnPaperChannelConfig.getCompiutaGiacenzaArDuration()) >= 0;
    }

    private Mono<PnRequestError> buildAndSavePnEventError(
            PnDeliveryRequest entity,
            PaperProgressStatusEventDto paperRequest,
            PnEventMeta recrn011,
            PnEventMeta recrn005A){
        PnRequestError requestError = PnRequestError.builder()
                .requestId(paperRequest.getRequestId())
                .paId(entity.getRequestPaId())
                .error(String.format("RECRN005A statusDateTime: %s, RECRN011 statusDateTime: %s",
                        recrn005A.getStatusDateTime(), recrn011.getStatusDateTime()))
                .flowThrow(FlowTypeEnum.RECRN005C.name()) // RECRN005
                .category(RequestErrorCategoryEnum.RENDICONTAZIONE_SCARTATA)
                .cause(RequestErrorCauseEnum.GIACENZA_DATE_ERROR)
                .build();
        return paperRequestErrorDAO.created(requestError);
    }
}
