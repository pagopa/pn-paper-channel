package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.GenericEventHeader;

import it.pagopa.pn.paperchannel.middleware.queue.action.PrepareDeliveryMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.action.SendDeliveryMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.PrepareDeliveryEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.middleware.queue.model.SendDeliveryEvent;
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
    private PrepareDeliveryMomProducer prepareDeliveryMomProducer;
    @Autowired
    private SendDeliveryMomProducer sendDeliveryMomProducer;

    @Override
    public void pushSendEvent(SendEvent event) {
        GenericEventHeader deliveryHeader= GenericEventHeader.builder()
                .publisher("paper-channel-update")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(EventTypeEnum.SEND_PAPER_RESPONSE.name())
                .build();

        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        paperChannelUpdate.setSendEvent(event);
        SendDeliveryEvent sendDeliveryEvent = new SendDeliveryEvent(deliveryHeader, paperChannelUpdate);

        this.sendDeliveryMomProducer.push(sendDeliveryEvent);
    }

    @Override
    public void pushPrepareEvent(PrepareEvent event) {
        GenericEventHeader deliveryHeader= GenericEventHeader.builder()
                .publisher("paper-channel-update")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(EventTypeEnum.PREPARE_PAPER_RESPONSE.name())
                .build();

        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        paperChannelUpdate.setPrepareEvent(event);

        PrepareDeliveryEvent prepareDeliveryEvent = new PrepareDeliveryEvent(deliveryHeader, paperChannelUpdate);

        this.prepareDeliveryMomProducer.push(prepareDeliveryEvent);
    }
}