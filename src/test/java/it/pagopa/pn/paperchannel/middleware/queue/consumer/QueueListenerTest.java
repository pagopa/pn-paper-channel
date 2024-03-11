package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.PN_EVENT_HEADER_EVENT_TYPE;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.MAPPER_ERROR;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_ATTEMPT;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_EXPIRED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class QueueListenerTest {

    @InjectMocks
    private QueueListener queueListener;
    @Mock
    private QueueListenerService queueListenerService;
    @Mock
    private PaperRequestErrorDAO paperRequestErrorDAO;
    @Mock
    private SqsSender sender;
    @Mock
    private PnPaperChannelConfig pnPaperChannelConfig;

    @Spy
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        //setto lo stesso objectMapper di Spring
        objectMapper.registerModule(new JavaTimeModule())
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void internalQueueOKTest(){
        String json = """
                {
                    "correlationId": "ideerf",
                    "requestId": "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.PREPARE_ASYNC_FLOW.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }
    @Test
    void internalQueueJsonBadlyTest(){
        String json = """
                {
                    correlationId: null,
                    requestId: "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.PREPARE_ASYNC_FLOW.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");
        PnGenericException exception = assertThrows(PnGenericException.class, () ->{
            queueListener.pullFromInternalQueue(json, headers);
        });
        assertEquals(MAPPER_ERROR, exception.getExceptionType());
    }

    @Test
    void internalQueueNationalRegJsonBadlyTest(){
        String json = """
                {
                    correlationId: null,
                    requestId: "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");
        PnGenericException exception = assertThrows(PnGenericException.class, () ->{
            queueListener.pullFromInternalQueue(json, headers);
        });
        assertEquals(MAPPER_ERROR, exception.getExceptionType());
    }


    @Test
    void internalQueueEventNationalRegNoAttempsTest(){

        // Given
        String json = """
                {
                    "correlationId": "abc",
                    "requestId": "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "4");
        PnRequestError requestError = new PnRequestError();

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(requestError));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }

    @Test
    void internalQueueEventNationalRegExpiredTest(){

        // Given
        String json = """
                {
                    "correlationId": "abc",
                    "requestId": "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2030-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        Mockito.when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }

    @Test
    void internalQueueEventNationalRegistryErrorTest(){

        // Given
        String json = """
                {
                    "correlationId": "abc",
                    "requestId": "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-02-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }


    @Test
    void pullF24OkTest(){
        String json = """
                {
                     "clientId": "123",
                     "pdfSetReady":
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "status": "OK",
                        "generatedPdfsUrls": [{
                            "pathTokens": "0/1",
                            "uri": "safestorage://e4r56t78"
                            },{
                            "pathTokens": "0/2",
                            "uri": "safestorage://e4dfgdfyhk8"
                            }]
                     }
                }
                """;
        Assertions.assertDoesNotThrow(() -> queueListener.pullF24(json, new HashMap<>()));
    }

    @Test
    void internalQueueEventF24ErrorTest(){

        // Given
        String json = """
                {
                    "correlationId": "abc",
                    "requestId": "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.F24_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-02-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }


    @Test
    void internalQueueEventExternalChannelJsonBadlyTest(){

        // Given
        String json = """
                {
                    correlationId: null,
                    requestId: "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        PnGenericException exception = assertThrows(PnGenericException.class, () -> queueListener.pullFromInternalQueue(json, headers));

        // Then
        assertEquals(MAPPER_ERROR, exception.getExceptionType());
    }

    @Test
    void internalQueueEventExternalChannelNoAttempsTest(){

        // Given
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail":
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null,
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode": "002",
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "4");
        PnRequestError requestError = new PnRequestError();

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(requestError));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }

    @Test
    void internalQueueEventExternalChannelExpiredTest(){

        // Given
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail":
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null,
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode": "002",
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2030-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }

    @Test
    void internalQueueEventExternalChannelErrorTest(){

        // Given
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail":
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null,
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode": "002",
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-02-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }

    @Test
    void internalQueueEventSafeStorageJsonBadlyTest(){

        // Given
        String json = """
                {
                    correlationId: null,
                    requestId: "NRTK-EWZL-KVPV-202212-Q-1124ds"
                }
                """;
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.SAFE_STORAGE_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        PnGenericException exception = assertThrows(PnGenericException.class, () -> queueListener.pullFromInternalQueue(json, headers));

        // Then
        assertEquals(MAPPER_ERROR, exception.getExceptionType());
    }

    @Test
    void internalQueueEventSafeStorageNoAttempsTest(){

        // Given
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail":
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null,
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode": "002",
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.SAFE_STORAGE_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "4");
        PnRequestError requestError = new PnRequestError();

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(requestError));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }

    @Test
    void internalQueueEventSafeStorageExpiredTest(){

        // Given
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail":
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null,
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode": "002",
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.SAFE_STORAGE_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2030-04-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }

    @Test
    void internalQueueEventSafeStorageErrorTest(){

        // Given
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail":
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null,
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode": "002",
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.SAFE_STORAGE_ERROR.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, "2023-02-12T14:35:35.135725152Z");
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");

        // When
        when(paperRequestErrorDAO.created(Mockito.any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));
        queueListener.pullFromInternalQueue(json, headers);

        // Then
        assertTrue(true);
    }

    @Test
    void pullNationalRegistriesOkTest(){
        String json = """
                {
                    "correlationId": "string",
                    "taxId": "CODICEFISCALE200",
                    "digitalAddress": null,
                    "physicalAddress":
                    {
                        "at": "MarioRossi",
                        "address": "ViaAldoMoro",
                        "addressDetails": "39",
                        "zip": "21047",
                        "municipality": "Saronno",
                        "municipalityDetails": "test",
                        "province": "VA",
                        "foreignState": "Italy"
                    }
                }
                """;
        queueListener.pullNationalRegistries(json, new HashMap<>());
        assertTrue(true);

    }
    @Test
    void pullNationalRegistriesKOJsonTest(){
        String json = """
                {
                    correlationId: 12,
                    "taxId": "CODICEFISCALE200",
                    "digitalAddress": null,
                    "physicalAddress":
                    {
                        "at": "MarioRossi",
                        "address": "ViaAldoMoro",
                        "addressDetails": "39",
                        "zip": "21047",
                        "municipality": "Saronno",
                        "municipalityDetails": "test",
                        "province": "VA",
                        "foreignState": "Italy"
                    }
                }
                """;
        PnGenericException exception = assertThrows(PnGenericException.class, ()-> {
            queueListener.pullNationalRegistries(json, new HashMap<>());
        });
        assertEquals(MAPPER_ERROR, exception.getExceptionType());

    }

    @Test
    void pullExternalChannelOkTest(){
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail":
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null,
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode": "002",
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        queueListener.pullExternalChannel(json, new HashMap<>());
        assertTrue(true);
    }

    @Test
    void pullExternalChannelKOJsonTest(){
        String json = """
                {
                     digitalCourtesy: null,
                     "digitalLegal": null,
                     "analogMail":
                     {
                        "registeredLetterCode": null,
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode": "002",
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        PnGenericException exception = assertThrows(PnGenericException.class, ()->{
            queueListener.pullExternalChannel(json, new HashMap<>());
        });
        assertEquals(MAPPER_ERROR, exception.getExceptionType());
    }

    @Test
    void pullManualRetryExternalChannelOK(){
        String json = """
                {
                    "requestId": "1234RequestId",
                    "newPcRetry": "newPcRetry123"
                }""";
        Map<String, Object> headers = new HashMap<>();
        headers.put(PN_EVENT_HEADER_EVENT_TYPE, EventTypeEnum.MANUAL_RETRY_EXTERNAL_CHANNEL.name());
        headers.put(PN_EVENT_HEADER_EXPIRED, Instant.now().minus(30, ChronoUnit.SECONDS).toString());
        headers.put(PN_EVENT_HEADER_ATTEMPT, "0");
        doNothing().when(this.queueListenerService).manualRetryExternalChannel("1234RequestId", "newPcRetry123");

        assertDoesNotThrow(() -> {
            queueListener.pullFromInternalQueue(json, headers);
        });
    }

}
