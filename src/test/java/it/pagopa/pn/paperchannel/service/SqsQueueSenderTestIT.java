package it.pagopa.pn.paperchannel.service;


import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.DeliveryPushMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.NormalizeAddressQueueMomProducer;
import it.pagopa.pn.paperchannel.model.ExternalChannelError;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

class SqsQueueSenderTestIT extends BaseTest {

    @SpyBean
    private DeliveryPushMomProducer deliveryPushMomProducer;

    @SpyBean
    private InternalQueueMomProducer internalQueueMomProducer;

    @SpyBean
    private NormalizeAddressQueueMomProducer normalizeAddressQueueMomProducer;

    @Autowired
    private SqsSender sqsSender;


    @BeforeEach
    void setUp(){
        Mockito.doNothing().when(deliveryPushMomProducer)
                .push((DeliveryPushEvent) Mockito.any());

        Mockito.doNothing().when(internalQueueMomProducer)
                .push((InternalPushEvent) Mockito.any());

        Mockito.doNothing().when(normalizeAddressQueueMomProducer)
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


    @Test
    void pushSingleStatusUpdateEventTest(){
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setRequestId("requestid");
        paperProgressStatusEventDto.setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));
        paperProgressStatusEventDto.setStatusCode("CODE");
        paperProgressStatusEventDto.setStatusDescription("DESCRIPTION");
        paperProgressStatusEventDto.setRegisteredLetterCode("LETTER");
        paperProgressStatusEventDto.setProductType("890");
        paperProgressStatusEventDto.setClientRequestTimeStamp(Instant.now().atOffset(ZoneOffset.UTC));

        singleStatusUpdateDto.setAnalogMail(paperProgressStatusEventDto);
        this.sqsSender.pushSingleStatusUpdateEvent(singleStatusUpdateDto);

        Mockito.verify(internalQueueMomProducer, Mockito.times(1))
                .push((InternalPushEvent) Mockito.any());
    }

    @Test
    void pushToNormalizeAddressQueueTest(){
        final PrepareNormalizeAddressEvent event = PrepareNormalizeAddressEvent.builder()
                .requestId("")
                .iun("")
                .isAddressRetry(false)
                .attempt(0)
                .build();
        this.sqsSender.pushToNormalizeAddressQueue(event);
        Mockito.verify(normalizeAddressQueueMomProducer, Mockito.times(1))
                .push((InternalPushEvent) Mockito.any());
    }



    private SendEvent getSendEvent() {
        SendEvent event = new SendEvent();
        event.setRequestId("1234");
        event.setStatusCode(StatusCodeEnum.OK);
        event.setStatusDateTime(Instant.now());
        event.setStatusDescription(StatusCodeEnum.OK.getValue());
        event.setRegisteredLetterCode("AR");
        return event;
    }

    private PrepareEvent getPrepareEvent() {
        PrepareEvent event = new PrepareEvent();
        AnalogAddress address = new AnalogAddress();
        address.setFullname("fullName");
        address.setAddress("address");
        address.setCap("cap");
        address.setCity("city");
        address.setPr("pr");
        address.setCountry("country");

        event.setRequestId("1234");
        event.setStatusCode(StatusCodeEnum.OK);
        event.setStatusDateTime(Instant.now());
        event.setStatusDetail(StatusCodeEnum.OK.getValue());
        event.setProductType("AR");
        event.setReceiverAddress(address);
        return event;
    }
}
