package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.GenericEventHeader;

import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.DeliveryPushMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
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
     @Autowired
     private InternalQueueMomProducer internalQueueMomProducer;

    @Override
    public void pushSendEvent(SendEvent event) {
        push(event, null);
    }

    @Override
    public void pushPrepareEvent(PrepareEvent event) {
        push(null, event);
    }

    private void push(SendEvent sendEvent, PrepareEvent prepareEvent){
        log.info("Push event to queue {}", (sendEvent != null ? sendEvent.getRequestId() : prepareEvent.getRequestId()));
        GenericEventHeader deliveryHeader= GenericEventHeader.builder()
                .publisher("paper-channel-update")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType((sendEvent == null) ? EventTypeEnum.PREPARE_ANALOG_RESPONSE.name(): EventTypeEnum.SEND_ANALOG_RESPONSE.name())
                .build();

        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        paperChannelUpdate.setPrepareEvent(prepareEvent);
        paperChannelUpdate.setSendEvent(sendEvent);

        DeliveryPushEvent deliveryPushEvent = new DeliveryPushEvent(deliveryHeader, paperChannelUpdate);

        this.deliveryPushMomProducer.push(deliveryPushEvent);
    }

    @Override
    public void pushToInternalQueue(PrepareAsyncRequest prepareAsyncRequest){
        GenericEventHeader prepareHeader= GenericEventHeader.builder()
                .publisher("paper-channel-prepare")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(EventTypeEnum.PREPARE_ASYNC_FLOW.name())
                .build();

        InternalPushEvent<PrepareAsyncRequest> internalPushEvent = new InternalPushEvent<>(prepareHeader, prepareAsyncRequest);
        this.internalQueueMomProducer.push(internalPushEvent);
    }

    @Override
    public void pushNationalRegistriesError(NationalRegistryError nationalRegistryError){
        GenericEventHeader prepareHeader= GenericEventHeader.builder()
                .publisher("paper-channel-prepare")
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name())
                .build();

        InternalPushEvent<NationalRegistryError> internalPushEvent = new InternalPushEvent<>(prepareHeader, nationalRegistryError);
        this.internalQueueMomProducer.push(internalPushEvent);
    }


}