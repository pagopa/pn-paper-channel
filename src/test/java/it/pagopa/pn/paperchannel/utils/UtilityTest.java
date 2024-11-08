package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.model.Address;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class UtilityTest {
    private String capListDuplicate;
    private String capListErrorRange;
    private String capListOk;

    @Test
    void sub(){
        String status = "RECRN005C";
        status = status.substring(0, status.length()-1).concat("A");
        log.info(status);
    }


    @Test
    void convertToHashTest(){
        String testString = "Via MONTE rosA";
        String testString1 = " via monte rosa ";
        String hashString = DigestUtils.sha256Hex(testString);
        String hashString1 = DigestUtils.sha256Hex(testString1);

        String hashResult = Utility.convertToHash(testString);
        String hashResult1 = Utility.convertToHash(testString1);

        assertNotNull(hashResult);
        assertNotNull(hashResult1);

        // check removed white space and string is lowercase
        Assertions.assertEquals(hashResult1, hashResult);

        // check removed white space and string is lowercase
        Assertions.assertNotEquals(hashResult, hashString);
        Assertions.assertNotEquals(hashResult1, hashString1);

    }


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
        assertNotNull(caps);
        Assertions.assertEquals(17,caps.size());
    }

    @Test
    void objectToJsonTest(){
        PnCap cap = new PnCap();
        cap.setCap("00166");
        String json = Utility.objectToJson(cap);
        log.info(json);
    }


    @Test
    void getRequestIdWithoutPrefixClientIdTest(){
        String requestIdTest = ".lll-222333.001-000";
        String result = Utility.getRequestIdWithoutPrefixClientId(requestIdTest);
        assertNotNull(result);
        assertEquals(requestIdTest, result);

        requestIdTest = "001.pippoABC-0033";
        result = Utility.getRequestIdWithoutPrefixClientId(requestIdTest);
        assertNotNull(requestIdTest);
        assertEquals("pippoABC-0033", result);
    }


    @Test
    void getClientIdFromRequestIdTest(){
        String requestIdTest = ".lll-222333.001-000";
        String result = Utility.getClientIdFromRequestId(requestIdTest);
        assertNull(result);

        requestIdTest = "001.pippoABC-0033";
        result = Utility.getClientIdFromRequestId(requestIdTest);
        assertNotNull(requestIdTest);
        assertEquals("001", result);
    }

    @Test
    void getGeokeyCapTest(){
        // Arrange
        Address address = new Address();
        address.setCap("63087");
        address.setCountry("it");

        // Act
        String geokey = Utility.getGeokey(address);

        // Assert
        assertEquals("63087", geokey);
    }

    @Test
    void getGeokeyCountryTest(){
        // Arrange
        Address address = new Address();
        address.setCap("1234");
        address.setCountry("fr");

        // Act
        String geokey = Utility.getGeokey(address);

        // Assert
        assertEquals("fr", geokey);
    }
}