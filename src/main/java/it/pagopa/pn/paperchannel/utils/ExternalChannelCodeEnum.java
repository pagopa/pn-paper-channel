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
    RECRS004C("OK"),
    RECRS005C("OK"),
    RECRS006("PROGRESS"), // furto o smarrimento
    RECRN001C("OK"),
    RECAG002C("OK"),
    RECAG003C("KO"),
    RECAG003F("KO"),
    RECAG004("PROGRESS"),
    RECAG005C("PROGRESS"), // KO or Progress
    RECAG006C("PROGRESS"), // KO or Progress
    RECAG007C("PROGRESS"), // KO or Progress
    RECAG008C("PROGRESS"),
    PNAG012("KO"),
    RECRI003C("OK"),
    RECRI004C("KO"),
    RECRI005("PROGRESS"),
    RECRSI003C("OK"),
    RECRSI004C("KO"),
    RECRSI005("PROGRESS"),
    CON998("KO"),
    CON997("KO"),
    CON996("KO"),
    CON995("KO"),
    CON993("KO");

    private final String message;

    ExternalChannelCodeEnum(String message) {
        this.message = message;
    }

    public static boolean isRetryStatusCode(String code) {
        if (StringUtils.equalsIgnoreCase(code, RECRS006.name())) return true;
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
            log.error("no code found"+statusCode);
        }
        return code;
    }
}
