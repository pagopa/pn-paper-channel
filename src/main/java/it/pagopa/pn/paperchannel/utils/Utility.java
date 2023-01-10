package it.pagopa.pn.paperchannel.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

@Slf4j
public class Utility {

    private Utility() {
        throw new IllegalCallerException();
    }

    public static String convertToHash(String string) {
        if(string==null){
            return null;
        }

        String stringHash = DigestUtils.sha256Hex(string);
        return stringHash;
    }

    public static <T> T jsonToObject(ObjectMapper objectMapper, String json, Class<T> tClass){
        try {

            return objectMapper.readValue(json, tClass);
        } catch (JsonProcessingException e) {
            log.error("Error with mapping : {}", e.getMessage());
            return null;
        }
    }

}
