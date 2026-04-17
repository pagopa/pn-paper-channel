package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.CommunicationType;
import it.pagopa.pn.paperchannel.model.PrepareRequestInt;
import it.pagopa.pn.paperchannel.model.PrintType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class RequestDeliveryMapperTest {


    @Test
    void requestDeliveryMapperTest () {
        PrepareRequestInt prepareRequest = getPrepareRequest();
        PnDeliveryRequest response= RequestDeliveryMapper.toEntity(prepareRequest);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getAarWithRadd(), prepareRequest.getAarWithRadd());
        Assertions.assertEquals(response.getRequestId(), prepareRequest.getRequestId());
        Assertions.assertEquals(response.getRelatedRequestId(), prepareRequest.getRelatedRequestId());
        Assertions.assertEquals(response.getIun(), prepareRequest.getIun());
        Assertions.assertEquals(response.getReceiverType(), prepareRequest.getReceiverType());
        Assertions.assertEquals(response.getPrintType(), prepareRequest.getPrintType().name());
        Assertions.assertEquals(response.getFiscalCode(), prepareRequest.getReceiverFiscalCode());
        Assertions.assertEquals(response.getNotificationSentAt(), prepareRequest.getNotificationSentAt());
        Assertions.assertEquals(response.getAttachments().stream().map(PnAttachmentInfo::getFileKey).toList(), prepareRequest.getAttachmentUrls());
        Assertions.assertEquals(response.getSenderPaId(), prepareRequest.getSenderPaId());
        Assertions.assertEquals(response.getClientId(), prepareRequest.getClientId());
        Assertions.assertEquals(response.getCommunicationType(), prepareRequest.getCommunicationType().name());
    }

    private PrepareRequestInt getPrepareRequest() {
        PrepareRequestInt prepareRequest = new PrepareRequestInt();
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
        prepareRequest.setSenderPaId("senderPaId");
        prepareRequest.setAttachmentUrls(attachmentUrls);
        prepareRequest.setDiscoveredAddress(analogAddress);
        prepareRequest.setIun("iun");
        prepareRequest.setReceiverAddress(analogAddress);
        prepareRequest.setPrintType(PrintType.BN_FRONTE);
        prepareRequest.setRelatedRequestId(null);
        prepareRequest.setProposalProductType(ProposalTypeEnum.AR);
        prepareRequest.setReceiverFiscalCode("FRMTTR76M06B715E");
        prepareRequest.setReceiverType("PF");
        prepareRequest.setAarWithRadd(true);
        prepareRequest.setClientId("clientId");
        prepareRequest.setCommunicationType(CommunicationType.LEGAL);
        return prepareRequest;
    }
}
