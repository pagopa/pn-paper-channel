package it.pagopa.pn.paperchannel.encryption.impl;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class DataVaultEncryptionImplTest extends BaseTest.WithMockServer {

    @Autowired
    DataVaultEncryptionImpl dataVaultEncryption;

    @Test
    void encodeOkTest(){
        String data = dataVaultEncryption.encode("FRMTTR76M06B715E","PF");
        Assertions.assertNotNull(data);
    }
    @Test
    void encodeErrorTest(){
        try{
            String data = dataVaultEncryption.encode("FRMTTR76M06B715A","PF");
        }
        catch (PnGenericException ex) {
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        }

    }

    @Test
    void decodeOkTest(){
        String data = dataVaultEncryption.decode("123e4567-e89b-12d3-a456-426655440000");
        Assertions.assertNotNull(data);
    }

    @Test
    void decodeErrorTest(){
        try{
            String data = dataVaultEncryption.decode("123e4567-e89b-12d3-a456-426655440002");
        }
        catch (PnGenericException ex) {
            Assertions.assertEquals( HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        }
    }


}