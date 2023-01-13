package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;


@Slf4j
class QueueListenerTest extends BaseTest {

    @InjectMocks
    private QueueListener queueListener;
    @Mock
    private PaperAsyncService paperAsyncService;
    @Mock
    private SqsSender sender;
    @Mock
    private PaperResultAsyncService paperResultAsyncService;
    @Mock
    private ObjectMapper objectMapper;


    @Test
    void internalQueueJsonBadlyTest(){
        String json = "{}";
        try {
            queueListener.pullFromInternalQueue(json, new HashMap<>());
            fail("Error with pull. Missed an exception");
        } catch (PnGenericException ex){
            assertNotNull(ex);
            assertNotNull(ex.getExceptionType());
            assertEquals(MAPPER_ERROR, ex.getExceptionType());
        }
    }


    @Test
    void internalQueuePrepareAsyncThrowErrorTest(){
        String json = "{'correlationId': null,'requestId':'NRTK-EWZL-KVPV-202212-Q-1124ds'}";
        try {
            Mockito.when(paperAsyncService.prepareAsync(Mockito.any())).thenThrow(new PnGenericException(DOCUMENT_URL_NOT_FOUND, DOCUMENT_URL_NOT_FOUND.getMessage()));
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
        String json = "{'correlationId': null,'requestId':'NRTK-EWZL-KVPV-202212-Q-1124ds'}";
        Mockito.when(paperAsyncService.prepareAsync(Mockito.any())).thenReturn(Mono.just(new PnDeliveryRequest()));
        queueListener.pullFromInternalQueue(json, new HashMap<>());
        assertTrue(true);
    }


}
