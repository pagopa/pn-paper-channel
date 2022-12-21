package it.pagopa.pn.paperchannel.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utility {

    private Utility() {
    }

    public static String convertToHash(String string) {

        if(string==null){
            return null;
        }
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        messageDigest.update(string.getBytes());
        String stringHash = new String(messageDigest.digest());
        return stringHash;
    }

}
