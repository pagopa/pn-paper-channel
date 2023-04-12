package it.pagopa.pn.paperchannel.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Slf4j
class Paper_890IT extends BaseTest {

    @Autowired
    private PaperResultAsyncService paperResultAsyncService;

    @MockBean
    private SqsSender sqsSender;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;



    @Test
    @DirtiesContext
    void test_890_DossierClose_RECAG001C(){
        generateEvent("RECAG001C","",null);
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));
    }


    @Test
    @DirtiesContext
    void test_890_DeliverDossierClose_RECAG002C(){
        generateEvent("RECAG002C","",null);
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));
    }

    @Test
    @DirtiesContext
    void test_890_refusedDossierClose_RECAG007C(){
        generateEvent("RECAG007C","",null);
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));
    }

    @Test
    @DirtiesContext
    void test_890_theftOrLoss_RECAG004(){
        //TODO: verificare
        //generateFinalSequence("RECAG004");
    }

    @Test
    @DirtiesContext
    void test_890_deliverStockDossierClose_RECAG005C(){
        generateEvent("RECAG005C","",null);
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));
    }

    @Test
    @DirtiesContext
    void test_890_deliverStockDossierClose_RECAG006C(){
        generateEvent("RECAG006C","",null);
        //verify(sqsSender, timeout(2000).times(1)).pushSendEvent(any(SendEvent.class));

        /* PER DEBUG TEMPORANEO */
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }


    @Test
    @DirtiesContext
    void test_890_NotDeliverDossierClose_RECAG003A_RECAG003C(){
        generateEvent("RECAG003A","M02",null);
        generateEvent("RECAG003C","",null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }


    @Test
    @DirtiesContext
    void test_890_untraceableDossierClose_RECAG003D_RECAG003F(){
        generateEvent("RECAG003D","M01",null);
        generateEvent("RECAG003F","",null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    @DirtiesContext
    void test_890_finishedDossierClose_RECAG012_RECAG011B_RECAG008C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","",null);
        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG008C","",null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    @DirtiesContext
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","",null);
        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG005C","",null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    @DirtiesContext
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","",null);
        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG006C","",null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    @DirtiesContext
    void test_890_rejectedDossierClose_RECAG012_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","",null);
        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG007C","",null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }


    @Test
    @DirtiesContext
    void test_890_finishedDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG008C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG012","",null);
        generateEvent("RECAG011B","",null);

        generateEvent("RECAG008C","",null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    @DirtiesContext
    void test_890_DeliveredDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG012","",null);
        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        generateEvent("RECAG005C","",null);

        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }

    @Test
    @DirtiesContext
    void test_890_DeliveredDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG012","",null);
        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        generateEvent("RECAG006C","",null);

        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    @DirtiesContext
    void test_890_rejectedDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG012","",null);
        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        generateEvent("RECAG007C","",null);

        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    @DirtiesContext
    void test_890_deliverStockDossierClose_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG005C","",null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    @DirtiesContext
    void test_890_deliverStockDossierClose_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG006C","",null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    @DirtiesContext
    void test_890_refusedDossierClose_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","",Arrays.asList("23L","CAD"));

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG007C","",null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        //TODO: verificare KO
        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }


    private void generateEvent(String statusCode, String deliveryFailureCause, List<String> attach){
        // event (final only)
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();

        analogMail.setStatusCode(statusCode);
        analogMail.setProductType("890");

        if(deliveryFailureCause != null && !deliveryFailureCause.trim().equalsIgnoreCase("")){
            analogMail.setDeliveryFailureCause(deliveryFailureCause);
        }

        if(attach != null && attach.size() > 0){
            List<AttachmentDetailsDto> attachments = new LinkedList<>();
            for(String elem: attach){
                attachments.add(
                        new AttachmentDetailsDto()
                                .documentType(elem)
                                .date(OffsetDateTime.now())
                                .url("https://safestorage.it"));
            }
            analogMail.setAttachments(attachments);
        }

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        afterSetForUpdate.setStatusCode(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDetail(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());
    }


}