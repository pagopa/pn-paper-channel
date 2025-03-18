package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.api.PaperMessagesApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperEngageRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.Const.PN_AAR;

@ExtendWith(MockitoExtension.class)
class ExternalChannelClientImplTest {

    @InjectMocks
    ExternalChannelClientImpl externalChannelClient;

    @Mock
    private PnPaperChannelConfig pnPaperChannelConfig;
    @Mock
    private PaperMessagesApi paperMessagesApi;


    private final SendRequest sendRequest = new SendRequest();
    private final List<String> attachmentUrls = new ArrayList<>();
    private final AnalogAddress analogAddress = new AnalogAddress();

    @BeforeEach
    public void init(){
        inizialize();
    }
    @Test
    void sendEngageRequest() {
        AttachmentInfo attachmentInfo = new AttachmentInfo();
        attachmentInfo.setDocumentType(PN_AAR);
        attachmentInfo.setFileKey("safestorage://PN_AAR-0002-GR7Z-3UBM-81QT-1QWV?docTag=AAR");
        attachmentInfo.setSha256("234567890");

        ArgumentCaptor<String> caturedSRequestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> caturedSCxId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaperEngageRequestDto> caturedSPaperEngageRequestDto = ArgumentCaptor.forClass(PaperEngageRequestDto.class);

        Mockito.when(pnPaperChannelConfig.getRequestPaIdOverride()).thenReturn("");
        Mockito.when(pnPaperChannelConfig.getXPagopaExtchCxId()).thenReturn("cxid");
        Mockito.when(paperMessagesApi.sendPaperEngageRequest(caturedSRequestId.capture(), caturedSCxId.capture(), caturedSPaperEngageRequestDto.capture())).thenReturn(Mono.empty());

        externalChannelClient.sendEngageRequest(sendRequest, List.of(attachmentInfo), null).block();

        PaperEngageRequestDto dto = caturedSPaperEngageRequestDto.getValue();
        String cxid = caturedSCxId.getValue();
        String reqId = caturedSRequestId.getValue();

        Assertions.assertEquals("safestorage://PN_AAR-0002-GR7Z-3UBM-81QT-1QWV", dto.getAttachments().get(0).getUri());
        Assertions.assertEquals("cxid", cxid);
        Assertions.assertEquals(sendRequest.getRequestId(), reqId);
        Assertions.assertNull(dto.getApplyRasterization());
    }

    void sendEngageRequestApplyRasterizatonTrue() {
        AttachmentInfo attachmentInfo = new AttachmentInfo();
        attachmentInfo.setDocumentType(PN_AAR);
        attachmentInfo.setFileKey("safestorage://PN_AAR-0002-GR7Z-3UBM-81QT-1QWV?docTag=AAR");
        attachmentInfo.setSha256("234567890");

        ArgumentCaptor<String> caturedSRequestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> caturedSCxId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PaperEngageRequestDto> caturedSPaperEngageRequestDto = ArgumentCaptor.forClass(PaperEngageRequestDto.class);

        Mockito.when(pnPaperChannelConfig.getRequestPaIdOverride()).thenReturn("");
        Mockito.when(pnPaperChannelConfig.getXPagopaExtchCxId()).thenReturn("cxid");
        Mockito.when(paperMessagesApi.sendPaperEngageRequest(caturedSRequestId.capture(), caturedSCxId.capture(), caturedSPaperEngageRequestDto.capture())).thenReturn(Mono.empty());

        externalChannelClient.sendEngageRequest(sendRequest, List.of(attachmentInfo), Boolean.TRUE).block();

        PaperEngageRequestDto dto = caturedSPaperEngageRequestDto.getValue();
        String cxid = caturedSCxId.getValue();
        String reqId = caturedSRequestId.getValue();

        Assertions.assertEquals("safestorage://PN_AAR-0002-GR7Z-3UBM-81QT-1QWV", dto.getAttachments().get(0).getUri());
        Assertions.assertEquals("cxid", cxid);
        Assertions.assertEquals(sendRequest.getRequestId(), reqId);
        Assertions.assertNotNull(dto.getApplyRasterization());
        Assertions.assertTrue(dto.getApplyRasterization());

    }



    private void inizialize(){
        attachmentUrls.add("safestorage://PN_AAR-0002-GR7Z-3UBM-81QT-1QWV?docTag=AAR");

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");
        analogAddress.setNameRow2("Ettore");

        sendRequest.setRequestId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        sendRequest.setReceiverFiscalCode("PLOMRC01P30L736Y5");
        sendRequest.setProductType(ProductTypeEnum.RIR);
        sendRequest.setReceiverType("PF");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setRequestPaId("FFPAPERTEST.IUN_FATY-FATY-2023041520230302-101111.RECINDEX_0");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        sendRequest.setSenderAddress(analogAddress);
        sendRequest.setArAddress(analogAddress);
        sendRequest.setClientRequestTimeStamp(Instant.now());
        sendRequest.setProductType(ProductTypeEnum.RIR);

    }
}