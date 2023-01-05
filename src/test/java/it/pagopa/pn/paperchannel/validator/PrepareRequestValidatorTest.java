package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class PrepareRequestValidatorTest {


    @Test
    void prepareRequestValidatorTestOK() {
        PrepareRequestValidator.compareRequestEntity(getRequestOK(),getEntityOK());
    }

    @Test
    void prepareRequestValidatorTestError() {
        List<String> errors = new ArrayList<>();
        PnGenericException ex = Assertions.assertThrows(PnInputValidatorException.class,
                () -> PrepareRequestValidator.compareRequestEntity(getRequest(),getEntity()));
        Assertions.assertNotNull(errors);
        Assertions.assertEquals("Richiesta già preso in carico ma sono state inviate informazioni differenti (requestId già presente)", ex.getMessage());
    }
    private PnDeliveryRequest getEntity(){
        PnDeliveryRequest sendRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("");
        pnAttachmentInfo.setFileKey("");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("");
        attachmentUrls.add(pnAttachmentInfo);

        sendRequest.setRequestId("");
        sendRequest.setFiscalCode("");
        sendRequest.setReceiverType("");
        sendRequest.setIun("");
        sendRequest.setCorrelationId("");
        sendRequest.setAddressHash("");
        sendRequest.setStatusCode("");
        sendRequest.setStatusDetail("");
        sendRequest.setStatusDate("");
        sendRequest.setProposalProductType("");
        sendRequest.setPrintType("");
        sendRequest.setStartDate("");
        sendRequest.setProductType("");
        sendRequest.setAttachments(attachmentUrls);
        List<PnAttachmentInfo> attachments;
        return sendRequest;
    }

    private PrepareRequest getRequest(){
        PrepareRequest sendRequest= new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress= new AnalogAddress();
        String s = "http://localhost:8080";
        attachmentUrls.add(s);

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");
        sendRequest.setRequestId("12345abcde");
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProposalProductType(ProposalTypeEnum.AR);
        sendRequest.setReceiverType("RT");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        return sendRequest;
    }

    private PnDeliveryRequest getEntityOK(){
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
        deliveryRequest.setIun("iun");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode("");
        deliveryRequest.setStatusDetail("");
        deliveryRequest.setStatusDate("");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        List<PnAttachmentInfo> attachments;
        return deliveryRequest;
    }

    private PrepareRequest getRequestOK(){
        PrepareRequest sendRequest= new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress= new AnalogAddress();
        String s = "http://localhost:8080";
        attachmentUrls.add(s);

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");
        analogAddress.setNameRow2("Ettore");

        sendRequest.setRequestId("12345abcde");
        sendRequest.setReceiverFiscalCode("ABCD123AB501");
        sendRequest.setProposalProductType(ProposalTypeEnum.AR);
        sendRequest.setReceiverType("RT");
        sendRequest.setPrintType("PT");
        sendRequest.setIun("iun");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        return sendRequest;
    }
}
