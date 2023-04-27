package it.pagopa.pn.paperchannel.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Getter
@Slf4j
public enum ExternalChannelCodeEnum {
    CON080("PROGRESS"),
    RECRS001C("OK"),
    RECRS002C("KO"),
    RECRS002F("KO"),
    RECRS003C("OK"),
    RECRS004C("KO"),
    RECRS005C("KO"),
    RECRN006("PROGRESS"), // furto o smarrimento
    RECRS006("PROGRESS"), // furto o smarrimento
    RECAG001C("OK"),
    RECRN001C("OK"),
    RECRN002C("KO"),
    RECRN002F("KO"),
    RECRN003C("OK"),
    RECRN004C("KO"),
    RECRN005C("KO"),
    RECAG002C("OK"),
    RECAG003C("KO"),
    RECAG003F("KO"),
    RECAG004("PROGRESS"), // furto o smarrimento
    RECAG005C("OK"),
    RECAG006C("OK"),
    RECAG007C("KO"),
    RECAG008C("PROGRESS"),
    PNAG012("KO"),
    RECRI003C("OK"),
    RECRI004C("KO"),
    RECRSI003C("OK"),
    RECRSI004C("KO"),
    RECRSI005("PROGRESS"), // furto o smarrimento
    RECRI005("PROGRESS"), // furto o smarrimento
    CON998("KO"),
    CON997("KO"),
    CON996("KO"),
    CON995("KO"),
    CON993("KO"),

    //DEMAT STATUS CODE
    RECRS002B("PROGRESS"),
    RECRS002E("PROGRESS"),
    RECRS004B("PROGRESS"),
    RECRS005B("PROGRESS"),
    RECRN001B("PROGRESS"),
    RECRN002B("PROGRESS"),
    RECRN002E("PROGRESS"),
    RECRN003B("PROGRESS"),
    RECRN004B("PROGRESS"),
    RECRN005B("PROGRESS"),
    RECAG001B("PROGRESS"),
    RECAG002B("PROGRESS"),
    RECAG003B("PROGRESS"),
    RECAG003E("PROGRESS"),
    RECRI003B("PROGRESS"),
    RECRI004B("PROGRESS"),
    RECRSI004B("PROGRESS"),
    RECAG005B("PROGRESS"),
    RECAG006B("PROGRESS"),
    RECAG007B("PROGRESS"),
    RECAG008B("PROGRESS"),

    //890
    RECAG011B("PROGRESS");

    private final String message;

    ExternalChannelCodeEnum(String message) {
        this.message = message;
    }

    public static boolean isRetryStatusCode(String code) {
        if (StringUtils.equalsIgnoreCase(code, RECRS006.name())
            || StringUtils.equalsIgnoreCase(code, RECRN006.name())
            || StringUtils.equalsIgnoreCase(code, RECAG004.name())
            || StringUtils.equalsIgnoreCase(code, RECRI005.name())
            || StringUtils.equalsIgnoreCase(code, RECRSI005.name())
        ) return true;
        return false;
    }

    public static boolean isErrorStatusCode(String code) {
        if (StringUtils.equalsIgnoreCase(code, CON998.name())
        || StringUtils.equalsIgnoreCase(code, CON997.name())
        || StringUtils.equalsIgnoreCase(code, CON996.name())
        || StringUtils.equalsIgnoreCase(code, CON995.name())
        || StringUtils.equalsIgnoreCase(code, CON993.name())) return true;
        return false;
    }

    public static String getStatusCode(String statusCode) {
        String code = statusCode;
        try {
            code = ExternalChannelCodeEnum.valueOf(statusCode).getMessage();
        } catch (Exception e) {
            log.info("no code found "+statusCode);
        }
        return code;
    }
}
