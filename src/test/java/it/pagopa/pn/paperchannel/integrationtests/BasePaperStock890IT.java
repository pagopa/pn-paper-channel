package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.encryption.impl.DataVaultEncryptionImpl;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

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
public abstract class BasePaperStock890IT extends BaseTest {

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

    protected void generateEvent(SingleStatusUpdateDto singleStatusUpdateDto, Class<? extends Exception> exception){

        Mockito.when(dataVaultEncryption.encode(Mockito.any(), Mockito.any())).thenReturn("returnOk");
        Mockito.when(dataVaultEncryption.decode(Mockito.any())).thenReturn("returnOk");

        if (exception != null) {
            assertThrows(exception, () -> paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto, 0).block());
        } else {
            assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto, 0).block());
        }
    }

    protected SingleStatusUpdateDto buildStatusUpdateDto(String statusCode, List<String> attach, Instant statusDateTimeToSet) {
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

    protected void checkFlowCorrectness(Map<String, StatusCodeEnum> assertionLookupTable, Boolean isExpectedRefined) {
        ArgumentCaptor<SendEvent> capturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        PnDeliveryRequest deliveryRequest = requestDeliveryDAO.getByRequestId(REQUEST_ID).block();

        /* Expected events to delivery push */
        verify(sqsSender, timeout(2000)
            .times(assertionLookupTable.size()))
            .pushSendEvent(capturedSendEvent.capture());

        /* Verify all events are sent with right status code */
        capturedSendEvent.getAllValues().forEach(sendEvent -> {
            StatusCodeEnum expectedStatusCode = assertionLookupTable.get(sendEvent.getStatusDetail());
            assertEquals(expectedStatusCode, sendEvent.getStatusCode());
        });

        /* Delivery request expected to be refined */
        assertNotNull(deliveryRequest);
        assertEquals(isExpectedRefined, deliveryRequest.getRefined());
    }
}
