package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class ChainedMessageHandlerTest {

    private ChainedMessageHandler chainedMessageHandler;

    @Test
    void handleMessage() {

        chainedMessageHandler = ChainedMessageHandler.builder()
                .handlers(List.of((entity, paperRequest) -> {
                    log.info("handler1");
                    return Mono.empty();
                }, (entity, paperRequest) -> {
                    log.info("handler2");
                    return Mono.empty();
                }))
                .build();


        StepVerifier.create(chainedMessageHandler.handleMessage(new PnDeliveryRequest(), new PaperProgressStatusEventDto()))
                .verifyComplete();
    }


    @Test
    void handleMessageError() {

        chainedMessageHandler = ChainedMessageHandler.builder()
                .handlers(List.of((entity, paperRequest) -> {
                    log.info("handler1");
                    return Mono.error(new RuntimeException("stoop"));
                }, (entity, paperRequest) -> {
                    log.info("handler2");
                    return Mono.empty();
                }))
                .build();


        StepVerifier.create(chainedMessageHandler.handleMessage(new PnDeliveryRequest(), new PaperProgressStatusEventDto()))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof RuntimeException);
                    assertEquals("stoop",((RuntimeException) ex).getMessage());
                    return true;
                }).verify();
    }
}