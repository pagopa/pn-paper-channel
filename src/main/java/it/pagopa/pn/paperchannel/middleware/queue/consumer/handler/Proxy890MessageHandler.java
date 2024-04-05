package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.OriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperProgressStatusEventOriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventError;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Instant;

@Builder
@Slf4j
public class Proxy890MessageHandler implements MessageHandler {

    private final Complex890MessageHandler complex890MessageHandler;
    private final Simple890MessageHandler simple890MessageHandler;
    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final PnEventErrorDAO pnEventErrorDAO;

    @PostConstruct
    public void postConstruct(){
        log.info("Init Proxy890MessageHandler - complexRefinementCodes: {} , enableSimple890Flow: {}",
                pnPaperChannelConfig.getComplexRefinementCodes(),pnPaperChannelConfig.isEnableSimple890Flow());
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        if(Boolean.TRUE.equals(entity.getRefined())){
            log.info("Proxying message to Simple890MessageHandler");
            return simple890MessageHandler.handleMessage(entity,paperRequest);
        }else if(!pnPaperChannelConfig.isEnableSimple890Flow() ||
                pnPaperChannelConfig.getComplexRefinementCodes().contains(paperRequest.getStatusCode())) {
            log.info("Proxying message to Complex890MessageHandler");
            return complex890MessageHandler.handleMessage(entity,paperRequest);
        }else {
            log.info("Writing in PnEventError");
            return buildAndSavePnEventError(entity,paperRequest).then();
        }
    }

    private Mono<PnEventError> buildAndSavePnEventError(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest){
        PnEventError pnEventError = new PnEventError();
        pnEventError.setRequestId(entity.getRequestId());
        pnEventError.setStatusBusinessDateTime(paperRequest.getStatusDateTime().toInstant());
        pnEventError.setStatusCode(paperRequest.getStatusCode());
        pnEventError.setIun(paperRequest.getIun());
        pnEventError.setCreatedAt(Instant.now());

        PaperProgressStatusEventOriginalMessageInfo messageInfo = new PaperProgressStatusEventOriginalMessageInfo();
        messageInfo.setEventType(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name());
        messageInfo.setStatusCode(paperRequest.getStatusCode());
        messageInfo.setStatusDescription(paperRequest.getStatusDescription());
        pnEventError.setOriginalMessageInfo(messageInfo);

        return pnEventErrorDAO.putItem(pnEventError);
    }

}
