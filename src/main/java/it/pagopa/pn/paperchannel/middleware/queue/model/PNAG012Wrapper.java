package it.pagopa.pn.paperchannel.middleware.queue.model;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import lombok.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.PNAG012_STATUS_CODE;

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

    public static PNAG012Wrapper buildPNAG012Wrapper(PnDeliveryRequest originalPnDeliveryRequest, PaperProgressStatusEventDto originalPaperRequest,  Instant statusDateTimeRECAG012) {
        return new PNAG012Wrapper(originalPnDeliveryRequest, originalPaperRequest, statusDateTimeRECAG012);
    }

    private PNAG012Wrapper(PnDeliveryRequest originalPnDeliveryRequest, PaperProgressStatusEventDto originalPaperRequest, Instant statusDateTimeRECAG012) {
        // nelle nuova entità PnDeliveryRequest valorizzo solo i campi necessari per SendEvent (evento mandato a delivery-push)
        pnDeliveryRequestPNAG012 = new PnDeliveryRequest();
        pnDeliveryRequestPNAG012.setStatusCode(StatusCodeEnum.OK.getValue()); //evento finale OK
        pnDeliveryRequestPNAG012.setStatusDetail("Distacco d'ufficio 23L fascicolo chiuso");
        pnDeliveryRequestPNAG012.setStatusDetail(originalPnDeliveryRequest.getStatusDetail());
        pnDeliveryRequestPNAG012.setRequestId(originalPnDeliveryRequest.getRequestId());

        // nelle nuova entità PaperProgressStatusEventDto valorizzo tutti i campi poiché poi questo oggetto
        // deve essere loggato negli AUDIT LOG
        paperProgressStatusEventDtoPNAG012 = new PaperProgressStatusEventDto();
        paperProgressStatusEventDtoPNAG012.setRequestId(originalPaperRequest.getRequestId());
        paperProgressStatusEventDtoPNAG012.setRegisteredLetterCode(originalPaperRequest.getRegisteredLetterCode());
        paperProgressStatusEventDtoPNAG012.setProductType(originalPaperRequest.getProductType());
        paperProgressStatusEventDtoPNAG012.setIun(originalPaperRequest.getIun());
        paperProgressStatusEventDtoPNAG012.setStatusDescription(originalPaperRequest.getStatusDescription());
        paperProgressStatusEventDtoPNAG012.setClientRequestTimeStamp(originalPaperRequest.getClientRequestTimeStamp());
        paperProgressStatusEventDtoPNAG012.setDeliveryFailureCause(originalPaperRequest.getDeliveryFailureCause());
        paperProgressStatusEventDtoPNAG012.setDiscoveredAddress(originalPaperRequest.getDiscoveredAddress());
        paperProgressStatusEventDtoPNAG012.setAttachments(originalPaperRequest.getAttachments());

        paperProgressStatusEventDtoPNAG012.setStatusDateTime(OffsetDateTime.ofInstant(statusDateTimeRECAG012, ZoneOffset.UTC));
        paperProgressStatusEventDtoPNAG012.setStatusCode(PNAG012_STATUS_CODE);

    }
}
