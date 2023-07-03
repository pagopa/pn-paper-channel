package it.pagopa.pn.paperchannel.encryption;

public interface DataEncryption {
    default String encode(String data) { return data; }
    default String encode(String data, String type) { return data; }
    String decode(String data);
}
