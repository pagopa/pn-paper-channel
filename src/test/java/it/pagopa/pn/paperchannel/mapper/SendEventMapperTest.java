package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class SendEventMapperTest {


    PnDeliveryRequest deliveryRequest;
    SendEvent response;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<SendEventMapper> constructor = SendEventMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, constructor::newInstance);
        Assertions.assertNull(exception.getMessage());
    }

    @Test
    void sendEventMapperFromResultTest () {
        response.setStatusCode(StatusCodeEnum.OK);
        response = SendEventMapper.fromResult(deliveryRequest ,getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertEquals("2023-07-07T08:43:00.764Z", response.getStatusDateTime().toString());
        Assertions.assertEquals("2023-07-07T08:43:00.764Z", response.getStatusDateTime().toString());
    }

    private void initialize(){
        response = new SendEvent();
        deliveryRequest = new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("2023-07-07T08:43:00.764Z");
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
        deliveryRequest.setStatusCode(StatusCodeEnum.OK.getValue());
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("2023-07-07T08:43:00.764Z");
        deliveryRequest.setProposalProductType("");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("2023-07-07T08:43:00.764Z");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        List<PnAttachmentInfo> attachments;
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
