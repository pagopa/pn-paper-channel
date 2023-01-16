package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

@Slf4j
class RequestDeliveryDAOTest extends BaseTest {

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;

    @Test
    void createWithAddressGetByRequestIdTest(){
        requestDeliveryDAO.createWithAddress(getPnDeliveryRequest("bvf123abcd"),getPnAddress()).block();
        PnDeliveryRequest pnDeliveryRequest = requestDeliveryDAO.getByRequestId(getPnDeliveryRequest("bvf123abcd").getRequestId()).block();
        Assertions.assertNotNull(pnDeliveryRequest);
    }

    @Test
    void createWithAddressGetByRequestIdAlredyExistTest(){
        requestDeliveryDAO.createWithAddress(getPnDeliveryRequest("abcd469gmr"),getPnAddress()).block();
        StepVerifier.create(requestDeliveryDAO.createWithAddress(getPnDeliveryRequest("abcd469gmr"),getPnAddress()))
                .expectError(PnHttpResponseException.class).verify();
    }

    @Test
    void createWithAddressGetByFiscalCodeTest(){
        requestDeliveryDAO.createWithAddress(getPnDeliveryRequest("jkl261lxw"),getPnAddress()).block();
        PnDeliveryRequest pnDeliveryRequest = requestDeliveryDAO.getByFiscalCode(getPnDeliveryRequest("jkl261lxw").getFiscalCode()).blockFirst();
        Assertions.assertNotNull(pnDeliveryRequest);
    }

    @Test
    void updateDataTest(){
        requestDeliveryDAO.createWithAddress(getPnDeliveryRequestUpdate(),getPnAddress()).block();
        requestDeliveryDAO.getByFiscalCode(getPnDeliveryRequest("lrc537nhe").getFiscalCode()).blockFirst();
        requestDeliveryDAO.updateData(getPnDeliveryRequest("lrc537nhe")).block();
        PnDeliveryRequest pnDeliveryRequestUpdate = requestDeliveryDAO.getByFiscalCode(getPnDeliveryRequest("lrc537nhe").getFiscalCode()).blockFirst();
        Assertions.assertNotNull(pnDeliveryRequestUpdate);
    }

    @Test
    void createWithAddressGetByCorrelationIdTest(){
        requestDeliveryDAO.createWithAddress(getPnDeliveryRequest("537nhe256"),getPnAddress()).block();
        PnDeliveryRequest pnDeliveryRequest = requestDeliveryDAO.getByCorrelationId(getPnDeliveryRequest("537nhe256").getCorrelationId()).block();
        Assertions.assertNotNull(pnDeliveryRequest);
    }

    private PnAddress getPnAddress(){
        PnAddress pnAddress = new PnAddress();
        pnAddress.setRequestId("abcd123abcd");
        pnAddress.setAddress("via roma");
        pnAddress.setAddressRow2("via lazio");
        pnAddress.setCap("00061");
        pnAddress.setCity("roma");
        pnAddress.setCity2("viterbo");
        pnAddress.setCountry("italia");
        pnAddress.setPr("PR");
        pnAddress.setFullName("Ettore Fieramosca");
        pnAddress.setNameRow2("Ettore");
        return pnAddress;
    }

    private PnDeliveryRequest getPnDeliveryRequest(String requestId){
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
        deliveryRequest.setRequestId(requestId);
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("RT");
        deliveryRequest.setIun("iun");
        deliveryRequest.setCorrelationId("537nhe256");
        deliveryRequest.setStatusCode("PC000");
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }

    private PnDeliveryRequest getPnDeliveryRequestUpdate(){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("http://localhost:8090");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(2);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        Address address = new Address();
        address.setAddress("via napoli");
        address.setAddressRow2("via milano");
        address.setCap("00090");
        address.setCity("napoli");
        address.setCity2("caserta");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Mario Fieramosca");
        address.setNameRow2("Mario");

        deliveryRequest.setAddressHash(address.convertToHash());
        deliveryRequest.setRequestId("abcd123abcd");
        deliveryRequest.setFiscalCode("MRND123AB501");
        deliveryRequest.setReceiverType("RT");
        deliveryRequest.setIun("iun");
        deliveryRequest.setCorrelationId("fghj6235paqk");
        deliveryRequest.setStatusCode("PC000");
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        return deliveryRequest;
    }
}
