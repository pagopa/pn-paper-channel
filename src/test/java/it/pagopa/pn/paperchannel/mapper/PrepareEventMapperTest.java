package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class PrepareEventMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<PrepareEventMapper> constructor = PrepareEventMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, constructor::newInstance);
        Assertions.assertNull(exception.getMessage());
    }

    @Test
    void prepareEventMapperProcessingTest () {
        PrepareEvent response= PrepareEventMapper.fromResult(getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING),getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getStatusCode().getValue(),StatusDeliveryEnum.IN_PROCESSING.getDetail());
        Assertions.assertEquals(response.getStatusDetail(), StatusDeliveryEnum.IN_PROCESSING.getCode());
    }

    @Test
    void prepareEventMapperUntaceableTest () {
        PrepareEvent response= PrepareEventMapper.fromResult(getDeliveryRequest(StatusDeliveryEnum.UNTRACEABLE),getPnAddress());
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getStatusCode().getValue(),StatusDeliveryEnum.UNTRACEABLE.getDetail());
        Assertions.assertEquals(response.getStatusDetail(), StatusDeliveryEnum.UNTRACEABLE.getCode());
    }

    @Test
    void prepareEventMapperToPrepareEventTest () {
        PrepareEvent response= PrepareEventMapper.toPrepareEvent(getDeliveryRequest(StatusDeliveryEnum.UNTRACEABLE),getAddress(), StatusCodeEnum.OK, null);
        Assertions.assertNotNull(response);
    }


    @Test
    void prepareEventMapperToPrepareEventTest_withgenerated () {
        PnDeliveryRequest req = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);

        PnAttachmentInfo f24 = new PnAttachmentInfo();
        f24.setUrl("safestorage://1");
        f24.setGeneratedFrom("f24set://qualcosa");
        req.getAttachments().add(f24);

        f24 = new PnAttachmentInfo();
        f24.setUrl("safestorage://2");
        f24.setGeneratedFrom("f24set://qualcosa");
        req.getAttachments().add(f24);

        PrepareEvent response= PrepareEventMapper.toPrepareEvent(req,getAddress(), StatusCodeEnum.OK, null);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(2, response.getReplacedF24AttachmentUrls().size());
    }

    private PnDeliveryRequest getDeliveryRequest(StatusDeliveryEnum status){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("2023-07-07T08:43:00.764Z");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("http://localhost:8080");
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
        address.setFromNationalRegistry(true);

        deliveryRequest.setAddressHash(address.convertToHash());
        deliveryRequest.setRequestId("12345");
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun("iun");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode(status.getCode());
        deliveryRequest.setStatusDescription(status.getDescription());
        deliveryRequest.setStatusDetail(status.getDetail());
        deliveryRequest.setStatusDate("2023-07-07T08:43:00.764Z");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }
    public Address getAddress(){
        Address address= new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");
        return address;
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
