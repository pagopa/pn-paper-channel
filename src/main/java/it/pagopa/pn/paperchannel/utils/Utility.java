package it.pagopa.pn.paperchannel.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        boolean check = false;
        if (StringUtils.isEmpty(value)) {
            check = false;
        } else {
            value = value.replace(".", "");
            value = value.replace(",", "");
            value = value.replace("'", "");
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(value);
            check = (m.find() && m.group().equals(value));
        }
        return check;
    }

    public static boolean isValidCapFromRegex(String value, String regex){
        if (StringUtils.isNotEmpty(value) && value.contains(".")) value = value.substring(0, value.indexOf("."));
        return isValidFromRegex(value, regex);
    }

    public static List<String> isValidCap(String capList) {
        String regex = Const.capRegex;
        List<String> capsFinded = new ArrayList<>();
        boolean check = false;
        String[] cap = capList.split(",");
        if (cap != null && cap.length > 0) {
            for (String item : cap) {
                if (item.contains("-")) {
                    String[] range = item.trim().split("-");
                    int low = Integer.parseInt(range[0]);
                    int high = Integer.parseInt(range[1]);
                    if (!isValidCapFromRegex(range[0],regex) || !isValidCapFromRegex(range[1],regex)) {
                        log.info("Il cap non è conforme agli standard previsti.");
                        return null;
                    }
                    if (low < high) {
                        log.info("Trovato cap in questo range " + item);
                        for (int i = low; i < high; i++){
                            String capFormatted = addZero(i);
                            capsFinded.add(capFormatted);
                        }
                    } else {
                        log.info("Intervallo errato.");
                        return null;
                    }
                } else {
                    String cap_formatted;
                    cap_formatted = item.replace(".0", "");
                    if (cap_formatted.length() < 5) {
                        int n_cap = Integer.parseInt(cap_formatted);
                        cap_formatted = addZero(n_cap);
                        check = isValidCapFromRegex(cap_formatted,regex);
                        if (check){
                            capsFinded.add(cap_formatted);
                        }
                    }
                    else {
                        check = isValidCapFromRegex(cap_formatted, regex);
                        if (check) {
                            capsFinded.add(cap_formatted);
                        }
                    }
                    if (!check) return null;
                }
            }
            Set<String> findedEquals = new HashSet<>(capsFinded);
            findedEquals.addAll(capsFinded);
            if (findedEquals.size() == capsFinded.size()){
                log.info("Non è stato trovato alcun cap duplicato.");
                return capsFinded;
            }
            else{
                log.info("Trovato cap duplicato.");
                return null;
            }
        } else {
            check = isValidCapFromRegex(capList,regex);
        }
        return capsFinded;
    }

    public static String addZero (int i){
        String str = String.format("%05d", i);
        return str;
    }
}
