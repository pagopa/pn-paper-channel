package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.paperchannel.queue.action.DeliveryMomProducer;
import it.pagopa.pn.paperchannel.queue.model.DeliveryEvent;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class SqsQueueSender {

    @Autowired
    private DeliveryMomProducer deliveryMomProducer;

    public void pushEvent(){
        StandardEventHeader deliveryHeader= StandardEventHeader.builder()
                .publisher("paperChannel")
                .iun("abcd")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType( "readyDelivery")
                .build();

        DeliveryPayload deliveryPayload= new DeliveryPayload("delivery Event body");

        DeliveryEvent deliveryEvent=new DeliveryEvent(deliveryHeader,deliveryPayload);

        this.deliveryMomProducer.push(deliveryEvent);
    }
}