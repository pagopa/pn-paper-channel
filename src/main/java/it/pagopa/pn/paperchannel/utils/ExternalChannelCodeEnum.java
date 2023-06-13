package it.pagopa.pn.paperchannel.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Getter
@Slf4j
public enum ExternalChannelCodeEnum {
    CON080(Constants.PROGRESS),
    RECRS001C(Constants.OK),
    RECRS002C(Constants.KO),
    RECRS002F(Constants.KO),
    RECRS003C(Constants.OK),
    RECRS004C(Constants.KO),
    RECRS005C(Constants.KO),
    RECRN006(Constants.PROGRESS), // furto o smarrimento o rapinato
    RECRS006(Constants.PROGRESS), // furto o smarrimento o rapinato
    RECRS013(Constants.PROGRESS), // furto o smarrimento o rapinato
    RECRN013(Constants.PROGRESS), // furto o smarrimento o rapinato
    RECRS015(Constants.PROGRESS),
    RECRN015(Constants.PROGRESS),
    RECAG015(Constants.PROGRESS),
    RECAG001C(Constants.OK),
    RECRN001C(Constants.OK),
    RECRN002C(Constants.OK),
    RECRN002F(Constants.KO),
    RECRN003C(Constants.OK),
    RECRN004C(Constants.OK),
    RECRN005C(Constants.KO),
    RECAG002C(Constants.OK),
    RECAG003C(Constants.OK),
    RECAG003F(Constants.KO),
    RECAG004(Constants.PROGRESS), // furto o smarrimento
    RECAG013(Constants.PROGRESS),
    RECAG005C(Constants.OK),
    RECAG006C(Constants.OK),
    RECAG007C(Constants.OK),
    RECAG008C(Constants.PROGRESS),
    PNAG012(Constants.KO),
    RECRI001(Constants.PROGRESS),
    RECRI002(Constants.PROGRESS),
    RECRI003C(Constants.OK),
    RECRI004C(Constants.OK),
    RECRSI003C(Constants.OK),
    RECRSI004C(Constants.KO),
    RECRSI005(Constants.PROGRESS), // furto o smarrimento
    RECRI005(Constants.PROGRESS), // furto o smarrimento
    CON998(Constants.KO),
    CON997(Constants.KO),
    CON996(Constants.KO),
    CON995(Constants.KO),
    CON993(Constants.KO),

    RECRN011(Constants.PROGRESS),

    //META STATUS CODE
    RECRN003A(Constants.PROGRESS),
    RECRN004A(Constants.PROGRESS),
    RECRN005A(Constants.PROGRESS),


    //DEMAT STATUS CODE
    RECRS002B(Constants.PROGRESS),
    RECRS002E(Constants.PROGRESS),
    RECRS004B(Constants.PROGRESS),
    RECRS005B(Constants.PROGRESS),
    RECRN001B(Constants.PROGRESS),
    RECRN002B(Constants.PROGRESS),
    RECRN002E(Constants.PROGRESS),
    RECRN003B(Constants.PROGRESS),
    RECRN004B(Constants.PROGRESS),
    RECRN005B(Constants.PROGRESS),
    RECAG001B(Constants.PROGRESS),
    RECAG002B(Constants.PROGRESS),
    RECAG003B(Constants.PROGRESS),
    RECAG003E(Constants.PROGRESS),
    RECRI003B(Constants.PROGRESS),
    RECRI004B(Constants.PROGRESS),
    RECRSI004B(Constants.PROGRESS),
    RECAG005B(Constants.PROGRESS),
    RECAG006B(Constants.PROGRESS),
    RECAG007B(Constants.PROGRESS),
    RECAG008B(Constants.PROGRESS),

    //890
    RECAG011B(Constants.PROGRESS);

    private final String message;

    ExternalChannelCodeEnum(String message) {
        this.message = message;
    }

    public static boolean isRetryStatusCode(String code) {
        return StringUtils.equalsIgnoreCase(code, RECRS006.name())
                || StringUtils.equalsIgnoreCase(code, RECRN006.name())
                || StringUtils.equalsIgnoreCase(code, RECAG004.name())
                || StringUtils.equalsIgnoreCase(code, RECRI005.name())
                || StringUtils.equalsIgnoreCase(code, RECRSI005.name())
                || StringUtils.equalsIgnoreCase(code, RECRS013.name())
                || StringUtils.equalsIgnoreCase(code, RECRN013.name())
                || StringUtils.equalsIgnoreCase(code, RECAG013.name());
    }

    public static boolean isErrorStatusCode(String code) {
        return StringUtils.equalsIgnoreCase(code, CON998.name())
                || StringUtils.equalsIgnoreCase(code, CON997.name())
                || StringUtils.equalsIgnoreCase(code, CON996.name())
                || StringUtils.equalsIgnoreCase(code, CON995.name())
                || StringUtils.equalsIgnoreCase(code, CON993.name());
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

    private static class Constants {
        private static final String PROGRESS = "PROGRESS";
        private static final String OK = "OK";
        private static final String KO = "KO";
    }
}
