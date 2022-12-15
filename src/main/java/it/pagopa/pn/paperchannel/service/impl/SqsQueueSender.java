package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.GenericEventHeader;

import it.pagopa.pn.paperchannel.middleware.queue.action.DeliveryMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPayload;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
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
    private DeliveryMomProducer deliveryMomProducer;

    @Override
    public void pushEvent(EventTypeEnum eventType, DeliveryPayload deliveryPayload){
        GenericEventHeader deliveryHeader= GenericEventHeader.builder()
                .publisher("paper-channel-update")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(eventType.name())
                .build();

        DeliveryEvent deliveryEvent=new DeliveryEvent(deliveryHeader, deliveryPayload);

        this.deliveryMomProducer.push(deliveryEvent);
    }
}