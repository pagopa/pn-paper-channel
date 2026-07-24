package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.MAPPER_ERROR;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class QueueListenerTest {

    @InjectMocks
    private QueueListener queueListener;
    @Mock
    private QueueListenerService queueListenerService;
    @Mock
    private PaperRequestErrorDAO paperRequestErrorDAO;
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
    void pullF24OkTest(){
        String json = """
                {
                     "clientId": "123",
                     "payloadType": "PnF24PdfSetReadyEvent",
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

}
