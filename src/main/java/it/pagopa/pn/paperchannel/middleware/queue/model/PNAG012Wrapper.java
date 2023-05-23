package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import lombok.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.PNAG012_STATUS_CODE;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.PNAG012_STATUS_DESCRIPTION;

/**
 * Classe wrapper utilizzata per contenere gli oggetti modificati di PnDeliveryRequest e PaperProgressStatusEventDto
 * per l'evento PNAG012.
 * Prende in input gli eventi originali di PnDeliveryRequest e PaperProgressStatusEventDto e crea due nuovi oggetti
 * dello stesso tipo ma adattati per l'evento di PNAG012.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PNAG012Wrapper {

    private PnDeliveryRequest pnDeliveryRequestPNAG012;

    private PaperProgressStatusEventDto paperProgressStatusEventDtoPNAG012;

    /**
     *
     * @param originalPnDeliveryRequest the original PnDeliveryRequest
     * @param originalPaperRequest the original PaperRequest
     * @param statusDateTimeToSet the status datetime to set on the newly created PNAG012 event (it is normally set to RECAG012 statusDateTime or RECAG011A + 10 days, based on the context)
     * @return
     */
    public static PNAG012Wrapper buildPNAG012Wrapper(PnDeliveryRequest originalPnDeliveryRequest, PaperProgressStatusEventDto originalPaperRequest,  Instant statusDateTimeToSet) {
        return new PNAG012Wrapper(originalPnDeliveryRequest, originalPaperRequest, statusDateTimeToSet);
    }

    private PNAG012Wrapper(PnDeliveryRequest originalPnDeliveryRequest, PaperProgressStatusEventDto originalPaperRequest, Instant statusDateTimeToSet) {
        // nelle nuova entità PnDeliveryRequest valorizzo solo i campi necessari per SendEvent (evento mandato a delivery-push)
        pnDeliveryRequestPNAG012 = new PnDeliveryRequest();
        pnDeliveryRequestPNAG012.setStatusDetail(StatusCodeEnum.OK.getValue()); //evento finale OK
        pnDeliveryRequestPNAG012.setStatusCode(originalPnDeliveryRequest.getStatusDetail());
        pnDeliveryRequestPNAG012.setRequestId(originalPnDeliveryRequest.getRequestId());

        // nelle nuova entità PaperProgressStatusEventDto valorizzo tutti i campi poiché poi questo oggetto
        // deve essere loggato negli AUDIT LOG
        paperProgressStatusEventDtoPNAG012 = new PaperProgressStatusEventDto();
        paperProgressStatusEventDtoPNAG012.setRequestId(originalPaperRequest.getRequestId());
        paperProgressStatusEventDtoPNAG012.setRegisteredLetterCode(originalPaperRequest.getRegisteredLetterCode());
        paperProgressStatusEventDtoPNAG012.setProductType(originalPaperRequest.getProductType());
        paperProgressStatusEventDtoPNAG012.setIun(originalPaperRequest.getIun());
        paperProgressStatusEventDtoPNAG012.setStatusDescription(PNAG012_STATUS_DESCRIPTION);
        paperProgressStatusEventDtoPNAG012.setClientRequestTimeStamp(OffsetDateTime.now());

        paperProgressStatusEventDtoPNAG012.setStatusDateTime(OffsetDateTime.ofInstant(statusDateTimeToSet, ZoneOffset.UTC));
        paperProgressStatusEventDtoPNAG012.setStatusCode(PNAG012_STATUS_CODE);

    }
}
