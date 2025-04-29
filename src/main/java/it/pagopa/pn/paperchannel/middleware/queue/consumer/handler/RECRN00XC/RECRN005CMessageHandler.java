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
 * RECRN005C message handler.
 * <p>
 * Gestisce l'evento di tipo "Compiuta giacenza" (RECRN005C).
 * <p>
 * - Se il tempo che intercorre tra RECRN010 (inesito) e RECRN005A è maggiore o uguale a 30 giorni
 *   (durata configurata tramite `CompiutaGiacenzaArDuration`), considerando solo la data senza l'orario,
 *   viene generato un evento PNRN012 con data: data di RECRN010 (troncata) + `RefinementDuration`.
 * - In caso contrario, l'evento viene scartato e memorizzato nella tabella `pn-PaperEventError`
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
                .flatMap(recrn010AndRecrn005a -> {
                    PnEventMeta eventRecrn010 = recrn010AndRecrn005a.getT1(); // Inesito
                    PnEventMeta eventRecrn005a = recrn010AndRecrn005a.getT2(); // Pre-esito fine giacenza

                    // Se il tempo che intercorre tra RECRN0010 e RECRN005A è >= 30gg (troncando le ore)
                    // Allora genera PNRN012 con data RECRN0010.date + 10gg (troncando le ore)
                    if (super.isDifferenceGreaterOrEqualToStockDuration
                            (eventRecrn010.getStatusDateTime(), eventRecrn005a.getStatusDateTime())) {
                        return super.sendPNRN012Event(eventRecrn010, entity, paperRequest);
                    }

                    // Altrimenti memorizza l'evento su pn-PaperEventError
                    return buildAndSavePnEventError(entity, paperRequest, eventRecrn010, eventRecrn005a).then();
                });
    }

    private Mono<PnRequestError> buildAndSavePnEventError(
            PnDeliveryRequest entity,
            PaperProgressStatusEventDto paperRequest,
            PnEventMeta recrn010,
            PnEventMeta recrn005a){
        PnRequestError requestError = PnRequestError.builder()
                .requestId(paperRequest.getRequestId())
                .paId(entity.getRequestPaId())
                .error(String.format("RECRN005A statusDateTime: %s, RECRN010 statusDateTime: %s",
                        recrn005a.getStatusDateTime(), recrn010.getStatusDateTime()))
                .flowThrow(FlowTypeEnum.RECRN005C.name()) // RECRN005
                .category(RequestErrorCategoryEnum.RENDICONTAZIONE_SCARTATA)
                .cause(RequestErrorCauseEnum.GIACENZA_DATE_ERROR)
                .build();
        return paperRequestErrorDAO.created(requestError);
    }
}
