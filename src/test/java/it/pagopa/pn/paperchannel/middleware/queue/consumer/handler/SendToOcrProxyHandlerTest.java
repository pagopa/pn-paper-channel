package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadInfoDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperChannelDeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperChannelDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.OcrInputPayload;
import it.pagopa.pn.paperchannel.middleware.queue.producer.OcrProducer;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.INVALID_SAFE_STORAGE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendToOcrProxyHandler Tests")
class SendToOcrProxyHandlerTest {

    @Mock private OcrProducer ocrProducer;
    @Mock private EventDematDAO eventDematDAO;
    @Mock private EventMetaDAO eventMetaDAO;
    @Mock private MessageHandler messageHandler;
    @Mock private SafeStorageClient safeStorageClient;
    @Mock private PaperChannelDeliveryDriverDAO deliveryDriverDAO;
    @Mock private PnPaperChannelConfig paperChannelConfig;
    @Mock private SqsSender sqsSender;

    private SendToOcrProxyHandler handler;
    private PnDeliveryRequest entity;
    private PaperProgressStatusEventDto paperRequest;
    private PnEventMeta eventMeta;
    private PnEventDemat eventDemat;
    private PaperChannelDeliveryDriver deliveryDriver;

    @BeforeEach
    void setUp() {
        // Arrange - Common test data setup
        handler = SendToOcrProxyHandler.builder()
                .eventDematDAO(eventDematDAO)
                .eventMetaDAO(eventMetaDAO)
                .messageHandler(messageHandler)
                .safeStorageClient(safeStorageClient)
                .deliveryDriverDAO(deliveryDriverDAO)
                .paperChannelConfig(paperChannelConfig)
                .sqsSender(sqsSender)
                .build();

        setupCommonTestData();
    }

    private void setupCommonTestData() {
        // Common test entities
        entity = new PnDeliveryRequest();
        entity.setDriverCode("FULMINE");
        entity.setRefined(true);

        Instant testInstant = Instant.now();
        OffsetDateTime testDateTime = OffsetDateTime.ofInstant(testInstant, ZoneOffset.UTC);

        paperRequest = new PaperProgressStatusEventDto();
        paperRequest.setRequestId("TEST_REQUEST_ID");
        paperRequest.setStatusCode("RECRN001C");
        paperRequest.setStatusDateTime(testDateTime);
        paperRequest.setProductType("AR");
        paperRequest.setRegisteredLetterCode("REG123");
        paperRequest.setDeliveryFailureCause("M01");

        eventMeta = new PnEventMeta();
        eventMeta.setStatusDateTime(testInstant);

        eventDemat = new PnEventDemat();
        eventDemat.setStatusDateTime(testInstant);
        eventDemat.setUri("safestorage://uri");

        deliveryDriver = new PaperChannelDeliveryDriver();
        deliveryDriver.setUnifiedDeliveryDriver("Fulmine");
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        void shouldProcessMessageAndSendToOcrWhenAllConditionsMet() {
            // Arrange
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));

