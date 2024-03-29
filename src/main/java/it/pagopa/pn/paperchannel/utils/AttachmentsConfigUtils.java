package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.api.dto.events.ConfigTypeEnum;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static it.pagopa.pn.paperchannel.service.SafeStorageService.SAFESTORAGE_PREFIX;

public class AttachmentsConfigUtils {

    public static final String DELIMITER_PK = "##";

    public static final String ZIPCODE_PK_PREFIX = "ZIP";

    public static final String DOCTAG_QUERY_PARAM = "docTag";

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

    public static String getDocTagFromFileKey(String fileKey) {
        if (fileKey == null) {
            return null;
        }
        var uri = URI.create(fileKey);
        var uriComponents = UriComponentsBuilder.fromUri(uri).build();
        var docTags = uriComponents.getQueryParams().get(DOCTAG_QUERY_PARAM);
        if(CollectionUtils.isEmpty(docTags)) {
            return null;
        }
        return docTags.get(0);
    }

    public static String cleanFileKey(String fileKey) {
        if (fileKey == null) {
            return null;
        }

        StringBuilder fileKeyNew = new StringBuilder();

        if (fileKey.contains(SAFESTORAGE_PREFIX)){
            fileKeyNew.append(fileKey.replace(SAFESTORAGE_PREFIX, ""));
        }
        else {
            fileKeyNew.append(fileKey);
        }

        var queryParamStartIndex = fileKeyNew.indexOf("?");
        if(queryParamStartIndex != -1) {
            fileKeyNew.delete(queryParamStartIndex, fileKeyNew.length());
        }

        return fileKeyNew.toString();
    }
}
