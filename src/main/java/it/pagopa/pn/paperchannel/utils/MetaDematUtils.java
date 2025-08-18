package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;

import java.util.Map;


public class MetaDematUtils {

    public static final String DEMAT_PREFIX = "DEMAT";

    public static final String METADATA_PREFIX = "META";

    public static final String DELIMITER = "##";

    public static final String RECAG011B_STATUS_CODE = "RECAG011B";

    public static final String RECAG008B_STATUS_CODE = "RECAG008B";

    public static final String PNAG012_STATUS_CODE = "PNAG012";

    public static final String RECAG012_STATUS_CODE = "RECAG012";
    public static final String PNRN012_STATUS_CODE = "PNRN012";

    public static final String RECAG011A_STATUS_CODE = "RECAG011A";

    public static final String RECRN011_STATUS_CODE = "RECRN011";
    public static final String RECRN010_STATUS_CODE = "RECRN010";

    public static final String PNAG012_STATUS_DESCRIPTION = "Distacco d'ufficio 23L fascicolo chiuso";
    public static final String PNRN012_STATUS_DESCRIPTION = "Perfezionamento d’ufficio AR";

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

    public static PnEventMeta createMETAForPNAG012Event(PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMetaRECAG012, Long ttlDaysMeta) {
        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(buildMetaRequestId(paperRequest.getRequestId()));
        pnEventMeta.setMetaStatusCode(buildMetaStatusCode(PNAG012_STATUS_CODE));
        pnEventMeta.setTtl(paperRequest.getStatusDateTime().plusDays(ttlDaysMeta).toEpochSecond());

        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setStatusCode(PNAG012_STATUS_CODE);
        pnEventMeta.setStatusDateTime(pnEventMetaRECAG012.getStatusDateTime());
        return pnEventMeta;
    }

    // simulo lo stesso log di evento ricevuto da ext-channels
    public static void logSuccessAuditLog(PaperProgressStatusEventDto paperRequest, PnDeliveryRequest entity, PnLogAudit pnLogAudit) {
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto().analogMail(paperRequest);
        pnLogAudit.addsSuccessReceive(entity.getIun(),
                String.format("prepare requestId = %s Response %s from external-channel status code %s",
                        entity.getRequestId(), singleStatusUpdateDto.toString().replaceAll("\n", ""), entity.getStatusCode()));
    }

    /**
     * Converts a final status code to its Meta equivalent.
     * Replaces the last character:
     * - 'C' -> 'A'
     * - 'F' -> 'D'
     *
     * @param statusCode the original status code ending with 'C' or 'F'
     * @return the transformed status code
     * @throws IllegalArgumentException if the input is null, empty, or ends with an unsupported character
     */
    public static String changeStatusCodeToMeta(String statusCode) {
        return changeLastChar(statusCode, Map.of('C', 'A', 'F', 'D'));
    }

    /**
     * Converts a final status code to its Demat equivalent.
     * Replaces the last character:
     * - 'C' -> 'B'
     * - 'F' -> 'E'
     *
     * @param statusCode the original status code ending with 'C' or 'F'
     * @return the transformed status code
     * @throws IllegalArgumentException if the input is null, empty, or ends with an unsupported character
     */
    public static String changeStatusCodeToDemat(String statusCode) {
        return changeLastChar(statusCode, Map.of('C', 'B', 'F', 'E'));
    }

    /**
     * Replaces the last character of the input string based on a given mapping.
     *
     * @param code the input string
     * @param replacements a map defining which characters to replace and with what
     * @return the input string with its last character replaced accordingly
     * @throws IllegalArgumentException if the input is null, empty, or the last character has no replacement
     */
    private static String changeLastChar(String code, Map<Character, Character> replacements) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("invalid statusCode");
        }

        char lastChar = code.charAt(code.length() - 1);
        Character newChar = replacements.get(lastChar);

        if (newChar == null) {
            throw new IllegalArgumentException("Last char not handled: " + lastChar);
        }

        return code.substring(0, code.length() - 1) + newChar;
    }
}
