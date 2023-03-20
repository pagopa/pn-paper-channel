package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperEvent;
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
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("id");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("documentType");
        pnAttachmentInfo.setUrl("url");
        attachmentUrls.add(pnAttachmentInfo);
        deliveryRequest.setRequestId("requestId");
        deliveryRequest.setRequestPaId("requestPaId");
        deliveryRequest.setFiscalCode("fiscalCode");
        deliveryRequest.setReceiverType("receiverType");
        deliveryRequest.setIun("iun");
        deliveryRequest.setHashOldAddress("hashOldAddress");
        deliveryRequest.setPrintType("printType");
        deliveryRequest.setCorrelationId("correlationId");
        deliveryRequest.setStatusDetail("statusDetail");
        deliveryRequest.setProposalProductType("proposalProductType");
        deliveryRequest.setProductType("productType");
        deliveryRequest.setAttachments(attachmentUrls);
        deliveryRequest.setStatusCode("statusCode");
    }

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<PreparePaperResponseMapper> constructor = PreparePaperResponseMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void preparePaperResponseMapperFromResultTest () {
        PaperChannelUpdate response= PreparePaperResponseMapper.fromResult(deliveryRequest,getPnAddress());
        Assertions.assertNotNull(response);
    }
    @Test

    void preparePaperResponseMapperFromResultStatusTest () {
        PaperChannelUpdate response= PreparePaperResponseMapper.fromResult(getPnDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING.getCode()),getPnAddress());
        Assertions.assertNotNull(response);
    }
    @Test
    void preparePaperResponseMapperFromEventTest () {
        PaperEvent response= PreparePaperResponseMapper.fromEvent("12345");
        Assertions.assertNotNull(response);
    }
    private PnDeliveryRequest getPnDeliveryRequest(String status){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");
        deliveryRequest.setAddressHash(address.convertToHash());

        deliveryRequest.setRequestId("12345abcde");
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("RT");
        deliveryRequest.setIun("");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode(status);
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        List<PnAttachmentInfo> attachments;
        return deliveryRequest;
    }
    public PnAddress getPnAddress() {
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
