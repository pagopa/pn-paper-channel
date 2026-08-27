package it.pagopa.pn.paperchannel.service;


import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.queue.model.AttemptPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalPushEvent;
import it.pagopa.pn.paperchannel.middleware.queue.producer.EventBridgeProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.InternalQueueMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.NormalizeAddressQueueMomProducer;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.ZoneOffset;

class SqsQueueSenderTestIT extends BaseTest {

    private static final String CLIENT_ID = "clientId";

    @MockitoSpyBean
    private InternalQueueMomProducer internalQueueMomProducer;

    @MockitoSpyBean
    private NormalizeAddressQueueMomProducer normalizeAddressQueueMomProducer;

    @MockitoSpyBean
    private EventBridgeProducer eventBridgeProducer;

    @Autowired
    private SqsSender sqsSender;


    @BeforeEach
    void setUp(){

        Mockito.doNothing().when(internalQueueMomProducer)
                .push((InternalPushEvent) Mockito.any());

        Mockito.doNothing().when(normalizeAddressQueueMomProducer)
                .push((AttemptPushEvent) Mockito.any());

        Mockito.doNothing().when(eventBridgeProducer)
                .sendEvent(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void pushSendEventOnEventBridgeTest(){
        this.sqsSender.pushSendEventOnEventBridge(CLIENT_ID, getSendEvent());
        Mockito.verify(eventBridgeProducer, Mockito.times(1))
                .sendEvent(Mockito.anyString(), Mockito.anyString());
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
                .push((AttemptPushEvent) Mockito.any());
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
