package it.pagopa.pn.paperchannel.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class AttachmentValidatorTest {

    @Test
    void prepareRequestValidatorTestTrue() {
        Assertions.assertTrue(AttachmentValidator.checkBetweenLists(getAttachmentUrls(),getAttachmentUrls()));
    }

    @Test
    void prepareRequestValidatorTestFalse() {
        Assertions.assertFalse(AttachmentValidator.checkBetweenLists(getAttachmentUrls(),new ArrayList<>()));
    }
    private  List<String> getAttachmentUrls(){
        return new ArrayList<>(List.of("http://localhost:8080", "Abxlllsss", "Safe1urnn"));
    }

}
