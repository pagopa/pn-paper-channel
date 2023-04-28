package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class PnDeliveryRequestTest {
    private String requestId;
    private String fiscalCode;
    private String hashedFiscalCode;
    private String receiverType;
    private String iun;
    private String correlationId;
    private String addressHash;
    private String hashOldAddress;
    private String statusCode;
    private String statusDetail;
    private String statusDate;
    private String proposalProductType;
    private String printType;
    private String startDate;
    private String productType;
    private String relatedRequestId;
    private List<PnAttachmentInfo> attachments;
    private String requestPaId;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

   // @Test
    void toStringTest() {
        PnDeliveryRequest pnDeliveryRequest = initPnDeliveryRequest();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnDeliveryRequest.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("requestId=");
        stringBuilder.append(requestId);
        stringBuilder.append(", ");
        stringBuilder.append("fiscalCode=");
        stringBuilder.append(fiscalCode);
        stringBuilder.append(", ");
        stringBuilder.append("hashedFiscalCode=");
        stringBuilder.append(hashedFiscalCode);
        stringBuilder.append(", ");
        stringBuilder.append("receiverType=");
        stringBuilder.append(receiverType);
        stringBuilder.append(", ");
        stringBuilder.append("iun=");
        stringBuilder.append(iun);
        stringBuilder.append(", ");
        stringBuilder.append("correlationId=");
        stringBuilder.append(correlationId);
        stringBuilder.append(", ");
        stringBuilder.append("addressHash=");
        stringBuilder.append(addressHash);
        stringBuilder.append(", ");
        stringBuilder.append("hashOldAddress=");
        stringBuilder.append(hashOldAddress);
        stringBuilder.append(", ");
        stringBuilder.append("statusCode=");
        stringBuilder.append(statusCode);
        stringBuilder.append(", ");
        stringBuilder.append("statusDetail=");
        stringBuilder.append(statusDetail);
        stringBuilder.append(", ");
        stringBuilder.append("statusDate=");
        stringBuilder.append(statusDate);
        stringBuilder.append(", ");
        stringBuilder.append("proposalProductType=");
        stringBuilder.append(proposalProductType);
        stringBuilder.append(", ");
        stringBuilder.append("printType=");
        stringBuilder.append(printType);
        stringBuilder.append(", ");
        stringBuilder.append("startDate=");
        stringBuilder.append(startDate);
        stringBuilder.append(", ");
        stringBuilder.append("productType=");
        stringBuilder.append(productType);
        stringBuilder.append(", ");
        stringBuilder.append("relatedRequestId=");
        stringBuilder.append(relatedRequestId);
        stringBuilder.append(", ");
        stringBuilder.append("attachments=");
        stringBuilder.append(attachments);
        stringBuilder.append(", ");
        stringBuilder.append("requestPaId=");
        stringBuilder.append(requestPaId);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnDeliveryRequest.toString());
    }

    private PnDeliveryRequest initPnDeliveryRequest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId(requestId);
        pnDeliveryRequest.setFiscalCode(fiscalCode);
        pnDeliveryRequest.setHashedFiscalCode(hashedFiscalCode);
        pnDeliveryRequest.setReceiverType(receiverType);
        pnDeliveryRequest.setIun(iun);
        pnDeliveryRequest.setCorrelationId(correlationId);
        pnDeliveryRequest.setAddressHash(addressHash);
        pnDeliveryRequest.setHashOldAddress(hashOldAddress);
        pnDeliveryRequest.setStatusCode(statusCode);
        pnDeliveryRequest.setStatusDetail(statusDetail);
        pnDeliveryRequest.setStatusDate(statusDate);
        pnDeliveryRequest.setProposalProductType(proposalProductType);
        pnDeliveryRequest.setPrintType(printType);
        pnDeliveryRequest.setStartDate(startDate);
        pnDeliveryRequest.setProductType(productType);
        pnDeliveryRequest.setRelatedRequestId(relatedRequestId);
        pnDeliveryRequest.setAttachments(attachments);
        pnDeliveryRequest.setRequestPaId(requestPaId);
        return pnDeliveryRequest;
    }

    private void initialize() {
        requestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_1";
        fiscalCode = "ABCDEF98G76H543I";
        hashedFiscalCode = "j54jknbi3392llfm29fe8fhokfgretg54jfkjiop4kyoads";
        receiverType = "PF";
        iun = "ABCD-HILM-YKWX-202202-1";
        correlationId = "ZAXSCDVFBGNH";
        addressHash = "fjn3kj4h4i4oh095grt0e320h3gvnr9rgmprm3g308h80rg30j";
        hashOldAddress = "pv84bfoij30chewdfhg0023jhnhofj0393ihif200ri4";
        statusCode = StatusDeliveryEnum.IN_PROCESSING.getCode();
        statusDetail = StatusDeliveryEnum.IN_PROCESSING.getDescription();
        statusDate = DateUtils.formatDate(new Date());
        proposalProductType = "AR";
        printType = "BN_FRONTE_RETRO";
        startDate = DateUtils.formatDate(new Date());
        productType = "890";
        relatedRequestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_2";
        attachments = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("2022-12-20T16:17:35.02647+01:00");
        pnAttachmentInfo.setFileKey("/safe-storage/v1/files/PDFURL");
        pnAttachmentInfo.setId("A1S2D3F4");
        pnAttachmentInfo.setDocumentType("PN_LEGALFACT");
        pnAttachmentInfo.setNumberOfPage(2);
        pnAttachmentInfo.setUrl("https://www.africau.edu/images/default/sample.pdf");
        pnAttachmentInfo.setChecksum("j49fkldvnj4890efmeff433t2gvnr9rgmprm3g308jknbi3392llfm29fe8");
        attachments.add(new PnAttachmentInfo());
        requestPaId = "requestPaId";
    }
}
