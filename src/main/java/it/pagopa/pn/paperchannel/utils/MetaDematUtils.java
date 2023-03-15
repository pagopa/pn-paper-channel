package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;

public class MetaDematUtils {

    public static final String DEMAT_PREFIX = "DEMAT";

    public static final String METADATA_PREFIX = "META";

    public static final String DELIMITER = "##";

    public static final String RECAG011B_STATUS_CODE = "RECAG011B";

    public static final String PNAG012_STATUS_CODE = "PNAG012";

    public static final String RECAG012_STATUS_CODE = "RECAG012";

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


    public static void editPnDeliveryRequestForPNAG012(PnDeliveryRequest entity) {
        entity.setStatusCode(StatusCodeEnum.OK.getValue());
        entity.setStatusDetail("Distacco d'ufficio 23L fascicolo chiuso");
    }


}
