package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.utils.Utility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SendRequestValidatorTest {

    //@Test
    void sendRequestValidatorTestOK() {
        SendRequestValidator.compareRequestEntity(getRequestOK(),getEntityOK());
    }

    @Test
    void sendRequestValidatorTestError() {
        try {
            SendRequestValidator.compareRequestEntity(getRequest(),getEntity());
        } catch (PnGenericException ex) {
            assertNotNull(ex);
            assertEquals(DIFFERENT_DATA_REQUEST, ex.getExceptionType());
        }
        Assertions.assertNotNull(getEntity().getAddressHash());
    }

    @Test
    void compareDtoExternalAndDeliveryRequestKOTest(){
        PaperProgressStatusEventDto dto = new PaperProgressStatusEventDto();
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        dto.setIun("123-asd");
        deliveryRequest.setIun("132-der");
        getEntity().setAddressHash(null);


        try {
            SendRequestValidator.compareProgressStatusRequestEntity(dto, deliveryRequest);
        } catch (PnGenericException ex) {
            assertNotNull(ex);
            assertEquals(DIFFERENT_DATA_RESULT, ex.getExceptionType());
        }

        deliveryRequest.setIun(dto.getIun());
        dto.setProductType("AR");
        deliveryRequest.setProposalProductType("890");

        try {
            SendRequestValidator.compareProgressStatusRequestEntity(dto, deliveryRequest);
        } catch (PnGenericException ex) {
            assertNotNull(ex);
            assertEquals(DIFFERENT_DATA_RESULT, ex.getExceptionType());
        }

        deliveryRequest.setProposalProductType("AR");
        AttachmentDetailsDto detailsDto = new AttachmentDetailsDto();
        detailsDto.setUri("localhost:8080");
        dto.setAttachments(List.of(detailsDto));

        try {
            SendRequestValidator.compareProgressStatusRequestEntity(dto, deliveryRequest);
        } catch (PnGenericException ex) {
            assertNotNull(ex);
            assertEquals(DIFFERENT_DATA_RESULT, ex.getExceptionType());
        }

    }


    @Test
    void compareRequestCostEntityKO() {
        try {
            SendRequestValidator.compareRequestCostEntity(10, 100);
            Assertions.fail(new Throwable("invalid"));
        } catch (PnGenericException ex) {
            assertNotNull(ex);
            assertEquals(DIFFERENT_SEND_COST, ex.getExceptionType());
            assertEquals(422, ex.getHttpStatus().value());
        }
    }



    @Test
    void compareRequestCostEntityOK() {
        Assertions.assertDoesNotThrow(() -> SendRequestValidator.compareRequestCostEntity(10, 10));

    }



    @Test
    void compareRequestCostEntityOK2() {
        Assertions.assertDoesNotThrow(() -> SendRequestValidator.compareRequestCostEntity(10, null));

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

    private SendRequest getRequest(){
        SendRequest sendRequest= new SendRequest();
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
        sendRequest.setProductType(ProductTypeEnum.RIR);
        sendRequest.setReceiverType("RT");
        sendRequest.setPrintType("PT");
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
        deliveryRequest.setHashedFiscalCode(Utility.convertToHash("ABCD123AB501"));
        deliveryRequest.setReceiverType("RT");
        deliveryRequest.setIun("");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode("");
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

    private SendRequest getRequestOK(){
        SendRequest sendRequest= new SendRequest();
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
        sendRequest.setProductType(ProductTypeEnum.RIR);
        sendRequest.setReceiverType("RT");
        sendRequest.setPrintType("PT");
        sendRequest.setAttachmentUrls(attachmentUrls);
        sendRequest.setReceiverAddress(analogAddress);
        return sendRequest;
    }
}
