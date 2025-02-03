package it.pagopa.pn.paperchannel.integrationtests;

import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.TAKING_CHARGE;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.RECRN011_STATUS_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
@ActiveProfiles("test")
class PaperPNRN012Test extends BaseTest.WithOutLocalStackTest {
    private static final String REQUEST_ID = "abc-234-SDSS";
    private static final String PRODUCT_TYPE = "AR";
    private static final String META_STRING = "META##";
    private static final String PNRN012 = "PNRN012";


    @Autowired
    private PaperResultAsyncService paperResultAsyncService;

    @MockBean
    private SqsSender sqsSender;
    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;
    @MockBean
    private EventDematDAO eventDematDAO;
    @MockBean
    private EventMetaDAO eventMetaDAO;
    @MockBean
    private MetaDematCleaner metaDematCleaner;


    @Test
    void Test_AR_StartProcessing__RECRN011(){
        /* BODY OF EXTERNAL CHANNEL QUEUE */
        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(createSimpleAnalogMail("RECRN011", StatusCodeEnum.PROGRESS.getValue(), PRODUCT_TYPE));

        /* ENTITY ALREADY EXISTED INTO DB */
        PnDeliveryRequest entityFromDB = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());

        /* ENTITY FOR UPDATE WITH NEW STATUS ON DB */
        PnDeliveryRequest entityForUpdated = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());

        /* MOCKS OF RESULT ASYNC SERVICE */
        mockResultAsync(entityFromDB, entityForUpdated, extChannelMessage.getAnalogMail());


        Mockito.doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        Mockito.when(eventMetaDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(new PnEventMeta()));

        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

        /*
            Capture PnEventMeta
         */

        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender).pushSendEvent(capturedSendEvent.capture());
        assertNotNull(capturedSendEvent);
        assertNotNull(capturedSendEvent.getValue());
        assertEquals(RECRN011_STATUS_CODE, capturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, capturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void Test_AR_Save_MetaData__RECRN003A() {
        final String RECRN003A_STATUS_CODE = "RECRN003A";
        /* BODY OF EXTERNAL CHANNEL QUEUE */
        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(createSimpleAnalogMail(RECRN003A_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), PRODUCT_TYPE));

        /* ENTITY ALREADY EXISTED INTO DB */
        PnDeliveryRequest entityFromDB = getDeliveryRequest(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), RECRN011_STATUS_CODE.concat(StatusCodeEnum.PROGRESS.getValue()));

        /* ENTITY FOR UPDATE WITH NEW STATUS ON DB */
        PnDeliveryRequest entityForUpdated = getDeliveryRequest(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), RECRN011_STATUS_CODE.concat(StatusCodeEnum.PROGRESS.getValue()));

        /* MOCKS OF RESULT ASYNC SERVICE */
        mockResultAsync(entityFromDB, entityForUpdated, extChannelMessage.getAnalogMail());

        PnEventMeta pnEventMeta = getEventMeta(RECRN003A_STATUS_CODE, Instant.now());

        Mockito.when(eventMetaDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(pnEventMeta));

        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

        ArgumentCaptor<PnEventMeta> capturedEventMetaDAO = ArgumentCaptor.forClass(PnEventMeta.class);
        verify(eventMetaDAO).createOrUpdate(capturedEventMetaDAO.capture());
        assertNotNull(capturedEventMetaDAO.getValue());
        assertEquals(RECRN003A_STATUS_CODE, capturedEventMetaDAO.getValue().getStatusCode());
    }

    @Test
    void Test_AR_SaveDemat__RECRN004B(){
        final String RECRN004B = "RECRN004B";
        final String RECRN004A = "RECRN004A";

        /* BODY OF EXTERNAL CHANNEL QUEUE */
        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        PaperProgressStatusEventDto analog = createSimpleAnalogMail(RECRN004B, StatusCodeEnum.PROGRESS.getValue(), PRODUCT_TYPE);

        analog.setAttachments(List.of(
                new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(OffsetDateTime.now())
                        .uri("https://safestorage.it")));

        extChannelMessage.setAnalogMail(analog);

        /* ENTITY ALREADY EXISTED INTO DB */
        PnDeliveryRequest entityFromDB = getDeliveryRequest(RECRN004A, StatusCodeEnum.PROGRESS.getValue(), RECRN004A.concat(StatusCodeEnum.PROGRESS.getValue()));

        /* ENTITY FOR UPDATE WITH NEW STATUS ON DB */
        PnDeliveryRequest entityForUpdated = getDeliveryRequest(RECRN004A, StatusCodeEnum.PROGRESS.getValue(), RECRN004A.concat(StatusCodeEnum.PROGRESS.getValue()));

        /* MOCKS OF RESULT ASYNC SERVICE */
        mockResultAsync(entityFromDB, entityForUpdated, extChannelMessage.getAnalogMail());

        /* START MOCK SAVE DEMAT HANDLER */
        Mockito.when(eventDematDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(new PnEventDemat()));

        /* END MOCK SAVE DEMAT HANDLER */

        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

        ArgumentCaptor<PnEventDemat> capturedEventMetaDAO = ArgumentCaptor.forClass(PnEventDemat.class);
        verify(eventDematDAO).createOrUpdate(capturedEventMetaDAO.capture());
        assertNotNull(capturedEventMetaDAO.getValue());
        assertNotNull(RECRN004B, capturedEventMetaDAO.getValue().getStatusCode());
    }


    @Test
    void Test_AR_SendPNRN012ToDeliveryPush__RECRN00XC_GreaterEquals10(){
        final String RECRN004C = "RECRN004C";

        /* BODY OF EXTERNAL CHANNEL QUEUE */
        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        PaperProgressStatusEventDto analog = createSimpleAnalogMail(RECRN004C, StatusCodeEnum.OK.getValue(), PRODUCT_TYPE);
        analog.setAttachments(List.of(
                new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(OffsetDateTime.now())
                        .uri("https://safestorage.it")));
        extChannelMessage.setAnalogMail(analog);

        /* ENTITY ALREADY EXISTED INTO DB */
        PnDeliveryRequest entityFromDB = getDeliveryRequest(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), RECRN011_STATUS_CODE.concat(StatusCodeEnum.PROGRESS.getValue()));

        /* ENTITY FOR UPDATE WITH NEW STATUS ON DB */
        PnDeliveryRequest entityForUpdated = getDeliveryRequest(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), RECRN011_STATUS_CODE.concat(StatusCodeEnum.PROGRESS.getValue()));

        /* MOCKS OF RESULT ASYNC SERVICE */
        mockResultAsync(entityFromDB, entityForUpdated, extChannelMessage.getAnalogMail());

        /* ENTITY FOR CALCULATED TIME DIFFERENCE */
        PnEventMeta eventRECRN011 = getEventMeta(RECRN011_STATUS_CODE, Instant.now().minus(20, ChronoUnit.DAYS));
        PnEventMeta eventRECRN004A = getEventMeta("RECRN004A", Instant.now().minus(5, ChronoUnit.DAYS));

        /* START MOCKS OF RECRN00XCMESSAGEHANDLER */
        Mockito.when(eventMetaDAO.getDeliveryEventMeta(eventRECRN011.getMetaRequestId(), eventRECRN011.getMetaStatusCode()))
                .thenReturn(Mono.just(eventRECRN011));

        Mockito.when(eventMetaDAO.getDeliveryEventMeta(eventRECRN004A.getMetaRequestId(), eventRECRN004A.getMetaStatusCode()))
                .thenReturn(Mono.just(eventRECRN004A));

        Mockito.doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        Mockito.when(metaDematCleaner.clean(REQUEST_ID))
                .thenReturn(Mono.just("").then());

        /* END OF MOCKS OF RECRN00XCMESSAGEHANDLER*/
        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender , times(2)).pushSendEvent(caturedSendEvent.capture());
        assertNotNull(caturedSendEvent);
        assertNotNull(caturedSendEvent.getAllValues());
        assertEquals(2, caturedSendEvent.getAllValues().size());

        assertEquals(PNRN012, caturedSendEvent.getAllValues().get(0).getStatusDetail());
        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getAllValues().get(0).getStatusCode());
        assertEquals(RECRN004C, caturedSendEvent.getAllValues().get(1).getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getAllValues().get(1).getStatusCode());
    }

    @Test
    void Test_AR_SendPNRN012ToDeliveryPush__RECRN00XC_Minor10(){
        final String RECRN004C = "RECRN004C";

        /* BODY OF EXTERNAL CHANNEL QUEUE */
        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        PaperProgressStatusEventDto analog = createSimpleAnalogMail(RECRN004C, StatusCodeEnum.OK.getValue(), PRODUCT_TYPE);
        analog.setAttachments(List.of(
                new AttachmentDetailsDto()
                        .documentType("Plico")
                        .date(OffsetDateTime.now())
                        .uri("https://safestorage.it")));
        extChannelMessage.setAnalogMail(analog);

        /* ENTITY ALREADY EXISTED INTO DB */
        PnDeliveryRequest entityFromDB = getDeliveryRequest(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), RECRN011_STATUS_CODE.concat(StatusCodeEnum.PROGRESS.getValue()));

        /* ENTITY FOR UPDATE WITH NEW STATUS ON DB */
        PnDeliveryRequest entityForUpdated = getDeliveryRequest(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), RECRN011_STATUS_CODE.concat(StatusCodeEnum.PROGRESS.getValue()));

        /* MOCKS OF RESULT ASYNC SERVICE */
        mockResultAsync(entityFromDB, entityForUpdated, extChannelMessage.getAnalogMail());

        /* ENTITY FOR CALCULATED TIME DIFFERENCE */
        PnEventMeta eventRECRN011 = getEventMeta(RECRN011_STATUS_CODE, Instant.now().minus(20, ChronoUnit.DAYS));
        PnEventMeta eventRECRN004A = getEventMeta("RECRN004A", Instant.now().minus(15, ChronoUnit.DAYS));

        /* START MOCKS OF RECRN00XCMESSAGEHANDLER */
        Mockito.when(eventMetaDAO.getDeliveryEventMeta(eventRECRN011.getMetaRequestId(), eventRECRN011.getMetaStatusCode()))
                .thenReturn(Mono.just(eventRECRN011));

        Mockito.when(eventMetaDAO.getDeliveryEventMeta(eventRECRN004A.getMetaRequestId(), eventRECRN004A.getMetaStatusCode()))
                .thenReturn(Mono.just(eventRECRN004A));

        Mockito.when(metaDematCleaner.clean(REQUEST_ID))
                .thenReturn(Mono.just("").then());

        /* END OF MOCKS OF RECRN00XCMESSAGEHANDLER*/
        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

        verify(metaDematCleaner, times(1)).clean(REQUEST_ID);
    }


    private void mockResultAsync(PnDeliveryRequest deliveryRequest, PnDeliveryRequest forUpdate, PaperProgressStatusEventDto analogMail) {
        Mockito.when(requestDeliveryDAO.getByRequestId(REQUEST_ID))
                .thenReturn(Mono.just(deliveryRequest));

        RequestDeliveryMapper.changeState(
                forUpdate,
                analogMail.getStatusCode(),
                analogMail.getStatusDescription(),
                ExternalChannelCodeEnum.getStatusCode(analogMail.getStatusCode()),
                forUpdate.getProductType(),
                analogMail.getStatusDateTime().toInstant()
        );

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(forUpdate));
        Mockito.when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(forUpdate));
        Mockito.when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(forUpdate));
    }

    private PnDeliveryRequest getDeliveryRequest(String code, String detail, String description){
        var request = new PnDeliveryRequest();
        request.setRequestId(REQUEST_ID);
        request.setStatusCode(code);
        request.setStatusDetail(detail);
        request.setStatusDescription(description);
        return request;
    }

    private PaperProgressStatusEventDto createSimpleAnalogMail(String statusCode, String statusDetail, String productType) {
        var analogMail = new PaperProgressStatusEventDto();
        analogMail.requestId(REQUEST_ID);
        analogMail.setClientRequestTimeStamp(OffsetDateTime.now());
        analogMail.setStatusDateTime(OffsetDateTime.now());
        analogMail.setStatusCode(statusCode);
        analogMail.setProductType(productType);

        analogMail.setStatusDescription(statusDetail);

        return analogMail;
    }

    private PnEventMeta getEventMeta(String statusCode, Instant time){
        final int ttlOffsetDays = 365;
        final PnDiscoveredAddress address1 = new PnDiscoveredAddress();
        address1.setAddress("discoveredAddress1");
        var eventMeta = new PnEventMeta();
        eventMeta.setMetaRequestId(META_STRING.concat(REQUEST_ID));
        eventMeta.setMetaStatusCode(META_STRING.concat(statusCode));
        eventMeta.setRequestId("1234");
        eventMeta.setStatusCode(statusCode);
        eventMeta.setDiscoveredAddress(address1);
        eventMeta.setDeliveryFailureCause("failureCause1");
        eventMeta.setStatusDateTime(time);
        eventMeta.setTtl(Instant.now().plus(ttlOffsetDays, ChronoUnit.DAYS).toEpochMilli());
        return eventMeta;
    }

}
