package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.PNRN012_STATUS_CODE;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.PNRN012_STATUS_DESCRIPTION;


@Getter
@Setter
public class PNRN012Wrapper {

    private PnDeliveryRequest pnDeliveryRequestPNRN012;

    private PaperProgressStatusEventDto paperProgressStatusEventDtoPNRN012;

    /**
     *
     * @param originalPnDeliveryRequest the original PnDeliveryRequest
     * @param originalPaperRequest the original PaperRequest
     * @param statusDateTimeToSet the status datetime to set on the newly created PNRN012 event (it is normally set to PNRN012 statusDateTime or PNRN011A + 10 days, based on the context)
     * @return
     */
    public static PNRN012Wrapper buildPNRN012Wrapper(PnDeliveryRequest originalPnDeliveryRequest, PaperProgressStatusEventDto originalPaperRequest,  Instant statusDateTimeToSet) {
        return new PNRN012Wrapper(originalPnDeliveryRequest, originalPaperRequest, statusDateTimeToSet);
    }

    private PNRN012Wrapper(PnDeliveryRequest originalPnDeliveryRequest, PaperProgressStatusEventDto originalPaperRequest, Instant statusDateTimeToSet) {
        // nelle nuova entità PnDeliveryRequest valorizzo solo i campi necessari per SendEvent (evento mandato a delivery-push)
        pnDeliveryRequestPNRN012 = new PnDeliveryRequest();
        pnDeliveryRequestPNRN012.setStatusDetail(StatusCodeEnum.OK.getValue()); //evento finale PROGRESS
        pnDeliveryRequestPNRN012.setStatusCode(originalPnDeliveryRequest.getStatusDetail());
        pnDeliveryRequestPNRN012.setRequestId(originalPnDeliveryRequest.getRequestId());
        pnDeliveryRequestPNRN012.setStatusDescription(PNRN012_STATUS_DESCRIPTION);

        paperProgressStatusEventDtoPNRN012 = new PaperProgressStatusEventDto();
        paperProgressStatusEventDtoPNRN012.setRequestId(originalPaperRequest.getRequestId());
        paperProgressStatusEventDtoPNRN012.setRegisteredLetterCode(originalPaperRequest.getRegisteredLetterCode());
        paperProgressStatusEventDtoPNRN012.setProductType(originalPaperRequest.getProductType());
        paperProgressStatusEventDtoPNRN012.setIun(originalPaperRequest.getIun());
        paperProgressStatusEventDtoPNRN012.setStatusDescription(PNRN012_STATUS_DESCRIPTION);
        paperProgressStatusEventDtoPNRN012.setClientRequestTimeStamp(OffsetDateTime.now());

        paperProgressStatusEventDtoPNRN012.setStatusDateTime(OffsetDateTime.ofInstant(statusDateTimeToSet, ZoneOffset.UTC));
        paperProgressStatusEventDtoPNRN012.setStatusCode(PNRN012_STATUS_CODE);

    }
}
