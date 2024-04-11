package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.encryption.impl.DataVaultEncryptionImpl;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.service.impl.PaperResultAsyncServiceImpl;
import lombok.extern.slf4j.Slf4j;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest(properties = {
    "pn.paper-channel.enable-simple-890-flow=false",
    "pn.paper-channel.complex-refinement-codes=RECAG007C"
})
class PaperComplexStock890IT extends BaseTest {

    private static final String IUN = "NEQP-YAZD-XNGK-202312-L-1";
    private static final String REQUEST_ID = "PREPARE_ANALOG_DOMICILE.IUN_" + IUN + ".RECINDEX_0.SENTATTEMPTMADE_1";

    @Autowired
    private PaperResultAsyncServiceImpl paperResultAsyncService;

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private SqsSender sqsSender;

    @MockBean
    private DataVaultEncryptionImpl dataVaultEncryption;

    @BeforeEach
    public void setUp() {
        buildAndCreateDeliveryRequest();
    }

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
        generateEvent(RECAG011A);
        generateEvent(RECAG005A);
        generateEvent(RECAG005B);
        generateEvent(RECAG005C);

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

    private void generateEvent(SingleStatusUpdateDto singleStatusUpdateDto){

        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto, 0).block());
    }

    private void buildAndCreateDeliveryRequest() {
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequestWithRequestId(REQUEST_ID, IUN);
        requestDeliveryDAO.createWithAddress(pnDeliveryRequest, null, null).block();
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
