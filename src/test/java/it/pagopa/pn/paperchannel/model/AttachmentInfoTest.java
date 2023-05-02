package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class AttachmentInfoTest {

    private String id;
    private String documentType;
    private String url;
    private String date;
    private String fileKey;
    private int numberOfPage;
    private String sha256;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void testSorting(){
        List<AttachmentInfo> lisUnsorted = getListOfAttachments();
        Collections.sort(lisUnsorted);

        Assertions.assertNotNull(lisUnsorted);
        Assertions.assertEquals(Const.PN_AAR, lisUnsorted.get(0).getDocumentType());
        Assertions.assertEquals(Const.PN_AAR, lisUnsorted.get(1).getDocumentType());

        List<AttachmentInfo> list2 = getListOfAttachments();
        AttachmentInfo first = list2.get(0);
        list2.remove(0);
        list2.add(list2.size(), first);

        Collections.sort(list2);

        Assertions.assertNotNull(list2);
        Assertions.assertEquals(Const.PN_AAR, list2.get(0).getDocumentType());
        Assertions.assertEquals(Const.PN_AAR, list2.get(1).getDocumentType());


        AttachmentInfo attachmentInfo1 = new AttachmentInfo();
        attachmentInfo1.setId("12345");
        attachmentInfo1.setDocumentType(Const.PN_PAPER_CHANNEL);
        attachmentInfo1.setUrl("09876");
        AttachmentInfo attachmentInfo2 = new AttachmentInfo();
        attachmentInfo2.setId("09876");
        attachmentInfo2.setDocumentType(Const.PN_PAPER_CHANNEL);
        attachmentInfo2.setUrl("12345");
        lisUnsorted.clear();
        lisUnsorted.add(attachmentInfo1);
        lisUnsorted.add(attachmentInfo2);
        Collections.sort(lisUnsorted);
        Assertions.assertNotNull(lisUnsorted);
        Assertions.assertEquals(Const.PN_PAPER_CHANNEL, lisUnsorted.get(0).getDocumentType());
        Assertions.assertEquals(Const.PN_PAPER_CHANNEL, lisUnsorted.get(1).getDocumentType());
    }

    @Test
    void toStringTest() {
        AttachmentInfo attachmentInfo = initAttachmentInfo();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(attachmentInfo.getClass().getSimpleName());
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
        stringBuilder.append("sha256=");
        stringBuilder.append(sha256);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, attachmentInfo.toString());
    }

    private List<AttachmentInfo> getListOfAttachments(){
        AttachmentInfo info1 = new AttachmentInfo();
        info1.setId("1234");
        info1.setDocumentType(Const.PN_AAR);
        info1.setUrl("12345");

        AttachmentInfo info2 = new AttachmentInfo();
        info2.setId("1234");
        info2.setDocumentType("PN_DOCUMENT");
        info2.setUrl("12345");

        AttachmentInfo info3 = new AttachmentInfo();
        info3.setId("1234");
        info3.setDocumentType("PN_LEGALFACT");
        info3.setUrl("12345");

        AttachmentInfo info4 = new AttachmentInfo();
        info4.setId("1234");
        info4.setDocumentType(Const.PN_AAR);
        info4.setUrl("12345");

        AttachmentInfo info5 = new AttachmentInfo();
        info5.setId("1234");
        info5.setDocumentType("PN_ARP");
        info5.setUrl("12345");

        return new ArrayList<>(List.of(info1, info2, info3, info4, info5));
    }

    private AttachmentInfo initAttachmentInfo() {
        AttachmentInfo attachmentInfo = new AttachmentInfo();
        attachmentInfo.setId(id);
        attachmentInfo.setDocumentType(documentType);
        attachmentInfo.setUrl(url);
        attachmentInfo.setDate(date);
        attachmentInfo.setFileKey(fileKey);
        attachmentInfo.setNumberOfPage(numberOfPage);
        attachmentInfo.setSha256(sha256);
        return attachmentInfo;
    }

    private void initialize() {
        id = "A1S2D3F4";
        documentType = "PN_LEGALFACT";
        url = "https://www.africau.edu/images/default/sample.pdf";
        date = "2022-12-20T16:17:35.02647+01:00";
        fileKey = "/safe-storage/v1/files/PDFURL";
        numberOfPage = 3;
        sha256 = "j49fkldvnj4890efmeff433t2gvnr9rgmprm3g308jknbi3392llfm29fe8";
    }
}
