package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class PreparePaperResponseMapperTest {
    PnDeliveryRequest deliveryRequest;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    private void initialize() {
        deliveryRequest = new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("2023-07-07T08:43:00.764Z");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("id");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("documentType");
        pnAttachmentInfo.setUrl("url");
        attachmentUrls.add(pnAttachmentInfo);
        deliveryRequest.setRequestId("requestId");
        deliveryRequest.setRequestPaId("requestPaId");
        deliveryRequest.setFiscalCode("fiscalCode");
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun("iun");
        deliveryRequest.setHashOldAddress("hashOldAddress");
        deliveryRequest.setPrintType("FRONTE-RETRO");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setProductType("AR");
        deliveryRequest.setStatusDate("2023-07-07T08:43:00.764Z");
        deliveryRequest.setStartDate("2023-07-07T08:43:00.764Z");
        deliveryRequest.setAttachments(attachmentUrls);

    }

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<PreparePaperResponseMapper> constructor = PreparePaperResponseMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, constructor::newInstance);
        Assertions.assertNull(exception.getMessage());
    }

    @Test
    void preparePaperResponseMapperFromResultInProcessingStatusTest() {
        PaperChannelUpdate response= PreparePaperResponseMapper.fromResult(setStatus(StatusDeliveryEnum.IN_PROCESSING),getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getPrepareEvent());
        Assertions.assertEquals(response.getPrepareEvent().getStatusCode().getValue(),StatusDeliveryEnum.IN_PROCESSING.getDetail());
        Assertions.assertEquals(response.getPrepareEvent().getStatusDetail(), StatusDeliveryEnum.IN_PROCESSING.getCode());
        Assertions.assertEquals("2023-07-07T08:43:00.764Z", response.getPrepareEvent().getStatusDateTime().toString());
    }

    @Test
    void preparePaperResponseMapperFromResultTakingChargeStatusTest() {
        PaperChannelUpdate response= PreparePaperResponseMapper.fromResult(setStatus(StatusDeliveryEnum.TAKING_CHARGE),getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getPrepareEvent());
        Assertions.assertEquals(response.getPrepareEvent().getStatusCode().getValue(),StatusDeliveryEnum.TAKING_CHARGE.getDetail());
        Assertions.assertEquals(response.getPrepareEvent().getStatusDetail(), StatusDeliveryEnum.TAKING_CHARGE.getCode());
        Assertions.assertEquals("2023-07-07T08:43:00.764Z", response.getPrepareEvent().getStatusDateTime().toString());

    }

    @Test
    void preparePaperResponseMapperFromResultNationalRegistryWaitingStatusTest() {
        PaperChannelUpdate response= PreparePaperResponseMapper.fromResult(setStatus(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING),getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getPrepareEvent());
        Assertions.assertEquals(response.getPrepareEvent().getStatusCode().getValue(),StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDetail());
        Assertions.assertEquals(response.getPrepareEvent().getStatusDetail(), StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode());
        Assertions.assertEquals("2023-07-07T08:43:00.764Z", response.getPrepareEvent().getStatusDateTime().toString());
    }

    @Test
    void preparePaperResponseMapperFromResultAsyncErrorStatusTest() {
        PaperChannelUpdate response= PreparePaperResponseMapper.fromResult(setStatus(StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR),getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getPrepareEvent());
        Assertions.assertEquals(response.getPrepareEvent().getStatusCode().getValue(),StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR.getDetail());
        Assertions.assertEquals(response.getPrepareEvent().getStatusDetail(), StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR.getCode());
        Assertions.assertEquals("2023-07-07T08:43:00.764Z", response.getPrepareEvent().getStatusDateTime().toString());

    }

    @Test
    void preparePaperResponseMapperFromResultSafeStorageErrorStatusTest() {
        PaperChannelUpdate response= PreparePaperResponseMapper.fromResult(setStatus(StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR),getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getPrepareEvent());
        Assertions.assertEquals(response.getPrepareEvent().getStatusCode().getValue(),StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR.getDetail());
        Assertions.assertEquals(response.getPrepareEvent().getStatusDetail(), StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR.getCode());
        Assertions.assertEquals("2023-07-07T08:43:00.764Z", response.getPrepareEvent().getStatusDateTime().toString());

    }

    @Test
    void preparePaperResponseMapperPrepareEventNullTest() {
        PaperChannelUpdate response= PreparePaperResponseMapper.fromResult(setStatus(StatusDeliveryEnum.PRINTED),getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getSendEvent());
  }

    private PnDeliveryRequest setStatus(StatusDeliveryEnum status){
        deliveryRequest.setStatusCode(status.getCode());
        deliveryRequest.setStatusDetail(status.getDetail());
        deliveryRequest.setStatusDescription(status.getDescription());
        return  deliveryRequest;
    }



    private PnAddress getPnAddress() {
        PnAddress pnAddress = new PnAddress();
        pnAddress.setAddress("via roma");
        pnAddress.setAddressRow2("via lazio");
        pnAddress.setCap("00061");
        pnAddress.setCity("roma");
        pnAddress.setCity2("viterbo");
        pnAddress.setCountry("italia");
        pnAddress.setPr("PR");
        pnAddress.setFullName("Ettore Fieramosca");
        pnAddress.setNameRow2("Ettore");
        pnAddress.setTtl(10L);
        return pnAddress;
    }

}