            FileDownloadResponseDto downloadResponse = new FileDownloadResponseDto();
            FileDownloadInfoDto fileDownloadInfoDto = new FileDownloadInfoDto();
            fileDownloadInfoDto.setUrl("https://presigned-url.com");
            downloadResponse.setDownload(fileDownloadInfoDto);
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(downloadResponse));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender).pushToOcr(any(OcrInputPayload.class));
            verify(eventMetaDAO).getDeliveryEventMeta(eq("META##TEST_REQUEST_ID"), eq("META##RECRN001A"));
            verify(eventDematDAO).getDeliveryEventDemat(eq("DEMAT##TEST_REQUEST_ID"), eq("AR##RECRN001B"));
            verify(deliveryDriverDAO).getByDeliveryDriverId("FULMINE");
            verify(safeStorageClient).getFile("safestorage://uri");

            verifyOcrPayloadCorrectness(OcrInputPayload.DataDto.DocumentType.AR);
        }

        @Test
        void shouldHandleRECRN002FStatusCodeCorrectly() {
            // Arrange
            paperRequest.setStatusCode("RECRN002F");
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));

            FileDownloadResponseDto downloadResponse = new FileDownloadResponseDto();
            FileDownloadInfoDto fileDownloadInfoDto = new FileDownloadInfoDto();
            fileDownloadInfoDto.setUrl("https://presigned-url.com");
            downloadResponse.setDownload(fileDownloadInfoDto);
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(downloadResponse));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender).pushToOcr(any(OcrInputPayload.class));
            verify(eventMetaDAO).getDeliveryEventMeta(eq("META##TEST_REQUEST_ID"), eq("META##RECRN002D"));
            verify(eventDematDAO).getDeliveryEventDemat(eq("DEMAT##TEST_REQUEST_ID"), eq("Plico##RECRN002E"));
            verify(deliveryDriverDAO).getByDeliveryDriverId("FULMINE");
            verify(safeStorageClient).getFile("safestorage://uri");

            verifyOcrPayloadCorrectness(OcrInputPayload.DataDto.DocumentType.Plico);
        }

        @Test
        void shouldHandleRECRN002BStatusCodeCorrectly() {
            // Arrange
            paperRequest.setStatusCode("RECRN002C");
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));

            FileDownloadInfoDto fileDownloadInfo = new FileDownloadInfoDto();
            fileDownloadInfo.setUrl("https://presigned-url.com");
            FileDownloadResponseDto fileDownloadResponse = new FileDownloadResponseDto();
            fileDownloadResponse.setDownload(fileDownloadInfo);
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(fileDownloadResponse));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<OcrInputPayload> payloadCaptor = ArgumentCaptor.forClass(OcrInputPayload.class);
            verify(sqsSender).pushToOcr(payloadCaptor.capture());

            OcrInputPayload capturedPayload = payloadCaptor.getValue();
            assertEquals(OcrInputPayload.DataDto.DocumentType.Plico, capturedPayload.getData().getDocumentType());
        }

        private void verifyOcrPayloadCorrectness(OcrInputPayload.DataDto.DocumentType documentType) {
            ArgumentCaptor<OcrInputPayload> payloadCaptor = ArgumentCaptor.forClass(OcrInputPayload.class);
            verify(sqsSender).pushToOcr(payloadCaptor.capture());

            OcrInputPayload capturedPayload = payloadCaptor.getValue();
            assertNotNull(capturedPayload);
            assertEquals(OcrInputPayload.CommandType.postal, capturedPayload.getCommandType());
            assertEquals(documentType, capturedPayload.getData().getDocumentType());
            assertEquals(OcrInputPayload.DataDto.ProductType.AR, capturedPayload.getData().getProductType());
            assertEquals("https://presigned-url.com", capturedPayload.getData().getDetails().getAttachment());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandling {

        @Test
        void shouldSkipOcrWhenDematNotFound() {
            // Arrange
            paperRequest.setStatusCode("RECRN002C");
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.empty());

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender, never()).pushToOcr(any());
        }

        @Test
        void shouldSkipOcrWhenMetaNotFound() {
            // Arrange
            paperRequest.setStatusCode("RECRN002C");
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.empty());
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender, never()).pushToOcr(any());
        }

        @Test
        void shouldSkipOcrWhenOcrDisabled() {
            // Arrange
            when(paperChannelConfig.isEnableOcr()).thenReturn(false);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender, never()).pushToOcr(any());
            verifyNoInteractions(eventMetaDAO, eventDematDAO);
        }

        @Test
        void shouldSkipOcrWhenEntityNotRefined() {
            // Arrange
            entity.setRefined(false);
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(new FileDownloadResponseDto()));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender, never()).pushToOcr(any());
        }

        @Test
        void shouldSkipOcrWhenTimestampsDontMatch() {
            // Arrange
            eventMeta.setStatusDateTime(Instant.now().minusSeconds(10));
            eventDemat.setStatusDateTime(Instant.now().minusSeconds(5));

            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(new FileDownloadResponseDto()));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender, never()).pushToOcr(any());
        }

        @Test
        void shouldHandleSafeStorageErrorGracefully() {
            // Arrange
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));

            FileDownloadResponseDto fileDownloadResponse = new FileDownloadResponseDto();
            fileDownloadResponse.setDownload(null);
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(fileDownloadResponse));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender, never()).pushToOcr(any());
        }

        @Test
        void shouldHandleMissingDownloadUrlInSafeStorageResponse() {
            // Arrange
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));

            FileDownloadInfoDto fileDownloadInfo = new FileDownloadInfoDto();
            fileDownloadInfo.setUrl(null);
            FileDownloadResponseDto fileDownloadResponse = new FileDownloadResponseDto();
            fileDownloadResponse.setDownload(fileDownloadInfo);
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(fileDownloadResponse));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void shouldHandleEntityWithNullRefinedFlag() {
            // Arrange
            entity.setRefined(null);
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(new FileDownloadResponseDto()));

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            verify(messageHandler).handleMessage(entity, paperRequest);
            verify(sqsSender, never()).pushToOcr(any());
        }
    }

    @Nested
    @DisplayName("Status Code Mapping Tests")
    class StatusCodeMappingTests {

        @Test
        void shouldMapRECRN003BToARDocumentType() {
            // Arrange
            paperRequest.setStatusCode("RECRN003C");
            setupSuccessfulOcrFlow();

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<OcrInputPayload> payloadCaptor = ArgumentCaptor.forClass(OcrInputPayload.class);
            verify(sqsSender).pushToOcr(payloadCaptor.capture());

            assertEquals(OcrInputPayload.DataDto.DocumentType.AR, payloadCaptor.getValue().getData().getDocumentType());
        }

        @Test
        void shouldMapRECRN004BToPlicoDocumentType() {
            // Arrange
            paperRequest.setStatusCode("RECRN004C");
            setupSuccessfulOcrFlow();

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<OcrInputPayload> payloadCaptor = ArgumentCaptor.forClass(OcrInputPayload.class);
            verify(sqsSender).pushToOcr(payloadCaptor.capture());

            assertEquals(OcrInputPayload.DataDto.DocumentType.Plico, payloadCaptor.getValue().getData().getDocumentType());
        }

        @Test
        void shouldMapRECRN005BToPlicoDocumentType() {
            // Arrange
            paperRequest.setStatusCode("RECRN005C");
            setupSuccessfulOcrFlow();

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<OcrInputPayload> payloadCaptor = ArgumentCaptor.forClass(OcrInputPayload.class);
            verify(sqsSender).pushToOcr(payloadCaptor.capture());

            assertEquals(OcrInputPayload.DataDto.DocumentType.Plico, payloadCaptor.getValue().getData().getDocumentType());
        }

        @Test
        void shouldMapRECRN002EToPlicoDocumentType() {
            // Arrange
            paperRequest.setStatusCode("RECRN002F"); // This will be mapped to RECRN002E
            setupSuccessfulOcrFlow();

            // Act
            Mono<Void> result = handler.handleMessage(entity, paperRequest);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            ArgumentCaptor<OcrInputPayload> payloadCaptor = ArgumentCaptor.forClass(OcrInputPayload.class);
            verify(sqsSender).pushToOcr(payloadCaptor.capture());

            assertEquals(OcrInputPayload.DataDto.DocumentType.Plico, payloadCaptor.getValue().getData().getDocumentType());
        }

        private void setupSuccessfulOcrFlow() {
            when(paperChannelConfig.isEnableOcr()).thenReturn(true);
            when(messageHandler.handleMessage(any(), any())).thenReturn(Mono.empty());
            when(eventMetaDAO.getDeliveryEventMeta(anyString(), anyString())).thenReturn(Mono.just(eventMeta));
            when(eventDematDAO.getDeliveryEventDemat(anyString(), anyString())).thenReturn(Mono.just(eventDemat));
            when(deliveryDriverDAO.getByDeliveryDriverId(anyString())).thenReturn(Mono.just(deliveryDriver));

            FileDownloadResponseDto downloadResponse = new FileDownloadResponseDto();
            FileDownloadInfoDto fileDownloadInfoDto = new FileDownloadInfoDto();
            fileDownloadInfoDto.setUrl("https://presigned-url.com");
            downloadResponse.setDownload(fileDownloadInfoDto);
            when(safeStorageClient.getFile(anyString())).thenReturn(Mono.just(downloadResponse));
        }
    }
}