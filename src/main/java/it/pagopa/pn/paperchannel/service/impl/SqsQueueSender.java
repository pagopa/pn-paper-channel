package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SqsQueueSender implements SqsSender {

//    @Autowired
//    private DeliveryMomProducer deliveryMomProducer;

    @Override
    public void pushEvent(EventTypeEnum eventType){
//        GenericEventHeader deliveryHeader= GenericEventHeader.builder()
//                .publisher("paper-channel-update")
//                .eventId(UUID.randomUUID().toString())
//                .createdAt(Instant.now())
//                .eventType(eventType.name())
//                .build();
//
//        DeliveryPayload deliveryPayload= new DeliveryPayload("delivery Event body");
//
//        DeliveryEvent deliveryEvent=new DeliveryEvent(deliveryHeader,deliveryPayload);

        // this.deliveryMomProducer.push(deliveryEvent);
    }
}