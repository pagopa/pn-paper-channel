package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.model.Address;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class UtilityTest {
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