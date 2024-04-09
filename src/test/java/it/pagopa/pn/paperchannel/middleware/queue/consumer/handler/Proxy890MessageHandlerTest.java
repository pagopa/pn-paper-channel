package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.FlowTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class Proxy890MessageHandlerTest {

    private RECAG008CMessageHandler mockRECAG008CMessageHandler;
    private Complex890MessageHandler mockComplex890MessageHandler;

    private Simple890MessageHandler simple890MessageHandler;
    private Proxy890MessageHandler handler;

    private PnEventErrorDAO pnEventErrorDAO;
    private PnPaperChannelConfig pnPaperChannelConfig;

    @BeforeEach
    public void init() {

        mockRECAG008CMessageHandler = mock(RECAG008CMessageHandler.class);
        mockComplex890MessageHandler = mock(Complex890MessageHandler.class);
        simple890MessageHandler = mock(Simple890MessageHandler.class);

        pnEventErrorDAO = mock(PnEventErrorDAO.class);

        pnPaperChannelConfig = mock(PnPaperChannelConfig.class);

        handler = Proxy890MessageHandler.builder()
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .recag008CMessageHandler(mockRECAG008CMessageHandler)
                .complex890MessageHandler(mockComplex890MessageHandler)
                .simple890MessageHandler(simple890MessageHandler)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();
    }

    @Test
    void handleMessageComplex890_noRefined_enableSimpleFalse_containsTrue() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG005C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode(statusCode)
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);


        when(mockComplex890MessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());
        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(false);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of(statusCode));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());
        
        verify(mockComplex890MessageHandler, times(1)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);

    }

    @Test
    void handleMessageComplex890_noRefined_enableSimpleFalse_containsFalse() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG005C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode(statusCode)
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);


        when(mockComplex890MessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());
        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(false);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of("RECAG005A"));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockComplex890MessageHandler, times(1)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);

    }

    @Test
    void handleMessageComplex890_noRefined_enableSimpleTrue_containsTrue() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG005C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode(statusCode)
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);


        when(mockComplex890MessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());
        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(true);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of(statusCode));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockComplex890MessageHandler, times(1)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);
    }


    @Test
    void handleSimple890_refinedTrue() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode("RECAG005C")
                .statusDateTime(instant)
                .clientRequestTimeStamp(instant);

        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(true);


        when(simple890MessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());
        when(mockComplex890MessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockComplex890MessageHandler, times(0)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(1)).handleMessage(entity,paperRequest);

    }


    @Test
    void handleError_noRefined_enableSimpleTrue_containsFalse() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG005C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
                .requestId("requestId")
                .statusCode(statusCode)
                .statusDateTime(instant)
                .iun("122324")
                .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());

        when(pnEventErrorDAO.putItem(any())).thenReturn(Mono.empty());

        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(true);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of("RECAG005B"));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockComplex890MessageHandler, times(0)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);

        verify(pnEventErrorDAO,times(1)).putItem(argThat(pnEventError -> {
            assertThat(pnEventError.getRequestId()).isEqualTo(entity.getRequestId());
            assertThat(pnEventError.getFlowType()).isEqualTo(FlowTypeEnum.COMPLEX_890.name());

            return true;
        }));
    }

    @Test
    void handleError_falseRefined_enableSimpleTrue_containsFalse() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG005C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
            .requestId("requestId")
            .statusCode(statusCode)
            .statusDateTime(instant)
            .iun("122324")
            .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);

        when(pnEventErrorDAO.putItem(any())).thenReturn(Mono.empty());

        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(true);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of("RECAG005B"));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockComplex890MessageHandler, times(0)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);

        verify(pnEventErrorDAO,times(1)).putItem(argThat(pnEventError -> {
            assertThat(pnEventError.getRequestId()).isEqualTo(entity.getRequestId());
            assertThat(pnEventError.getFlowType()).isEqualTo(FlowTypeEnum.SIMPLE_890.name());

            return true;
        }));
    }

    @Test
    void handleMessageRecag008C_noRefined_enableSimpleFalse_containsTrue() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG008C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
            .requestId("requestId")
            .statusCode(statusCode)
            .statusDateTime(instant)
            .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);


        when(mockRECAG008CMessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());
        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(false);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of(statusCode));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockRECAG008CMessageHandler, times(1)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);
        verify(mockComplex890MessageHandler,times(0)).handleMessage(entity,paperRequest);

    }

    @Test
    void handleMessageRecag008C_noRefined_enableSimpleFalse_containsFalse() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG008C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
            .requestId("requestId")
            .statusCode(statusCode)
            .statusDateTime(instant)
            .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);


        when(mockRECAG008CMessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());
        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(false);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of("RECAG005C"));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockRECAG008CMessageHandler, times(1)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);
        verify(mockComplex890MessageHandler,times(0)).handleMessage(entity,paperRequest);

    }

    @Test
    void handleMessageRecag008C_noRefined_enableSimpleTrue_containsTrue() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG008C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
            .requestId("requestId")
            .statusCode(statusCode)
            .statusDateTime(instant)
            .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);


        when(mockRECAG008CMessageHandler.handleMessage(entity, paperRequest)).thenReturn(Mono.empty());
        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(true);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of(statusCode));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockRECAG008CMessageHandler, times(1)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);
        verify(mockComplex890MessageHandler,times(0)).handleMessage(entity,paperRequest);
    }

    @Test
    void handleErrorRecag008C_falseRefined_enableSimpleTrue_containsFalse() {
        OffsetDateTime instant = OffsetDateTime.parse("2023-03-09T14:44:00.000Z");
        String statusCode = "RECAG008C";

        PaperProgressStatusEventDto paperRequest = new PaperProgressStatusEventDto()
            .requestId("requestId")
            .statusCode(statusCode)
            .statusDateTime(instant)
            .iun("122324")
            .clientRequestTimeStamp(instant);


        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId("requestId");
        entity.setStatusCode("statusDetail");
        entity.setStatusDetail(StatusCodeEnum.OK.getValue());
        entity.setRefined(false);

        when(pnEventErrorDAO.putItem(any())).thenReturn(Mono.empty());

        when(pnPaperChannelConfig.isEnableSimple890Flow()).thenReturn(true);
        when(pnPaperChannelConfig.getComplexRefinementCodes()).thenReturn(Set.of("RECAG005B"));

        assertDoesNotThrow(() -> handler.handleMessage(entity, paperRequest).block());

        verify(mockRECAG008CMessageHandler, times(0)).handleMessage(entity, paperRequest);
        verify(mockComplex890MessageHandler, times(0)).handleMessage(entity, paperRequest);
        verify(simple890MessageHandler,times(0)).handleMessage(entity,paperRequest);

        verify(pnEventErrorDAO,times(1)).putItem(argThat(pnEventError -> {
            assertThat(pnEventError.getRequestId()).isEqualTo(entity.getRequestId());
            assertThat(pnEventError.getFlowType()).isEqualTo(FlowTypeEnum.SIMPLE_890.name());

            return true;
        }));
    }
}
