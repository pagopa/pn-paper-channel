package it.pagopa.pn.paperchannel.middleware.db.entities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class PnAttachmentInfoTest {
    private String id;
    private String documentType;
    private String url;
    private String date;
    private String fileKey;
    private Integer numberOfPage;
    private String checksum;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnAttachmentInfo pnAttachmentInfo = initAttachmentInfo();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnAttachmentInfo.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("id=");
        stringBuilder.append(id);
        stringBuilder.append(", ");
        stringBuilder.append("documentType=");
        stringBuilder.append(documentType);
        stringBuilder.append(", ");
        stringBuilder.append("url=");
        stringBuilder.append(url);
        stringBuilder.append(", ");
        stringBuilder.append("date=");
        stringBuilder.append(date);
        stringBuilder.append(", ");
        stringBuilder.append("fileKey=");
        stringBuilder.append(fileKey);
        stringBuilder.append(", ");
        stringBuilder.append("numberOfPage=");
        stringBuilder.append(numberOfPage);
        stringBuilder.append(", ");
        stringBuilder.append("checksum=");
        stringBuilder.append(checksum);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnAttachmentInfo.toString());
    }

    private PnAttachmentInfo initAttachmentInfo() {
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setId(id);
        pnAttachmentInfo.setDocumentType(documentType);
        pnAttachmentInfo.setUrl(url);
        pnAttachmentInfo.setDate(date);
        pnAttachmentInfo.setFileKey(fileKey);
        pnAttachmentInfo.setNumberOfPage(numberOfPage);
        pnAttachmentInfo.setChecksum(checksum);
        return pnAttachmentInfo;
    }

    private void initialize() {
        id = "A1S2D3F4";
        documentType = "PN_LEGALFACT";
        url = "https://www.africau.edu/images/default/sample.pdf";
        date = "2022-12-20T16:17:35.02647+01:00";
        fileKey = "/safe-storage/v1/files/PDFURL";
        numberOfPage = 3;
        checksum = "j49fkldvnj4890efmeff433t2gvnr9rgmprm3g308jknbi3392llfm29fe8";
    }
}
