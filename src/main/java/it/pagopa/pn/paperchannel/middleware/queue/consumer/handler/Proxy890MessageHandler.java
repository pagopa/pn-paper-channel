package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperProgressStatusEventOriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventError;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.FlowTypeEnum;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Instant;

@SuperBuilder
@Slf4j
public class Proxy890MessageHandler implements MessageHandler {

    /* Complex handlers */
    private final Complex890MessageHandler complex890MessageHandler;
    private final RECAG008CMessageHandler recag008CMessageHandler;

    private final Simple890MessageHandler simple890MessageHandler;
    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final PnEventErrorDAO pnEventErrorDAO;

    @PostConstruct
    public void postConstruct(){
        log.info("Init Proxy890MessageHandler - complexRefinementCodes: {}",
                pnPaperChannelConfig.getComplexRefinementCodes());
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        FlowTypeEnum flowType = entity.getRefined() == null
            ? FlowTypeEnum.COMPLEX_890
            : FlowTypeEnum.SIMPLE_890;

        if(Boolean.TRUE.equals(entity.getRefined())){
            log.info("Proxying message to Simple890MessageHandler");
            return simple890MessageHandler.handleMessage(entity,paperRequest);
        } else if(Boolean.TRUE.equals(isComplexFlow(paperRequest))) {
            return callComplexHandler(entity,paperRequest);
        } else {
            log.info("Writing in PnEventError with flow type {}", flowType);
            return buildAndSavePnEventError(paperRequest, flowType).then();
        }
    }

    protected Boolean isComplexFlow(PaperProgressStatusEventDto paperRequest) {
        return pnPaperChannelConfig.getComplexRefinementCodes()
            .contains(paperRequest.getStatusCode());
    }

    /**
     * Route paper progress status management to specific handler based on status code.
     * In case of RECAG008C old flow must be managed using {@link RECAG008CMessageHandler},
     * otherwise old flow must be managed using {@link Complex890MessageHandler}
     *
     * @param entity        delivery request
     * @param paperRequest  current progress event
     * */
    private Mono<Void> callComplexHandler(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        if (paperRequest.getStatusCode().equals(ExternalChannelCodeEnum.RECAG008C.name())) {
            log.info("Proxying message to RECAG008CMessageHandler");
            return recag008CMessageHandler.handleMessage(entity,paperRequest);
        } else {
            log.info("Proxying message to Complex890MessageHandler");
            return complex890MessageHandler.handleMessage(entity,paperRequest);
        }
    }

    private Mono<PnEventError> buildAndSavePnEventError(PaperProgressStatusEventDto paperRequest, FlowTypeEnum flowType){

        PnEventError pnEventError = new PnEventError();
        pnEventError.setRequestId(paperRequest.getRequestId());
        pnEventError.setStatusBusinessDateTime(paperRequest.getStatusDateTime().toInstant());
        pnEventError.setStatusCode(paperRequest.getStatusCode());
        pnEventError.setIun(paperRequest.getIun());
        pnEventError.setCreatedAt(Instant.now());
        pnEventError.setFlowType(flowType.name());

        pnEventError.setOriginalMessageInfo(this.buildPaperProgressStatusEventOriginalMessageInfo(paperRequest));

        return pnEventErrorDAO.putItem(pnEventError);
    }

    private PaperProgressStatusEventOriginalMessageInfo buildPaperProgressStatusEventOriginalMessageInfo(PaperProgressStatusEventDto paperRequest) {
        PaperProgressStatusEventOriginalMessageInfo messageInfo = new PaperProgressStatusEventOriginalMessageInfo();
        messageInfo.setEventType(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name());
        messageInfo.setStatusCode(paperRequest.getStatusCode());
        messageInfo.setStatusDescription(paperRequest.getStatusDescription());
        messageInfo.setRegisteredLetterCode(paperRequest.getRegisteredLetterCode());
        messageInfo.setProductType(paperRequest.getProductType());
        messageInfo.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());
        messageInfo.setClientRequestTimeStamp(paperRequest.getClientRequestTimeStamp().toInstant());

        return messageInfo;
    }

}
