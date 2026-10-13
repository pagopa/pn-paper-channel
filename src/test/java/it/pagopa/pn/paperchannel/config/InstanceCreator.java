package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProposalTypeEnum;

import java.util.ArrayList;
import java.util.List;

public class InstanceCreator {


    public static PrepareRequest getPrepareRequest() {
        PrepareRequest prepareRequest = new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress= new AnalogAddress();
        String s ="url12345";
        attachmentUrls.add(s);

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");

        prepareRequest.setRequestId("12345ABC");
        prepareRequest.setAttachmentUrls(attachmentUrls);
        prepareRequest.setDiscoveredAddress(analogAddress);
        prepareRequest.setIun("iun");
        prepareRequest.setReceiverAddress(analogAddress);
        prepareRequest.setPrintType("BN_FRONTE_RETRO");
        prepareRequest.setRelatedRequestId(null);
        prepareRequest.setProposalProductType(ProposalTypeEnum.AR);
        prepareRequest.setReceiverFiscalCode("FRMTTR76M06B715E");
        prepareRequest.setReceiverType("PF");
        return prepareRequest;
    }

}
