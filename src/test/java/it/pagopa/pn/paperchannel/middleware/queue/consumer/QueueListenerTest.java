package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;


@Slf4j
class QueueListenerTest extends BaseTest {

    @InjectMocks
    private QueueListener queueListener;
    @Mock
    private QueueListenerService queueListenerService;
    @Mock
    private PaperAsyncService paperAsyncService;

    @Mock
    private SqsSender sender;
    @Mock
    private PaperResultAsyncService paperResultAsyncService;
    @Spy
    @Autowired
    private ObjectMapper objectMapper;


    //@Test
    void internalQueueJsonBadlyTest(){
        String json = "{}";
        try {
            queueListener.pullFromInternalQueue(json, new HashMap<>());
            fail("Error with pull. Missed an exception");
        } catch (PnGenericException ex){
            assertNotNull(ex);
            assertNotNull(ex.getExceptionType());
            assertEquals(PREPARE_ASYNC_LISTENER_EXCEPTION, ex.getExceptionType());
        }
    }


    //@Test
    void internalQueuePrepareAsyncThrowErrorTest(){
        String json = "{\"correlationId\": null,\"requestId\":\"NRTK-EWZL-KVPV-202212-Q-1124ds\"}";
        try {
            Mockito.when(paperAsyncService.prepareAsync(Mockito.any())).thenThrow(new RuntimeException());
            queueListener.pullFromInternalQueue(json, new HashMap<>());
            fail("Error with pull. Missed an exception");
        } catch (PnGenericException ex){
            assertNotNull(ex);
            assertNotNull(ex.getExceptionType());
            assertEquals(PREPARE_ASYNC_LISTENER_EXCEPTION, ex.getExceptionType());
        }
    }

    @Test
    void internalQueueOKTest(){
        String json = "{\"correlationId\": null,\"requestId\":\"NRTK-EWZL-KVPV-202212-Q-1124ds\"}";
        Mockito.when(paperAsyncService.prepareAsync(Mockito.any())).thenReturn(Mono.just(new PnDeliveryRequest()));
        queueListener.pullFromInternalQueue(json, new HashMap<>());
        assertTrue(true);
    }

    //@Test
    void pullNationalRegistriesJsonBadlyTest(){
        String json = "{}";
        try {
            queueListener.pullNationalRegistries(json, new HashMap<>());
            fail("Error with pull. Missed an exception");
        }
        catch (PnGenericException ex){
            assertNotNull(ex);
            assertNotNull(ex.getExceptionType());
            assertEquals(UNTRACEABLE_ADDRESS, ex.getExceptionType());
        }
    }

    //@Test
    void pullNationalRegistriesThrowErrorTest(){
        String json = "{\"correlationId\":\"\",\"taxId\":\"CODICEFISCALE200\",\"digitalAddress\":null,\"physicalAddress\":{\"at\":\"MarioRossi\",\"address\":\"ViaAldoMoro\",\"addressDetails\":\"39\",\"zip\":\"21047\",\"municipality\":\"Saronno\",\"municipalityDetails\":\"Test\",\"province\":\"VA\",\"foreignState\":\"Italy\"}}";
        try {
            queueListener.pullNationalRegistries(json, new HashMap<>());
            fail("Error with pull. Missed an exception");
        }
        catch (PnGenericException ex){
            assertNotNull(ex);
            assertNotNull(ex.getExceptionType());
            assertEquals(UNTRACEABLE_ADDRESS, ex.getExceptionType());

        }
    }

    @Test
    void pullNationalRegistriesOkTest(){
        String json = "{\"correlationId\":\"string\",\"taxId\":\"CODICEFISCALE200\",\"digitalAddress\":null,\"physicalAddress\":{\"at\":\"MarioRossi\",\"address\":\"ViaAldoMoro\",\"addressDetails\":\"39\",\"zip\":\"21047\",\"municipality\":\"Saronno\",\"municipalityDetails\":\"Test\",\"province\":\"VA\",\"foreignState\":\"Italy\"}}";
        Mockito.doNothing().when(sender).pushToInternalQueue(Mockito.any());
        queueListener.pullNationalRegistries(json, new HashMap<>());
        assertTrue(true);

    }

    //@Test
    void pullExternalChannelOkTest(){
        String json = "{\"digitalCourtesy\": null, \"digitalLegal\": null, \"analogMail\": { \"requestId\": \"AKUZ-AWPL-LTPX-20230415\", \"registeredLetterCode\": null, \"productType\": \"AR\", \"iun\": \"AKUZ-AWPL-LTPX-20230415\", \"statusCode\": \"002\", \"statusDescription\": \"Mock status\", \"statusDateTime\": \"2023-01-12T14:35:35.135725152Z\", \"deliveryFailureCause\": null, \"attachments\": null, \"discoveredAddress\": null, \"clientRequestTimeStamp\": \"2023-01-12T14:35:35.13572075Z\"}}";
        Mockito.when(paperResultAsyncService.resultAsyncBackground(Mockito.any(), Mockito.any())).thenReturn(Mono.just(new PnDeliveryRequest()));
        queueListener.pullExternalChannel(json, new HashMap<>());
        assertTrue(true);
    }

}
