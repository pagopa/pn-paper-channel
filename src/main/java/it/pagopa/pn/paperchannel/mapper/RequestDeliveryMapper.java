package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.time.Instant;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.IN_PROCESSING;

public class RequestDeliveryMapper {
    private RequestDeliveryMapper() {
        throw new IllegalStateException();
    }

    public static PnDeliveryRequest toEntity(PrepareRequest request){
        PnDeliveryRequest entity = new PnDeliveryRequest();
        entity.setRequestId(request.getRequestId());
        entity.setProposalProductType(request.getProposalProductType().getValue());
        entity.setStartDate(DateUtils.formatDate(Instant.now()));
        entity.setIun(request.getIun());
        entity.setRelatedRequestId(request.getRelatedRequestId());
        entity.setNotificationSentAt(request.getNotificationSentAt());

        changeState(entity, IN_PROCESSING.getCode(), IN_PROCESSING.getDescription(), IN_PROCESSING.getDetail(), null, null);

        entity.setFiscalCode(request.getReceiverFiscalCode());
        entity.setPrintType(request.getPrintType());
        entity.setReceiverType(request.getReceiverType());
        entity.setAarWithRadd(request.getAarWithRadd());
        entity.setAttachments(request.getAttachmentUrls().stream().map(key -> {
            PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
            pnAttachmentInfo.setFileKey(key);
            pnAttachmentInfo.setDocTag(AttachmentsConfigUtils.getDocTagFromFileKey(key));
            return pnAttachmentInfo;
        }).toList());

        return entity;
    }

    public static void changeState(@NotNull PnDeliveryRequest request, @NotNull String statusCode, @NotNull String statusDescription, @NotNull String statusDetail, String productType, Instant statusDate) {
        request.setStatusCode(statusCode);
        String description = statusCode;
        if (StringUtils.isNotBlank(statusDescription)) {
            description = description.concat(" - ").concat(statusDescription);
        }
        if (StringUtils.isNotBlank(productType)){
            description = productType.concat(" - ").concat(description);
        }
        request.setStatusDescription(description);
        request.setStatusDetail(statusDetail);
        request.setStatusDate(DateUtils.formatDate(Instant.now()));
        if (statusDate != null) {
            request.setStatusDate(DateUtils.formatDate(statusDate));
        }
    }

}
