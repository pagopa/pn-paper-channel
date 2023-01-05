package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class AttachmentValidatorTest {

    @Test
    void prepareRequestValidatorTestTrue() {
        Assertions.assertTrue(AttachmentValidator.checkBetweenLists(getAttachmentUrls(),getAttachments()));
    }

    @Test
    void prepareRequestValidatorTestFalse() {
        Assertions.assertFalse(AttachmentValidator.checkBetweenLists(getAttachmentUrls(),new ArrayList<>()));
    }
    private  List<String> getAttachmentUrls(){
        List<String> attachmentUrls = new ArrayList<>();
        String url = "http://localhost:8080";
        attachmentUrls.add(url);
        return attachmentUrls;
    }

    private  List<PnAttachmentInfo>  getAttachments(){
        List<PnAttachmentInfo> attachments = new ArrayList<>();
        PnAttachmentInfo attachmentInfo = new PnAttachmentInfo();
        attachmentInfo.setFileKey("http://localhost:8080");
        attachments.add(attachmentInfo);
        return attachments;
    }
}
