package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(properties = { "pn.paper-channel.queue-external-channel=local-ext-channels-outputs-test" })
class QueueListenerTestIT extends BaseTest
{

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PnPaperChannelConfig config;

    @SpyBean
    private QueueListenerService queueListenerService;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private AddressDAO addressDAO;

    @MockBean
    private ExternalChannelClient externalChannelClient;

    @MockBean
    private PaperRequestErrorDAO paperRequestErrorDAO;


    @Test
    void pullExternalChannelKoInDLQAndNotSavedInPaperErrorTest() {
        String iun = "MRGM-QVHD-UZAL-202307-M-1";
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.ATTEMPT_1";
        var messagePayload = createExtChannelMessage(requestId, iun);
        var pnDeliveryRequest = createPnDeliveryRequest(requestId, iun);

        when(requestDeliveryDAO.getByRequestId(requestId)).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(pnDeliveryRequest)).thenReturn(Mono.just(pnDeliveryRequest));
        when(addressDAO.findAllByRequestId(requestId)).thenReturn(Mono.just(createAddresses()));
        when(externalChannelClient.sendEngageRequest(any(), any())).thenReturn(Mono.error(WebClientResponseException.create(400, "", new HttpHeaders(), null, Charset.defaultCharset())));
        when(paperRequestErrorDAO.created(anyString(), anyString(), anyString())).thenReturn(Mono.just(new PnRequestError()));

        //invio il messaggio nella coda di ext-channel
        final String messageJson = toJson(messagePayload);
        sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(config.getQueueExternalChannel())
                .messageBody(messageJson)
                .build());

        //mi aspetto che a seguito del 400 di ext-channel, il messaggio venga letto 2 volte poiché in localstack è configurato maxReceiveCount=2
        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(queueListenerService, times(2)).externalChannelListener(any(), anyInt()));

        //mi aspetto che non cia mai invocato il salvataggio della PaperError
        verify(paperRequestErrorDAO, never()).created(anyString(), anyString(), anyString());

        //mi aspetto che dopo aver letto il messaggio 2 volte, quest'ultimo venga re-indirizzato in DLQ
        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                            .queueUrl(config.getQueueExternalChannel() + "-DLQ")
                            .build());

                    assertThat(receiveMessageResponse.hasMessages()).isTrue();
                    final Message message = receiveMessageResponse.messages().get(0);
                    assertThat(message.body()).isEqualTo(messageJson);
                });
    }

    private String toJson(Object obj) {
        try {
            return this.objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException var3) {
            throw new IllegalStateException(var3);
        }
    }

    private List<PnAddress> createAddresses() {
        var addressOne = new PnAddress();
        addressOne.setAddress(""); addressOne.setTypology(AddressTypeEnum.SENDER_ADDRES.name());
        addressOne.setCap("80120");
        addressOne.setCity("Napoli");
        addressOne.setCountry("ITALIA");
        addressOne.setAddressRow2("");
        addressOne.setNameRow2("row2");
        addressOne.setPr("NA");
        addressOne.setFullName("via full name");

        var addressTwo = new PnAddress();
        addressTwo.setAddress(""); addressTwo.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        addressTwo.setCap("80120");
        addressTwo.setCity("Napoli");
        addressTwo.setCountry("ITALIA");
        addressTwo.setAddressRow2("");
        addressTwo.setNameRow2("row2");
        addressTwo.setPr("NA");
        addressTwo.setFullName("via full name");

        return List.of(addressOne, addressTwo);
    }

    private SingleStatusUpdateDto createExtChannelMessage(String requestIdWithoutPcRetry, String iun) {
        return new SingleStatusUpdateDto()
                .analogMail(new PaperProgressStatusEventDto()
                        .requestId(requestIdWithoutPcRetry + ".PCRETRY_0")
                        .iun(iun)
                        .statusCode(ExternalChannelCodeEnum.RECAG013.name())
                        .statusDateTime(OffsetDateTime.now()));
    }

    private PnDeliveryRequest createPnDeliveryRequest(String requestId, String iun) {
        var pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId(requestId);
        pnDeliveryRequest.setStatusCode("RECAG013");
        pnDeliveryRequest.setIun(iun);
        pnDeliveryRequest.setProductType(ProductTypeEnum._890.getValue());
        pnDeliveryRequest.setAttachments(List.of());

        return pnDeliveryRequest;
    }

    @AfterEach
    public void clean() {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(config.getQueueExternalChannel() + "-DLQ").build());
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(config.getQueueExternalChannel()).build());
    }
}
