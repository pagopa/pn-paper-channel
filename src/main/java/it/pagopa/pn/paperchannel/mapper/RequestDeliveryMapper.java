package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.AttachmentInfoEntity;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.util.Date;
import java.util.stream.Collectors;

public class RequestDeliveryMapper {
    private RequestDeliveryMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static RequestDeliveryEntity toEntity(PrepareRequest request, String correlationId){
        RequestDeliveryEntity entity = new RequestDeliveryEntity();
        entity.setRequestId(request.getRequestId());
        entity.setRegisteredLetterCode(request.getProductType());
        entity.setStartDate(DateUtils.formatDate(new Date()));
        entity.setStatusCode(StatusDeliveryEnum.IN_PROCESSING.getCode());
        entity.setStatusDetail(StatusDeliveryEnum.IN_PROCESSING.getDescription());

        if (correlationId != null){
            entity.setStatusCode(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode());
            entity.setStatusDetail(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDescription());
        }

        entity.setStatusDate(DateUtils.formatDate(new Date()));
        entity.setFiscalCode(request.getReceiverFiscalCode());
        entity.setAddressHash("Hash code");
        entity.setCorrelationId(correlationId);

        if(request.getAttachmentUrls()!= null){
            entity.setAttachments(request.getAttachmentUrls().stream().map(key -> {
                AttachmentInfoEntity attachmentInfoEntity = new AttachmentInfoEntity();
                attachmentInfoEntity.setFileKey(key);
                return attachmentInfoEntity;
            }).collect(Collectors.toList()));
        }

        return entity;
    }

}
