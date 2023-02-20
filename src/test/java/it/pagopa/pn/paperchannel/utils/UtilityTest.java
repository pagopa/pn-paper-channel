package it.pagopa.pn.paperchannel.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.bouncycastle.jce.provider.BrokenPBE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilityTest {
    private String capListDuplicate;
    private String capListErrorRange;
    private String capListOk;

    @BeforeEach
    void setUp(){
        capListDuplicate = "20010-20900,20020,12934,98798,72648,17284";
        capListOk = "00165-00170,12345,29593-29600,40593,67399";
        capListErrorRange = "3e4433-45500";
    }

    @Test
    @DisplayName("whenCallingIsValidMethodIHaveDuplicates")
    void isValidCapErrorTest() {
        List<String> caps = Utility.isValidCap(capListDuplicate);
        Assertions.assertNull(caps);

        caps = Utility.isValidCap(capListErrorRange);
        Assertions.assertNull(caps);
    }
    @Test
    void isValidCapOkTest(){
        List<String> caps = Utility.isValidCap(capListOk);
        Assertions.assertNotNull(caps);
        Assertions.assertEquals(17,caps.size());
    }
}