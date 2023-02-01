package it.pagopa.pn.paperchannel.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static boolean isValidFromRegex(String value, String regex){
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(value);
        return m.find() && m.group().equals(value);
    }

    public static Boolean isValidCap(String capList) {
        String regex = "(\\d{5})(-\\d{5})?+";
        if (!isValidFromRegex(capList,regex)){
            return false;
        }
        boolean check = false;
        String[] cap = capList.split(",");
        for (String item : cap) {
            if (item.contains("-")) {
                String[] range = item.trim().split("-");
                int low = Integer.parseInt(range[0]);
                int high = Integer.parseInt(range[1]);
                if (low < high) {
                    check = true;
                    System.out.println("Trovato cap in questo range " + item);
                }
                else {
                    check = false;
                }
            }
        }
        return check;
    }
}
