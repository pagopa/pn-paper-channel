package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.GenericEventHeader;

import it.pagopa.pn.paperchannel.middleware.queue.producer.DeliveryPushMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class SqsQueueSender implements SqsSender {

    @Autowired
    private DeliveryPushMomProducer deliveryPushMomProducer;

    @Override
    public void pushSendEvent(SendEvent event) {
        push(event, null);
    }

    @Override
    public void pushPrepareEvent(PrepareEvent event) {
        push(null, event);
    }

    private void push(SendEvent sendEvent, PrepareEvent prepareEvent){
        GenericEventHeader deliveryHeader= GenericEventHeader.builder()
                .publisher("paper-channel-update")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType((sendEvent == null) ? EventTypeEnum.PREPARE_PAPER_RESPONSE.name(): EventTypeEnum.SEND_PAPER_RESPONSE.name())
                .build();

        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        paperChannelUpdate.setPrepareEvent(prepareEvent);
        paperChannelUpdate.setSendEvent(sendEvent);

        DeliveryPushEvent deliveryPushEvent = new DeliveryPushEvent(deliveryHeader, paperChannelUpdate);

        this.deliveryPushMomProducer.push(deliveryPushEvent);
    }
}