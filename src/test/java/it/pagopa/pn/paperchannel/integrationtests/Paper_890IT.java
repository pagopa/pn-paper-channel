package it.pagopa.pn.paperchannel.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RetryableErrorMessageHandler;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.*;

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

    @MockBean
    private ExternalChannelClient mockExtChannel;

    @MockBean
    private AddressDAO mockAddressDAO;



    @Test
    void test_890_DossierClose_RECAG001C(){
        generateEvent("RECAG001A","","",null,"");
        generateEvent("RECAG001B","","",Arrays.asList("23L"),"");

        generateEvent("RECAG001C","","",null,"");
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }


    @Test
    void test_890_DeliverDossierClose_RECAG002C(){
        generateEvent("RECAG002A","","",null,"");
        generateEvent("RECAG002B","","",Arrays.asList("CAN","23L"),"");

        generateEvent("RECAG002C","","",null,"");
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_DeliverDossierClose_RECAG002C_noCan(){
        generateEvent("RECAG002A","","",null,"");
        generateEvent("RECAG002B","","",Arrays.asList("23L"),"");

        generateEvent("RECAG002C","","",null,"");
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_NotDeliverDossierClose_RECAG003A_RECAG003C(){
        generateEvent("RECAG003A","M02","",null,"");
        generateEvent("RECAG003B","","",List.of("Plico"),"");
        generateEvent("RECAG003C","","",null,"");

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_untraceableDossierClose_RECAG003D_RECAG003F(){
        generateEvent("RECAG003D","M01","discoveredAddress",null,"");
        generateEvent("RECAG003E","","",List.of("Plico","Indagine"),"");
        //Da ritestare con il RECAG003E ripetuto con un attach solo per volta (vale per tutti quelli con più attach)
        generateEvent("RECAG003F","","",null,"");

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_untraceableDossierClose_RECAG003D_RECAG003F_singleAttachEvent(){
        generateEvent("RECAG003D","M01","discoveredAddress",null,"");

        generateEvent("RECAG003E","","",List.of("Indagine"),"");
        generateEvent("RECAG003E","","",List.of("Plico"),"");

        generateEvent("RECAG003F","","",null,"");

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_theftOrLoss_RECAG004(){
        //TODO: guardare meglio
        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");


        when(mockAddressDAO.findAllByRequestId(anyString())).thenReturn(Mono.just(List.of(pnAddress)));
        when(mockExtChannel.sendEngageRequest(any(SendRequest.class), anyList())).thenReturn(Mono.empty());

        generateEvent("RECAG004","","",null,"retry");

        verify(mockExtChannel, timeout(2000).times(1)).sendEngageRequest(any(SendRequest.class), anyList());
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
        //TODO: assertEquals("RECAG004", caturedSendEvent.getValue().getStatusDetail());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG005C_no11B(){

        generateEvent("RECAG005A","","",null,"");
        generateEvent("RECAG005B","","",List.of("ARCAD","23L"),"");

        generateEvent("RECAG005C","","",null,"");
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG005C(){
        generateEvent("RECAG011B","","", List.of("ARCAD"),"");

        generateEvent("RECAG005A","","",null,"");
        generateEvent("RECAG005B","","",List.of("23L"),"");

        generateEvent("RECAG005C","","",null,"");
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG005C_BIS(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", List.of("ARCAD"),"");
        generateEvent("RECAG012","","", null,"");

        generateEvent("RECAG005A","","",null,"");

        generateEvent("RECAG005B","","",List.of("23L"),"");

        generateEvent("RECAG005C","","",null,"");

        //verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
//        log.info("Event: \n"+caturedSendEvent.getAllValues());
//        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());
//        assertEquals(StatusCodeEnum.OK,caturedSendEvent.getValue().getStatusCode());

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

//        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
//        assertEquals("RECAG005B", caturedSendEvent.getValue().getStatusDetail());
        //TODO: bug
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG005C_TRIS(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", List.of("ARCAD"),"");
        generateEvent("RECAG012","","", null,"");
        generateEvent("RECAG011B","","", List.of("23L"),"");

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.OK,caturedSendEvent.getValue().getStatusCode());

        generateEvent("RECAG005A","","",null,"");
        generateEvent("RECAG005C","","",null,"");


        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

//        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
//        assertEquals("RECAG005B", caturedSendEvent.getValue().getStatusDetail());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_refusedDossierClose_RECAG007C(){

        generateEvent("RECAG011B","","", List.of("ARCAD"),"");

        //DA RITESTARE SENZA 11B con RECAG007B con ARCAD/CAD
        generateEvent("RECAG007A","","",null,"");
        generateEvent("RECAG007B","","", List.of("Plico"),"");
        generateEvent("RECAG007C","","",null,"");
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_refusedDossierClose_RECAG007C_no11B(){

        generateEvent("RECAG007A","","",null,"");
        generateEvent("RECAG007B","","", List.of("Plico","ARCAD"),"");
        generateEvent("RECAG007C","","",null,"");
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }




    @Test
    void test_890_deliverStockDossierClose_RECAG006C(){
        generateEvent("RECAG011B","","", List.of("ARCAD"),"");
        //DA RITESTARE SENZA 11B con RECAG006B con ARCAD/CAD
        generateEvent("RECAG006A","","",null,"");
        generateEvent("RECAG006B","","", List.of("23L"),"");
        generateEvent("RECAG006C","","",null,"");

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG006C_no11B(){
        generateEvent("RECAG006A","","",null,"");
        generateEvent("RECAG006B","","", List.of("ARCAD","23L"),"");
        generateEvent("RECAG006C","","",null,"");

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }


    
    @Test
    void test_890_finishedDossierClose_RECAG012_RECAG011B_RECAG008C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG008C","","",null,"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG008C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_finishedDossierClose_RECAG012_RECAG011B_RECAG008C_singleAttachEvent(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","", List.of("23L"),"");
        generateEvent("RECAG011B","","", List.of("CAD"),"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG008C","","",null,"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG008C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG005C","","",null,"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG005C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG005C_singleAttachEvent(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","", List.of("CAD"),"");
        generateEvent("RECAG011B","","", List.of("23L"),"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG005C","","",null,"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG005C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG006C","","",null,"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG006C_singleAttachEvent(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","", List.of("CAD"),"");
        generateEvent("RECAG011B","","", List.of("23L"),"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG006C","","",null,"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_rejectedDossierClose_RECAG012_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG007C","","",null,"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }

    @Test
    void test_890_rejectedDossierClose_RECAG012_RECAG011B_RECAG007C_singleAttachEvent(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","", List.of("23L"),"");
        generateEvent("RECAG011B","","", List.of("CAD"),"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG007C","","",null,"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }


    @Test
    void test_890_finishedDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG008C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG008C","","",null,"");

        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG008C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_DeliveredDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG005C","","",null,"");

        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }

    @Test
    void test_890_DeliveredDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG006C","","",null,"");

        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG006C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_rejectedDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        generateEvent("RECAG012","","",null,"");
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG007C","","",null,"");

        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_deliverStockDossierClose_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG005C","","",null,"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG006C","","",null,"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_refusedDossierClose_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"");

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG007C","","",null,"");

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }


    private void generateEvent(String statusCode, String deliveryFailureCause, String discoveredAddress, List<String> attach, String testType){
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

        if (discoveredAddress != null && !discoveredAddress.trim().equalsIgnoreCase("")) {
            PnDiscoveredAddress address = new PnDiscoveredAddress();
            address.setAddress(discoveredAddress);

            DiscoveredAddressDto discoveredAddressDto =
                    new BaseMapperImpl<>(PnDiscoveredAddress.class, DiscoveredAddressDto.class)
                            .toDTO(address);

            analogMail.setDiscoveredAddress(discoveredAddressDto);
        }



        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();

        if(testType != null && testType.equalsIgnoreCase("retry")){
            afterSetForUpdate.setProductType("_890");
            var attachment = new PnAttachmentInfo();
            attachment.setDocumentType("Plico");
            attachment.setDate(OffsetDateTime.now().toString());
            attachment.setUrl("https://safestorage.it");
            afterSetForUpdate.setAttachments(List.of(attachment));
        }

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
