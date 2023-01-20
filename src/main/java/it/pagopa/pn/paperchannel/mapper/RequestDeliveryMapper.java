package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.util.Date;

public class RequestDeliveryMapper {
    private RequestDeliveryMapper() {
        throw new IllegalStateException();
    }

    public static PnDeliveryRequest toEntity(PrepareRequest request, String correlationId){
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(request.getRequestId());
        entity.setProposalProductType(request.getProposalProductType().getValue());
        entity.setStartDate(DateUtils.formatDate(new Date()));
        entity.setStatusCode(StatusDeliveryEnum.IN_PROCESSING.getCode());
        entity.setStatusDetail(StatusDeliveryEnum.IN_PROCESSING.getDescription());
        entity.setIun(request.getIun());

        if (correlationId != null){
            entity.setStatusCode(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode());
            entity.setStatusDetail(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDescription());
        }

        entity.setStatusDate(DateUtils.formatDate(new Date()));
        entity.setFiscalCode(request.getReceiverFiscalCode());
        entity.setCorrelationId(correlationId);
        entity.setPrintType(request.getPrintType());
        entity.setReceiverType(request.getReceiverType());
        entity.setAttachments(request.getAttachmentUrls().stream().map(key -> {
            PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
            pnAttachmentInfo.setFileKey(key);
            return pnAttachmentInfo;
        }).toList());

        return entity;
    }

}
