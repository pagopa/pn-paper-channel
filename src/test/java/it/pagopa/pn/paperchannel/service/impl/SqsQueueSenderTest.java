package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.DeliveryPushMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class SqsQueueSenderTest extends BaseTest {

    @InjectMocks
    private SqsQueueSender sqsQueueSender;

    @Mock
    private DeliveryPushMomProducer deliveryPushMomProducer;

    @Mock
    private InternalQueueMomProducer internalQueueMomProducer;

    @Test
    void pushSendEventOkTest() {
        Mockito.doNothing().when(deliveryPushMomProducer).push((DeliveryPushEvent) Mockito.any());
        sqsQueueSender.pushSendEvent(new SendEvent());
        assertTrue(true);
    }


    @Test
    void pushPrepareEventOkTest() {
        Mockito.doNothing().when(deliveryPushMomProducer).push((DeliveryPushEvent) Mockito.any());
        sqsQueueSender.pushPrepareEvent(new PrepareEvent());
        assertTrue(true);
    }

    @Test
    void pushToInternalQueue() {
        Mockito.doNothing().when(internalQueueMomProducer).push((InternalPushEvent) Mockito.any());
        sqsQueueSender.pushToInternalQueue(new PrepareAsyncRequest());
        assertTrue(true);
    }
}