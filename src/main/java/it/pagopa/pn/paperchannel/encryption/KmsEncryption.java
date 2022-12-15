package it.pagopa.pn.paperchannel.encryption;

public interface KmsEncryption {


    String encode(String data);

    String decode(String data);

}
