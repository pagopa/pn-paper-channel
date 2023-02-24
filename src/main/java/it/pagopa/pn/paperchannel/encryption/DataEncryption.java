package it.pagopa.pn.paperchannel.encryption;

public interface DataEncryption {
    default String encode(String data) { return null; }
    default String encode(String data, String type) { return null; }
    String decode(String data);
    default String decodes(String data) { return null; }
}
