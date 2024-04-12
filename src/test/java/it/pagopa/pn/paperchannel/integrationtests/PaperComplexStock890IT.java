package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.encryption.impl.DataVaultEncryptionImpl;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.service.impl.PaperResultAsyncServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest(properties = {
    "pn.paper-channel.enable-simple-890-flow=false"
})
class PaperComplexStock890IT extends BaseTest {

    private static final String IUN = "NEQP-YAZD-XNGK-202312-L-1";
    private static final String REQUEST_ID = "PREPARE_ANALOG_DOMICILE.IUN_" + IUN + ".RECINDEX_0.SENTATTEMPTMADE_1";

    @Autowired
    private PaperResultAsyncServiceImpl paperResultAsyncService;

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;

    @Autowired
    private MetaDematCleaner metaDematCleaner;

    @MockBean
    private SqsSender sqsSender;

    @MockBean
    private DataVaultEncryptionImpl dataVaultEncryption;

    @BeforeEach
    public void setUp() {
        buildAndCreateDeliveryRequest();
    }

    @AfterEach
    public void tearDown() {
        cleanEnvironment();
    }

    /* RECAG005C CASES */

    @Test
    void test_complex_890_within_10days_stock_RECAG005C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG005C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_within_10days_stock_RECAG005C_without_RECAG011A(){

        // Given
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG005B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        
        // Expect exception because RECAG011A and PNAG012 do not exist
        generateEvent(RECAG005C, PnGenericException.class);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 1 event to delivery push */
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(false, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_within_10days_stock_RECAG005C_with_RECAG011B(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG005C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_after_10days_stock_RECAG005C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG005C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 4 events to delivery push */
        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }


    @Test
    void test_complex_890_after_10days_stock_RECAG005C_without_RECAG011A(){
        // Given
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG005B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG011B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG005C, PnGenericException.class);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 1 event to delivery push */
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(false, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_after_10days_stock_RECAG005C_without_RECAG011B(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG005C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 4 events to delivery push */
        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_stock_RECAG012_RECAG011B_RECAG005C(){
        // Given
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",Arrays.asList("23L","CAD"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG005C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_stock_RECAG012_RECAG0011B_RECAG005C_singleAttachEvent(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);
        generateEvent(RECAG005C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());
    }

    /* RECAG006C CASES */

    @Test
    void test_complex_890_within_10days_stock_RECAG006C(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG006C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_within_10days_stock_RECAG006C_without_RECAG011A(){

        // Given
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG006B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG006C, PnGenericException.class);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 1 event to delivery push */
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(false, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_within_10days_stock_RECAG006C_with_RECAG011B(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG006C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_after_10days_stock_RECAG006C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG006C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 4 events to delivery push */
        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }


    @Test
    void test_complex_890_after_10days_stock_RECAG006C_without_RECAG011A(){
        // Given
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG006B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG011B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG006C, PnGenericException.class);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 1 event to delivery push */
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(false, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_after_10days_stock_RECAG006C_without_RECAG011B(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG006C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 4 events to delivery push */
        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_stock_RECAG012_RECAG011B_RECAG006C(){
        // Given
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",Arrays.asList("23L","CAD"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG006C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_stock_RECAG012_RECAG0011B_RECAG006C_singleAttachEvent(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);
        generateEvent(RECAG006C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());
    }

    /* RECAG007C CASES */

    @Test
    void test_complex_890_within_10days_stock_RECAG007C(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_within_10days_stock_RECAG007C_without_RECAG011A(){

        // Given
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG007B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, PnGenericException.class);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 1 event to delivery push */
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(false, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_within_10days_stock_RECAG007C_with_RECAG011B(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_after_10days_stock_RECAG007C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 4 events to delivery push */
        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }


    @Test
    void test_complex_890_after_10days_stock_RECAG007C_without_RECAG011A(){
        // Given
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG007B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG011B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, PnGenericException.class);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 1 event to delivery push */
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(false, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_after_10days_stock_RECAG007C_without_RECAG011B(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 4 events to delivery push */
        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_stock_RECAG012_RECAG011B_RECAG007C(){
        // Given
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",Arrays.asList("23L","CAD"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG007C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_stock_RECAG012_RECAG0011B_RECAG007C_singleAttachEvent(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);
        generateEvent(RECAG007C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());
    }

    /* RECAG008C CASES */

    @Test
    void test_complex_890_stock_RECAG012_RECAG011B_RECAG008C(){
        // Given
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",Arrays.asList("23L","CAD"), null);
        SingleStatusUpdateDto RECAG008C = buildStatusUpdateDto("RECAG008C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG008C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG008C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_stock_RECAG011B_RECAG012_RECAG008B_RECAG008C(){
        // Given

        SingleStatusUpdateDto RECAG011BArcad = buildStatusUpdateDto("RECAG011B", List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG008A = buildStatusUpdateDto("RECAG008A",null, null);
        SingleStatusUpdateDto RECAG008B = buildStatusUpdateDto("RECAG008B",List.of("Plico"), null);
        SingleStatusUpdateDto RECAG008C = buildStatusUpdateDto("RECAG008C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG008B", StatusCodeEnum.PROGRESS,
            "RECAG008C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011BArcad, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG008A, null);
        generateEvent(RECAG008B, null);
        generateEvent(RECAG008C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 4 events to delivery push */
        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());

        log.info("Event: \n"+capturedSendEvent.getAllValues());
    }

    @Test
    void test_complex_890_stock_RECAG012_RECAG0011B_RECAG008C_singleAttachEvent(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);
        SingleStatusUpdateDto RECAG008C = buildStatusUpdateDto("RECAG008C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG008C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);
        generateEvent(RECAG008C, null);

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        // Then

        /* Expected 3 events to delivery push */
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(true, deliveryRequest.getRefined());
    }


    /* BEGIN PRIVATE UTILS METHODS */

    private void generateEvent(SingleStatusUpdateDto singleStatusUpdateDto, Class<? extends Exception> exception){

        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        if (exception != null) {
            assertThrows(exception, () -> paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto, 0).block());
        } else {
            assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto, 0).block());
        }
    }

    private void buildAndCreateDeliveryRequest() {
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequestWithRequestId(REQUEST_ID, IUN);

        try {
            requestDeliveryDAO.createWithAddress(pnDeliveryRequest, null, null).block();
        } catch (PnHttpResponseException e) {
            log.info("Request delivery already exists, reset existing one");
            requestDeliveryDAO.updateData(pnDeliveryRequest).block();
        }
    }

    private void cleanEnvironment() {
        this.metaDematCleaner.clean(REQUEST_ID).block();
    }

    private SingleStatusUpdateDto buildStatusUpdateDto(String statusCode, List<String> attach, Instant statusDateTimeToSet) {
        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail(IUN);

        analogMail.setStatusCode(statusCode);
        analogMail.setProductType("890");

        if (statusDateTimeToSet != null) {
            analogMail.setStatusDateTime(OffsetDateTime.ofInstant(statusDateTimeToSet, ZoneOffset.UTC));
        }

        if(attach != null && !attach.isEmpty()){
            List<AttachmentDetailsDto> attachments = new LinkedList<>();
            for(String elem: attach){
                attachments.add(
                    new AttachmentDetailsDto()
                        .documentType(elem)
                        .date(OffsetDateTime.now())
                        .uri("https://safestorage.it"));
            }
            analogMail.setAttachments(attachments);
        }

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        return extChannelMessage;
    }
}
