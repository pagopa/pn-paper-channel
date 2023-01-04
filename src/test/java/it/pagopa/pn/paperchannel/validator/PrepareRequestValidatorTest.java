package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
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
    void prepareRequestValidatorTest() {
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
}
