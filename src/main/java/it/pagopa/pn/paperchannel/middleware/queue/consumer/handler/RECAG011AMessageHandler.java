package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class RECAG011AMessageHandler extends SaveMetadataMessageHandler {

    private final SqsSender sqsSender;

    public RECAG011AMessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, Long ttlDaysMeta) {
        super(eventMetaDAO, ttlDaysMeta);
        this.sqsSender = sqsSender;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] RECAG011A handler start", paperRequest.getRequestId());

        return super.handleMessage(entity, paperRequest) // deal with the meta as a regular meta, saving it
                .then(sendToDeliveryPush(SendEventMapper.changeToProgressStatus(entity), paperRequest)); // send to delivery push as progress
    }

    public Mono<Void> sendToDeliveryPush(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        // code from SendToDeliveryPushHandler.handleMessage()
        log.debug("[{}] Sending to delivery-push", paperRequest.getRequestId());
        log.debug("[{}] Response of ExternalChannel from request id {}", paperRequest.getRequestId(), paperRequest);
        SendEvent sendEvent = SendEventMapper.createSendEventMessage(entity, paperRequest);
        sqsSender.pushSendEvent(sendEvent);
        log.info("[{}] Sent to delivery-push: {}", paperRequest.getRequestId(), sendEvent);
        return Mono.empty();
    }
}
