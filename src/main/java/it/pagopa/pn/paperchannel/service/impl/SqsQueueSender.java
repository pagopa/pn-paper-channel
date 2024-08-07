package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.DeliveryPushMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.EventBridgeProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqsQueueSender implements SqsSender {

    private static final String PUBLISHER_UPDATE = "paper-channel-update";
    private static final String PUBLISHER_PREPARE = "paper-channel-prepare";

    private final DeliveryPushMomProducer deliveryPushMomProducer;
    private final InternalQueueMomProducer internalQueueMomProducer;
    private final EventBridgeProducer eventBridgeProducer;

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
        this.deliveryPushMomProducer.push(getDeliveryPushEvent(prepareEvent, sendEvent));
    }


    @Override
    public void pushToInternalQueue(PrepareAsyncRequest prepareAsyncRequest){
        InternalEventHeader prepareHeader= InternalEventHeader.builder()
                .publisher(PUBLISHER_PREPARE)
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(EventTypeEnum.PREPARE_ASYNC_FLOW.name())
                .clientId(prepareAsyncRequest.getClientId())
                .attempt(0)
                .expired(Instant.now())
                .build();

        InternalPushEvent<PrepareAsyncRequest> internalPushEvent = new InternalPushEvent<>(prepareHeader, prepareAsyncRequest);
        this.internalQueueMomProducer.push(internalPushEvent);
    }

    @Override
    public void pushDematZipInternalEvent(DematInternalEvent dematZipInternalEvent) {
        InternalEventHeader prepareHeader= InternalEventHeader.builder()
                .publisher(PUBLISHER_PREPARE)
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(EventTypeEnum.SEND_ZIP_HANDLE.name())
                .clientId("")
                .attempt(0)
                .expired(Instant.now())
                .build();

        InternalPushEvent<DematInternalEvent> internalPushEvent = new InternalPushEvent<>(prepareHeader, dematZipInternalEvent);
        this.internalQueueMomProducer.push(internalPushEvent);
    }

    @Override
    public void pushSingleStatusUpdateEvent(SingleStatusUpdateDto singleStatusUpdateDto) {
        InternalEventHeader prepareHeader= InternalEventHeader.builder()
            .publisher(PUBLISHER_PREPARE)
            .eventId(UUID.randomUUID().toString())
            .createdAt(Instant.now())
            .eventType(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name())
            .clientId("")
            .attempt(0)
            .expired(Instant.now())
            .build();

        InternalPushEvent<SingleStatusUpdateDto> internalPushEvent = new InternalPushEvent<>(prepareHeader, singleStatusUpdateDto);
        this.internalQueueMomProducer.push(internalPushEvent);
    }

    @Override
    public void pushSendEventOnEventBridge(String clientId, SendEvent event) {
        PaperChannelUpdate update = new PaperChannelUpdate();
        update.setSendEvent(event);
        update.setClientId(clientId);
        String jsonMessage = Utility.objectToJson(update);
        this.eventBridgeProducer.sendEvent(jsonMessage, event.getRequestId());
    }

    @Override
    public void pushPrepareEventOnEventBridge(String clientId, PrepareEvent event) {
        PaperChannelUpdate update = new PaperChannelUpdate();
        update.setPrepareEvent(event);
        update.setClientId(clientId);
        String jsonMessage = Utility.objectToJson(update);
        this.eventBridgeProducer.sendEvent(jsonMessage, event.getRequestId());
    }

    @Override
    public <T> void pushInternalError(T entity, int attempt, Class<T> tClass) {
        EventTypeEnum eventTypeEnum = getTypeEnum(entity, tClass);
        if (eventTypeEnum == null) return;
        InternalEventHeader prepareHeader= InternalEventHeader.builder()
                .publisher(PUBLISHER_PREPARE)
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .attempt(attempt+1)
                .eventType(eventTypeEnum.name())
                .expired(DateUtils.addedTime(attempt+1, 1))
                .build();
        this.internalQueueMomProducer.push(new InternalPushEvent<>(prepareHeader, entity));
        log.info("pushed to queue entity={}", entity);
    }

    @Override
    public <T> void rePushInternalError(T entity, int attempt, Instant expired, Class<T> tClass) {
        EventTypeEnum eventTypeEnum = getTypeEnum(entity, tClass);
        if (eventTypeEnum == null) return;
        InternalEventHeader prepareHeader= InternalEventHeader.builder()
                .publisher(PUBLISHER_PREPARE)
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .attempt(attempt)
                .eventType(eventTypeEnum.name())
                .expired(expired)
                .build();
        this.internalQueueMomProducer.push(new InternalPushEvent<>(prepareHeader, entity));
    }

    private <T> EventTypeEnum getTypeEnum(T entity, Class<T> tClass){
        EventTypeEnum typeEnum = null;
        if (tClass == NationalRegistryError.class) typeEnum = NATIONAL_REGISTRIES_ERROR;
        if (tClass == ExternalChannelError.class) typeEnum = EXTERNAL_CHANNEL_ERROR;
        if (tClass == PrepareAsyncRequest.class) typeEnum = SAFE_STORAGE_ERROR;
        if (tClass == F24Error.class) typeEnum = F24_ERROR;
        if (tClass == PrepareAsyncRequest.class && ((PrepareAsyncRequest) entity).isAddressRetry()) typeEnum = ADDRESS_MANAGER_ERROR;
        if (tClass == DematInternalEvent.class) typeEnum = ZIP_HANDLE_ERROR;

        return typeEnum;
    }

    private DeliveryPushEvent getDeliveryPushEvent(PrepareEvent prepareEvent, SendEvent sendEvent){
        GenericEventHeader deliveryHeader= GenericEventHeader.builder()
                .publisher(PUBLISHER_UPDATE)
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType((sendEvent == null) ? EventTypeEnum.PREPARE_ANALOG_RESPONSE.name(): EventTypeEnum.SEND_ANALOG_RESPONSE.name())
                .build();

        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        paperChannelUpdate.setPrepareEvent(prepareEvent);
        paperChannelUpdate.setSendEvent(sendEvent);

        DeliveryPushEvent deliveryPushEvent = new DeliveryPushEvent(deliveryHeader, paperChannelUpdate);
        if (prepareEvent != null && prepareEvent.getReceiverAddress() != null){
            log.debug(
                    "name surname: {}, address: {}, zip: {},city:{}, pr:{},foreign state: {}",
                    LogUtils.maskGeneric(prepareEvent.getReceiverAddress().getFullname()),
                    LogUtils.maskGeneric(prepareEvent.getReceiverAddress().getAddress()),
                    LogUtils.maskGeneric(prepareEvent.getReceiverAddress().getCap()),
                    LogUtils.maskGeneric(prepareEvent.getReceiverAddress().getCity()),
                    LogUtils.maskGeneric(prepareEvent.getReceiverAddress().getPr()),
                    LogUtils.maskGeneric(prepareEvent.getReceiverAddress().getCountry())
            );
        }
        return deliveryPushEvent;
    }


}