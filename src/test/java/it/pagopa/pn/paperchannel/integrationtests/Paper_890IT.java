package it.pagopa.pn.paperchannel.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

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
        generateEvent("RECAG001A","","",null,"", null);
        generateEvent("RECAG001B","","",Arrays.asList("23L"),"", null);

        generateEvent("RECAG001C","","",null,"", null);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }


    @Test
    void test_890_DeliverDossierClose_RECAG002C(){
        generateEvent("RECAG002A","","",null,"", null);
        generateEvent("RECAG002B","","",Arrays.asList("CAN","23L"),"", null);

        generateEvent("RECAG002C","","",null,"", null);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_DeliverDossierClose_RECAG015(){
        generateEvent("RECAG015","","",null,"", null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_DeliverDossierClose_RECAG002C_noCan(){
        generateEvent("RECAG002A","","",null,"", null);
        generateEvent("RECAG002B","","",Arrays.asList("23L"),"", null);

        generateEvent("RECAG002C","","",null,"", null);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_NotDeliverDossierClose_RECAG003A_RECAG003C(){
        generateEvent("RECAG003A","M02","",null,"", null);
        generateEvent("RECAG003B","","",List.of("Plico"),"", null);
        generateEvent("RECAG003C","","",null,"", null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
        assertEquals("M02", caturedSendEvent.getValue().getDeliveryFailureCause());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_untraceableDossierClose_RECAG003D_RECAG003F(){
        generateEvent("RECAG003D","M01","discoveredAddress",null,"", null);
        generateEvent("RECAG003E","","",List.of("Plico","Indagine"),"", null);
        generateEvent("RECAG003F","","",null,"", null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_untraceableDossierClose_RECAG003D_RECAG003F_singleAttachEvent(){
        generateEvent("RECAG003D","M01","discoveredAddress",null,"", null);

        generateEvent("RECAG003E","","",List.of("Indagine"),"", null);
        generateEvent("RECAG003E","","",List.of("Plico"),"", null);

        generateEvent("RECAG003F","","",null,"", null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_theftOrLoss_RECAG004(){

        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");


        when(mockAddressDAO.findAllByRequestId(anyString())).thenReturn(Mono.just(List.of(pnAddress)));
        when(mockExtChannel.sendEngageRequest(any(SendRequest.class), anyList())).thenReturn(Mono.empty());

        generateEvent("RECAG004","F04","",null,"retry", null);

        verify(mockExtChannel, timeout(2000).times(1)).sendEngageRequest(any(SendRequest.class), anyList());
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
        assertEquals("RECAG004", caturedSendEvent.getValue().getStatusDetail());
    }


    @Test
    void test_890_theftOrLoss_RECAG013(){
        //TODO: guardare meglio
        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");

        when(mockAddressDAO.findAllByRequestId(anyString())).thenReturn(Mono.just(List.of(pnAddress)));
        when(mockExtChannel.sendEngageRequest(any(SendRequest.class), anyList())).thenReturn(Mono.empty());

        generateEvent("RECAG013","","",null,"retry", null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    @Disabled
    void test_890_deliverStockDossierClose_RECAG005C_no11B(){

        generateEvent("RECAG005A","","",null,"", null);
        generateEvent("RECAG005B","","",List.of("ARCAD","23L"),"", null);
        generateEvent("RECAG005C","","",null,"", null);


        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());

    }

    @Test
    void test_890_deliverStockDossierClose_RECAG005C(){
        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);

        generateEvent("RECAG005A","","",null,"", null);
        generateEvent("RECAG005B","","",List.of("23L"),"", null);

        generateEvent("RECAG005C","","",null,"", null);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG005C_BIS(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011A","","",null,"", Instant.parse("2023-03-05T17:07:00.000Z"));

        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);
        generateEvent("RECAG012","","", null,"", null);

        generateEvent("RECAG005A","","",null,"", Instant.parse("2023-03-16T17:07:00.000Z"));

        generateEvent("RECAG005B","","",List.of("23L"),"", null);

        generateEvent("RECAG005C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        List<SendEvent> allValues = caturedSendEvent.getAllValues();
        log.info("Event: \n"+ allValues);

        assertEquals("RECAG005B", allValues.get(allValues.size()-3).getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS,allValues.get(allValues.size()-3).getStatusCode());

        assertEquals("PNAG012", allValues.get(allValues.size()-2).getStatusDetail());
        assertEquals(StatusCodeEnum.OK,allValues.get(allValues.size()-2).getStatusCode());

        assertEquals("RECAG005C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }
    

    @Test
    @Disabled
    void test_890_deliverStockDossierClose_RECAG005C_TRIS(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);
        generateEvent("RECAG012","","", null,"", null);
        generateEvent("RECAG011B","","", List.of("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.OK,caturedSendEvent.getValue().getStatusCode());

        generateEvent("RECAG005A","","",null,"", null);
        generateEvent("RECAG005C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
        assertEquals("RECAG005C", caturedSendEvent.getValue().getStatusDetail());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

    }

    @Test
    void test_890_deliverStockDossierClose_RECAG006C_no11B(){
        generateEvent("RECAG006A","","",null,"", null);
        generateEvent("RECAG006B","","", List.of("ARCAD","23L"),"", null);
        generateEvent("RECAG006C","","",null,"", null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }


    @Test
    void test_890_deliverStockDossierClose_RECAG006C(){
        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);
        //DA RITESTARE SENZA 11B con RECAG006B con ARCAD/CAD
        generateEvent("RECAG006A","","",null,"", null);
        generateEvent("RECAG006B","","", List.of("23L"),"", null);
        generateEvent("RECAG006C","","",null,"", null);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG006C_BIS(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011A","","",null,"", Instant.parse("2023-03-05T17:07:00.000Z"));

        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);
        generateEvent("RECAG012","","", null,"", null);

        generateEvent("RECAG006A","","",null,"", Instant.parse("2023-03-16T17:07:00.000Z"));

        generateEvent("RECAG006B","","",List.of("23L"),"", null);

        generateEvent("RECAG006C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        List<SendEvent> allValues = caturedSendEvent.getAllValues();
        log.info("Event: \n"+ allValues);

        assertEquals("RECAG006B", allValues.get(allValues.size()-3).getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS,allValues.get(allValues.size()-3).getStatusCode());

        assertEquals("PNAG012", allValues.get(allValues.size()-2).getStatusDetail());
        assertEquals(StatusCodeEnum.OK,allValues.get(allValues.size()-2).getStatusCode());

        assertEquals("RECAG006C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG006C_TRIS(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);
        generateEvent("RECAG012","","", null,"", null);
        generateEvent("RECAG011B","","", List.of("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.OK,caturedSendEvent.getValue().getStatusCode());

        generateEvent("RECAG006A","","",null,"", null);
        generateEvent("RECAG006C","","",null,"", null);


        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
        assertEquals("RECAG006C", caturedSendEvent.getValue().getStatusDetail());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

    }

    @Test
    void test_890_refusedDossierClose_RECAG007C_no11B(){

        generateEvent("RECAG007A","","",null,"", null);
        generateEvent("RECAG007B","","", List.of("Plico","ARCAD"),"", null);
        generateEvent("RECAG007C","","",null,"", null);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }
    @Test
    void test_890_refusedDossierClose_RECAG007C(){
        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);

        generateEvent("RECAG007A","","",null,"", null);
        generateEvent("RECAG007B","","", List.of("Plico"),"", null);
        generateEvent("RECAG007C","","",null,"", null);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }
    @Test
    void test_890_deliverStockDossierClose_RECAG007C_BIS(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011A","","",null,"", Instant.parse("2023-03-05T17:07:00.000Z"));

        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);
        generateEvent("RECAG012","","", null,"", null);

        generateEvent("RECAG007A","","",null,"", Instant.parse("2023-03-16T17:07:00.000Z"));
        generateEvent("RECAG007B","","",List.of("23L"),"", null);
        generateEvent("RECAG007C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        List<SendEvent> allValues = caturedSendEvent.getAllValues();
        log.info("Event: \n"+ allValues);

        assertEquals("PNAG012", allValues.get(allValues.size()-2).getStatusDetail());
        assertEquals(StatusCodeEnum.OK,allValues.get(allValues.size()-2).getStatusCode());

        assertEquals("RECAG007C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

        log.info("Event: \n"+ allValues);
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG007C_TRIS(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);
        generateEvent("RECAG012","","", null,"", null);
        generateEvent("RECAG011B","","", List.of("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.OK,caturedSendEvent.getValue().getStatusCode());

        generateEvent("RECAG007A","","",null,"", null);
        generateEvent("RECAG007C","","",null,"", null);


        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
        assertEquals("RECAG007C", caturedSendEvent.getValue().getStatusDetail());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

    }



    @Test
    void test_890_finishedDossierClose_RECAG012_RECAG011B_RECAG008C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG008C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG008C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_finishedDossierClose_RECAG011B_RECAG012_RECAG008B_RECAG008C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", List.of("ARCAD"),"", null);
        generateEvent("RECAG012","","",null,"", null);

        generateEvent("RECAG011B","","",List.of("23L"),"", null);
        generateEvent("REAG008A","","", null,"", null);
        generateEvent("RECAG008B","","", List.of("Plico"),"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        List<SendEvent> allValues = caturedSendEvent.getAllValues();
        log.info("Event: \n"+allValues);

        assertEquals("PNAG012", allValues.get(allValues.size()-2).getStatusDetail());
        assertEquals(StatusCodeEnum.OK,allValues.get(allValues.size()-2).getStatusCode());

        generateEvent("RECAG008C","","", null,"", null);

        verify(sqsSender, timeout(2000).times(4)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG008C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_finishedDossierClose_RECAG012_RECAG011B_RECAG008C_singleAttachEvent(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","", List.of("23L"),"", null);
        generateEvent("RECAG011B","","", List.of("CAD"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG008C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG008C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }


    @Test
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","", Arrays.asList("23L","CAD"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG005C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG005C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG005C_singleAttachEvent(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","", List.of("CAD"),"", null);
        generateEvent("RECAG011B","","", List.of("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG005C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG005C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG006C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_DeliveredDossierClose_RECAG012_RECAG011B_RECAG006C_singleAttachEvent(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","", List.of("CAD"),"", null);
        generateEvent("RECAG011B","","", List.of("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG006C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_rejectedDossierClose_RECAG012_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG007C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }

    @Test
    void test_890_rejectedDossierClose_RECAG012_RECAG011B_RECAG007C_singleAttachEvent(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","", List.of("23L"),"", null);
        generateEvent("RECAG011B","","", List.of("CAD"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG007C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }


    @Test
    void test_890_finishedDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG008C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", List.of("CAD"),"", null);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","", List.of("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG008C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG008C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_DeliveredDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("CAD"),"", null);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","",Arrays.asList("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG005C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());

    }

    @Test
    void test_890_DeliveredDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("CAD"),"", null);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","",Arrays.asList("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG006C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals("RECAG006C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_rejectedDossierClose_RECAG011B_RECAG012_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("CAD"),"", null);

        generateEvent("RECAG012","","",null,"", null);
        generateEvent("RECAG011B","","",Arrays.asList("23L"),"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
        assertEquals("PNAG012", caturedSendEvent.getValue().getStatusDetail());

        generateEvent("RECAG007C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_deliverStockDossierClose_RECAG011B_RECAG005C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", Arrays.asList("23L","CAD"),"", null);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG005C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_deliverStockDossierClose_RECAG011B_RECAG006C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","", Arrays.asList("23L","CAD"),"", null);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG006C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
    }


    @Test
    void test_890_refusedDossierClose_RECAG011B_RECAG007C(){
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        generateEvent("RECAG011B","","",Arrays.asList("23L","CAD"),"", null);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        generateEvent("RECAG007C","","",null,"", null);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }


    private void generateEvent(String statusCode, String deliveryFailureCause, String discoveredAddress, List<String> attach, String testType, Instant statusDateTimeToSet){
        // event (final only)
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();

        analogMail.setStatusCode(statusCode);
        analogMail.setProductType("890");

        if (statusDateTimeToSet != null) {
            analogMail.setStatusDateTime(OffsetDateTime.ofInstant(statusDateTimeToSet, ZoneOffset.UTC));
        }

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
                                .uri("https://safestorage.it"));
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

        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDescription(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));

        afterSetForUpdate.setStatusCode(extChannelMessage.getAnalogMail().getStatusCode());

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());
    }


}
