package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.PcRetryUtils;
import lombok.extern.slf4j.Slf4j;
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

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private ExternalChannelClient mockExtChannel;

    @MockBean
    private AddressDAO mockAddressDAO;

    @Autowired
    private PcRetryUtils pcRetryUtils;

    @Autowired
    private PnPaperChannelConfig pnPaperChannelConfig;


    @Test
    void test_890_DossierClose_RECAG001C(){
        String iun = UUID.randomUUID().toString();
        generateEvent("RECAG001A","","",null,"", null, iun);
        generateEvent("RECAG001B","","",Arrays.asList("23L"),"", null, iun);

        generateEvent("RECAG001C","","",null,"", null, iun);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }


    @Test
    void test_890_DeliverDossierClose_RECAG002C(){
        String iun = UUID.randomUUID().toString();
        generateEvent("RECAG002A","","",null,"", null, iun);
        generateEvent("RECAG002B","","",Arrays.asList("CAN","23L"),"", null, iun);

        generateEvent("RECAG002C","","",null,"", null, iun);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_DeliverDossierClose_RECAG015(){
        String iun = UUID.randomUUID().toString();
        generateEvent("RECAG015","","",null,"", null, iun);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_DeliverDossierClose_RECAG002C_noCan(){
        String iun = UUID.randomUUID().toString();
        generateEvent("RECAG002A","","",null,"", null, iun);
        generateEvent("RECAG002B","","",Arrays.asList("23L"),"", null, iun);

        generateEvent("RECAG002C","","",null,"", null, iun);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_NotDeliverDossierClose_RECAG003A_RECAG003C(){
        String iun = UUID.randomUUID().toString();
        generateEvent("RECAG003A","M02","",null,"", null, iun);
        generateEvent("RECAG003B","","",List.of("Plico"),"", null, iun);
        generateEvent("RECAG003C","","",null,"", null, iun);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        // M02 -> Status OK
        assertEquals("RECAG003C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals("890 - RECAG003C - OK", caturedSendEvent.getValue().getStatusDescription());
        assertEquals(StatusCodeEnum.OK, caturedSendEvent.getValue().getStatusCode());
        assertEquals("M02", caturedSendEvent.getValue().getDeliveryFailureCause());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_OtherReason_RECAG003A_RECAG003C(){
        String iun = UUID.randomUUID().toString();
        generateEvent("RECAG003A","M06","",null,"", null, iun);
        generateEvent("RECAG003B","","",List.of("Plico"),"", null, iun);
        generateEvent("RECAG003C","","",null,"", null, iun);
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(2)).pushSendEvent(caturedSendEvent.capture());

        // M06 -> Status KO
        assertEquals("RECAG003C", caturedSendEvent.getValue().getStatusDetail());
        assertEquals("890 - RECAG003C - KO", caturedSendEvent.getValue().getStatusDescription());
        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
        assertEquals("M06", caturedSendEvent.getValue().getDeliveryFailureCause());
        log.info("Event: \n"+caturedSendEvent.getAllValues());
    }

    @Test
    void test_890_untraceableDossierClose_RECAG003D_RECAG003F(){
        String iun = UUID.randomUUID().toString();
        generateEvent("RECAG003D","M01","discoveredAddress",null,"", null, iun);
        generateEvent("RECAG003E","","",List.of("Plico","Indagine"),"", null, iun);
        generateEvent("RECAG003F","","",null,"", null, iun);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_untraceableDossierClose_RECAG003D_RECAG003F_singleAttachEvent(){
        String iun = UUID.randomUUID().toString();
        generateEvent("RECAG003D","M01","discoveredAddress",null,"", null, iun);

        generateEvent("RECAG003E","","",List.of("Indagine"),"", null, iun);
        generateEvent("RECAG003E","","",List.of("Plico"),"", null, iun);

        generateEvent("RECAG003F","","",null,"", null, iun);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);
        verify(sqsSender, timeout(2000).times(3)).pushSendEvent(caturedSendEvent.capture());
        log.info("Event: \n"+caturedSendEvent.getAllValues());

        assertEquals(StatusCodeEnum.KO, caturedSendEvent.getValue().getStatusCode());
    }

    @Test
    void test_890_theftOrLoss_RECAG004(){
        String iun = UUID.randomUUID().toString();
        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");

        pnPaperChannelConfig.setDisabledRetrySendEngageStatusCodes(List.of());

        when(mockAddressDAO.findAllByRequestId(anyString())).thenReturn(Mono.just(List.of(pnAddress)));
        when(mockExtChannel.sendEngageRequest(any(SendRequest.class), anyList(), any())).thenReturn(Mono.empty());

        generateEvent("RECAG004","F04","",null,"retry", null, iun);

        verify(mockExtChannel, timeout(2000).times(1)).sendEngageRequest(any(SendRequest.class), anyList(), any());
        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
        assertEquals("RECAG004", caturedSendEvent.getValue().getStatusDetail());
    }


    @Test
    void test_890_theftOrLoss_RECAG013(){
        //TODO: guardare meglio
        String iun = UUID.randomUUID().toString();
        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");

        pnPaperChannelConfig.setDisabledRetrySendEngageStatusCodes(List.of());

        when(mockAddressDAO.findAllByRequestId(anyString())).thenReturn(Mono.just(List.of(pnAddress)));
        when(mockExtChannel.sendEngageRequest(any(SendRequest.class), anyList(), any())).thenReturn(Mono.empty());

        generateEvent("RECAG013","","",null,"retry", null, iun);

        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        assertEquals(StatusCodeEnum.PROGRESS, caturedSendEvent.getValue().getStatusCode());
    }

    private void generateEvent(String statusCode, String deliveryFailureCause, String discoveredAddress, List<String> attach, String testType, Instant statusDateTimeToSet, String iun){
        // event (final only)
        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest(iun);

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail(iun);

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
            .concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant()));

        afterSetForUpdate.setStatusCode(extChannelMessage.getAnalogMail().getStatusCode());

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));

        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(afterSetForUpdate));
        when(requestDeliveryDAO.updateConditionalOnFeedbackStatus(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());
    }


}