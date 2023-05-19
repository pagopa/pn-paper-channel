package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;


import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.PN_EVENT_HEADER_EVENT_TYPE;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_ATTEMPT;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_EXPIRED;
import static org.junit.jupiter.api.Assertions.*;


@Slf4j
class QueueListenerTest extends BaseTest {

    @Autowired
    private QueueListener queueListener;
    @MockBean
    private QueueListenerService queueListenerService;
    @MockBean
    private PaperRequestErrorDAO paperRequestErrorDAO;
    @MockBean
    private SqsSender sender;

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
        Mockito.when(paperRequestErrorDAO.created(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(requestError));
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }

    @Test
    void internalQueueEventNationalRegExpiredTest(){
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
        Mockito.doNothing().when(sender).rePushInternalError(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }

    @Test
    void internalQueueEventNationalRegistryErrorTest(){
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
        Mockito.doNothing().when(queueListenerService).nationalRegistriesErrorListener(Mockito.any(), Mockito.anyInt());
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }

    @Test
    void internalQueueEventExternalChannelJsonBadlyTest(){
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
        PnGenericException exception = assertThrows(PnGenericException.class, () ->{
            queueListener.pullFromInternalQueue(json, headers);
        });
        assertEquals(MAPPER_ERROR, exception.getExceptionType());
    }

    @Test
    void internalQueueEventExternalChannelNoAttempsTest(){
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
        Mockito.when(paperRequestErrorDAO.created(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(requestError));
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }

    @Test
    void internalQueueEventExternalChannelExpiredTest(){
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
        Mockito.doNothing().when(sender).rePushInternalError(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }

    @Test
    void internalQueueEventExternalChannelErrorTest(){
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
        Mockito.doNothing().when(queueListenerService).externalChannelListener(Mockito.any(), Mockito.anyInt());
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }

    @Test
    void internalQueueEventSafeStorageJsonBadlyTest(){
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
        PnGenericException exception = assertThrows(PnGenericException.class, () ->{
            queueListener.pullFromInternalQueue(json, headers);
        });
        assertEquals(MAPPER_ERROR, exception.getExceptionType());
    }

    @Test
    void internalQueueEventSafeStorageNoAttempsTest(){
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
        Mockito.when(paperRequestErrorDAO.created(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(Mono.just(requestError));
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }

    @Test
    void internalQueueEventSafeStorageExpiredTest(){
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
        Mockito.doNothing().when(sender).rePushInternalError(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
        queueListener.pullFromInternalQueue(json, headers);
        assertTrue(true);
    }

    @Test
    void internalQueueEventSafeStorageErrorTest(){
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
        Mockito.doNothing().when(queueListenerService).internalListener(Mockito.any(), Mockito.anyInt());
        queueListener.pullFromInternalQueue(json, headers);
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
        Mockito.doNothing().when(sender).pushToInternalQueue(Mockito.any());
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
        PnGenericException exception = assertThrows(PnGenericException.class, ()->{
            Mockito.doNothing().when(sender).pushToInternalQueue(Mockito.any());
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

}
