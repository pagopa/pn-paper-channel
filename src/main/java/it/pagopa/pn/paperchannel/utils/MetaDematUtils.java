package it.pagopa.pn.paperchannel.utils;

public class MetaDematUtils {

    public static final String DEMAT_PREFIX = "DEMAT";

    public static final String METADATA_PREFIX = "META";

    public static final String DELIMITER = "##";

    private MetaDematUtils() {}

    public static String buildMetaRequestId(String requestId) {
        return METADATA_PREFIX + DELIMITER + requestId;
    }

    public static String buildMetaStatusCode(String statusCode) {
        return METADATA_PREFIX + DELIMITER + statusCode;
    }

    public static String buildDematRequestId(String requestId) {
        return DEMAT_PREFIX + DELIMITER + requestId;
    }

    public static String buildDocumentTypeStatusCode(String documentType, String statusCode) {
        return documentType + DELIMITER + statusCode;
    }


}
