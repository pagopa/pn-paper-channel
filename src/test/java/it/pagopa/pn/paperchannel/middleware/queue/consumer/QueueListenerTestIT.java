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
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import it.pagopa.pn.paperchannel.service.NationalRegistryService;
import it.pagopa.pn.paperchannel.service.PreparePhaseOneAsyncService;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ADDRESS_MANAGER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Disabled
@SpringBootTest(properties = {
        "pn.paper-channel.queue-external-channel=local-ext-channels-outputs-test",
        "pn.paper-channel.queue-normalize-address=local-paper-normalize-address-test"
})
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

    @MockBean
    private PreparePhaseOneAsyncService preparePhaseOneAsyncService;

    @MockBean
    private NationalRegistryService nationalRegistryService;


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
        when(paperRequestErrorDAO.created(any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));

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
        verify(paperRequestErrorDAO, never()).created(any(PnRequestError.class));

        //mi aspetto che dopo aver letto il messaggio 2 volte, quest'ultimo venga re-indirizzato in DLQ
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    final ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                            .queueUrl(config.getQueueExternalChannel() + "-DLQ")
                            .build());

                    assertThat(receiveMessageResponse.hasMessages()).isTrue();
                    final Message message = receiveMessageResponse.messages().get(0);
                    assertThat(message.body()).isEqualTo(messageJson);
                });
    }

    @Test
    void pullFromNormalizeAddressQueuePrepareEventTest() {
        var message = """
                {
                   "requestId":"41873b5b-b6a7-47b4-b170-2c38c0eb47ec",
                   "iun":"iun",
                   "correlationId":null,
                   "address":null,
                   "attempt":0,
                   "clientId":null,
                   "addressRetry":false
                 }
                """;

        var headers = Map.of(
                "attempt", MessageAttributeValue.builder().dataType("String").stringValue("0").build(),
                "eventType", MessageAttributeValue.builder().dataType("String").stringValue("PREPARE_ASYNC_FLOW").build()
        );

        when(preparePhaseOneAsyncService.preparePhaseOneAsync(any())).thenReturn(Mono.just(new PnDeliveryRequest()));


        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(config.getQueueNormalizeAddress())
                .messageAttributes(headers)
                .messageBody(message)
                .build();

        sqsClient.sendMessage(sendMessageRequest);

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> verify(queueListenerService, times(1)).normalizeAddressListener(any(), anyInt()));

        PrepareNormalizeAddressEvent expectedEvent = PrepareNormalizeAddressEvent.builder()
                .requestId("41873b5b-b6a7-47b4-b170-2c38c0eb47ec")
                .iun("iun")
                .build();
        verify(preparePhaseOneAsyncService, times(1)).preparePhaseOneAsync(expectedEvent);
    }

    @Test
    void pullFromNormalizeAddressQueueNationalRegistriesErrorEventWithAttemptNotFinishedTest() {
        var message = """
                {
                   "requestId":"PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1",
                   "iun":"iun",
                   "message":"Exception",
                   "fiscalCode":"FFFFFFFFFFFFFF",
                   "receiverType":"PF",
                   "setRelatedRequestId":"PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0"
                 }
                """;

        var headers = Map.of(
                "attempt", MessageAttributeValue.builder().dataType("String").stringValue("1").build(),
                "eventType", MessageAttributeValue.builder().dataType("String").stringValue("NATIONAL_REGISTRIES_ERROR").build()
        );

        doNothing().when(nationalRegistryService).finderAddressFromNationalRegistries(any(), any(), any(), any(), any(), any());

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(config.getQueueNormalizeAddress())
                .messageAttributes(headers)
                .messageBody(message)
                .build();

        sqsClient.sendMessage(sendMessageRequest);

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> verify(queueListenerService, times(1)).nationalRegistriesErrorListener(any(), eq(1)));

        verify(nationalRegistryService, times(1)).finderAddressFromNationalRegistries(any(), any(), any(), any(), any(), any());
    }

    @Test
    void pullFromNormalizeAddressQueueNationalRegistriesErrorEventWithAttemptFinishedTest() {
        ArgumentCaptor<PnRequestError> requestErrorArgumentCaptor = ArgumentCaptor.forClass(PnRequestError.class);

        var message = """
                {
                   "requestId":"PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1",
                   "iun":"iun",
                   "message":"Exception",
                   "fiscalCode":"FFFFFFFFFFFFFF",
                   "receiverType":"PF",
                   "setRelatedRequestId":"PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_0"
                 }
                """;

        var headers = Map.of(
                "attempt", MessageAttributeValue.builder().dataType("String").stringValue(config.getAttemptQueueNationalRegistries().toString()).build(),
                "eventType", MessageAttributeValue.builder().dataType("String").stringValue("NATIONAL_REGISTRIES_ERROR").build()
        );


        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(new PnRequestError()));

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(config.getQueueNormalizeAddress())
                .messageAttributes(headers)
                .messageBody(message)
                .build();

        sqsClient.sendMessage(sendMessageRequest);


        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> verify(paperRequestErrorDAO, times(1)).created(requestErrorArgumentCaptor.capture()));

        PnRequestError actualError = requestErrorArgumentCaptor.getValue();
        assertThat(actualError).isNotNull();
        assertThat(actualError.getRequestId()).isEqualTo("PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1");
        assertThat(actualError.getError()).isEqualTo("ERROR WITH RETRIEVE ADDRESS");
        assertThat(actualError.getFlowThrow()).isEqualTo(EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name());

        verify(queueListenerService, never()).nationalRegistriesErrorListener(any(), anyInt());
        verify(nationalRegistryService, never()).finderAddressFromNationalRegistries(any(), any(), any(), any(), any(), any());
    }

    @Test
    void pullFromNormalizeAddressQueueAddressManagerErrorEventWithAttemptNotFinishedTest() {
        PrepareNormalizeAddressEvent expectedEvent = PrepareNormalizeAddressEvent.builder()
                .requestId("PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1")
                .correlationId("corrId")
                .iun("iun")
                .isAddressRetry(false)
                .attempt(1)
                .build();
        var message = """
                {
                   "requestId":"PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1",
                   "correlationId":"corrId",
                   "iun":"iun",
                   "isAddressRetry":"true",
                   "attempt":"1"
                 }
                """;

        var headers = Map.of(
                "attempt", MessageAttributeValue.builder().dataType("String").stringValue("1").build(),
                "eventType", MessageAttributeValue.builder().dataType("String").stringValue("ADDRESS_MANAGER_ERROR").build()
        );

        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(new PnRequestError()));

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(config.getQueueNormalizeAddress())
                .messageAttributes(headers)
                .messageBody(message)
                .build();

        sqsClient.sendMessage(sendMessageRequest);

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> verify(queueListenerService, times(1)).normalizeAddressListener(expectedEvent, 1));

        verify(preparePhaseOneAsyncService, times(1)).preparePhaseOneAsync(expectedEvent);
    }

    @Test
    void pullFromNormalizeAddressQueueAddressManagerErrorEventWithAttemptFinishedTest() {
        ArgumentCaptor<PnRequestError> requestErrorArgumentCaptor = ArgumentCaptor.forClass(PnRequestError.class);
        var message = """
                {
                   "requestId":"PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1",
                   "correlationId":"corrId",
                   "iun":"iun",
                   "isAddressRetry":"true",
                   "attempt":"3"
                 }
                """;

        var headers = Map.of(
                "attempt", MessageAttributeValue.builder().dataType("String").stringValue("3").build(),
                "eventType", MessageAttributeValue.builder().dataType("String").stringValue("ADDRESS_MANAGER_ERROR").build()
        );

        when(paperRequestErrorDAO.created(any())).thenReturn(Mono.just(new PnRequestError()));

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(config.getQueueNormalizeAddress())
                .messageAttributes(headers)
                .messageBody(message)
                .build();

        sqsClient.sendMessage(sendMessageRequest);

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> verify(paperRequestErrorDAO, times(1)).created(requestErrorArgumentCaptor.capture()));

        PnRequestError actualError = requestErrorArgumentCaptor.getValue();
        assertThat(actualError).isNotNull();
        assertThat(actualError.getRequestId()).isEqualTo("PREPARE_ANALOG_DOMICILE.IUN_GJWA-HMEK-RGUJ-202307-H-1.RECINDEX_0.ATTEMPT_1");
        assertThat(actualError.getError()).isEqualTo(ADDRESS_MANAGER_ERROR.getMessage());
        assertThat(actualError.getFlowThrow()).isEqualTo(EventTypeEnum.ADDRESS_MANAGER_ERROR.name());

        verify(queueListenerService, never()).normalizeAddressListener(any(), anyInt());
        verify(preparePhaseOneAsyncService, never()).preparePhaseOneAsync(any());
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
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(config.getQueueNormalizeAddress()).build());
    }
}
