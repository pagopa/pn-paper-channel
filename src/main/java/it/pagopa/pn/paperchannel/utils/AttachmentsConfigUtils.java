package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.api.dto.events.ConfigTypeEnum;

public class AttachmentsConfigUtils {

    public static final String DELIMITER_PK = "##";

    public static final String ZIPCODE_PK_PREFIX = "ZIP";

    private AttachmentsConfigUtils() {}


    public static String buildPartitionKey(String configKey, String configType) {
        if(ConfigTypeEnum.ZIPCODE.name().equals(configType)) {
            return ZIPCODE_PK_PREFIX + DELIMITER_PK + configKey;
        }
        else if(configType != null) {
            return configType + DELIMITER_PK + configKey;
        }
        else {
            return configKey;
        }
    }
}
