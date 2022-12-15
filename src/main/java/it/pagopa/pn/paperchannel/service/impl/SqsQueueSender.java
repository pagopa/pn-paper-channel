package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.queue.action.DeliveryMomProducer;
import it.pagopa.pn.paperchannel.queue.model.DeliveryEvent;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import it.pagopa.pn.paperchannel.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class SqsQueueSender implements SqsSender {

    @Autowired
    private DeliveryMomProducer deliveryMomProducer;

    SubscriberPrepare subscriberPrepare;

    @Override
    public void pushEvent(EventTypeEnum eventType, DeliveryAsyncModel entity){
        GenericEventHeader deliveryHeader= GenericEventHeader.builder()
                .publisher("paper-channel-update")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(eventType.name())
                .build();
//entity.getPayload().getDeliveryAddress()
        DeliveryPayload deliveryPayload= new DeliveryPayload(entity.getAddress(),entity.getAmount());

        DeliveryEvent deliveryEvent=new DeliveryEvent(deliveryHeader,deliveryPayload);

        this.deliveryMomProducer.push(deliveryEvent);
    }
}