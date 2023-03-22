package it.pagopa.pn.paperchannel.service;


import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.DeliveryPushMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.paperchannel.model.ExternalChannelError;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

class SqsQueueSenderTest extends BaseTest {

    @SpyBean
    private DeliveryPushMomProducer deliveryPushMomProducer;

    @SpyBean
    private InternalQueueMomProducer internalQueueMomProducer;

    @Autowired
    private SqsSender sqsSender;


    @BeforeEach
    void setUp(){
        Mockito.doNothing().when(deliveryPushMomProducer)
                .push((DeliveryPushEvent) Mockito.any());

        Mockito.doNothing().when(internalQueueMomProducer)
                .push((InternalPushEvent) Mockito.any());
    }

    @Test
    void pushSendEventOnDeliveryQueueTest(){
        this.sqsSender.pushSendEvent(getSendEvent());
        Mockito.verify(deliveryPushMomProducer, Mockito.times(1))
                .push((DeliveryPushEvent) Mockito.any());
    }

    @Test
    void pushPrepareEventOnDeliveryQueueTest(){

        this.sqsSender.pushPrepareEvent(getPrepareEvent());
        Mockito.verify(deliveryPushMomProducer, Mockito.times(1)).push((DeliveryPushEvent) Mockito.any());
    }

    @Test
    void pushToInternalQueueTest(){
        PrepareAsyncRequest request = new PrepareAsyncRequest("1234", "iun", false, 1);
        this.sqsSender.pushToInternalQueue(request);
        Mockito.verify(internalQueueMomProducer, Mockito.times(1))
                .push((InternalPushEvent) Mockito.any());
    }

    @Test
    void pushToNationalRegistryErrorQueueTest(){
        NationalRegistryError error = new NationalRegistryError();
        error.setMessage("Error");
        error.setReceiverType("PF");
        error.setIun("IUN");
        error.setFiscalCode("MMDDD945439adfsf");
        error.setCorrelationId("correlation");
        this.sqsSender.pushInternalError(error, 1, NationalRegistryError.class);

        Mockito.verify(internalQueueMomProducer, Mockito.times(1))
                .push((InternalPushEvent) Mockito.any());
    }

    @Test
    void pushToExternalChannelErrorErrorQueueTest(){
        ExternalChannelError error = new ExternalChannelError();
        this.sqsSender.pushInternalError(error, 1, ExternalChannelError.class);

        Mockito.verify(internalQueueMomProducer, Mockito.times(1))
                .push((InternalPushEvent) Mockito.any());
    }

    @Test
    void pushToPrepareAsyncRequestErrorQueueTest(){
        PrepareAsyncRequest error = new PrepareAsyncRequest();
        this.sqsSender.pushInternalError(error, 1, PrepareAsyncRequest.class);

        Mockito.verify(internalQueueMomProducer, Mockito.times(1))
                .push((InternalPushEvent) Mockito.any());
    }

    @Test
    void rePushToExternalChannelErrorQueueTest(){
        ExternalChannelError error = new ExternalChannelError();
        this.sqsSender.rePushInternalError(error, 1, Instant.now().plus(200, ChronoUnit.MINUTES), ExternalChannelError.class);

        Mockito.verify(internalQueueMomProducer, Mockito.times(1))
                .push((InternalPushEvent) Mockito.any());
    }



    private SendEvent getSendEvent() {
        SendEvent event = new SendEvent();
        event.setRequestId("1234");
        event.setStatusCode(StatusCodeEnum.OK);
        event.setStatusDateTime(new Date());
        event.setStatusDescription(StatusCodeEnum.OK.getValue());
        event.setRegisteredLetterCode("AR");
        return event;
    }

    private PrepareEvent getPrepareEvent() {
        PrepareEvent event = new PrepareEvent();
        event.setRequestId("1234");
        event.setStatusCode(StatusCodeEnum.OK);
        event.setStatusDateTime(new Date());
        event.setStatusDetail(StatusCodeEnum.OK.getValue());
        event.setProductType("AR");
        return event;
    }
}
