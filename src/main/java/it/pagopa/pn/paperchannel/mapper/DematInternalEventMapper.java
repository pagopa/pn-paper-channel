package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.DematInternalEvent;

public class DematInternalEventMapper {

    private DematInternalEventMapper() {}


    public static DematInternalEvent toDematInternalEvent(PnDeliveryRequest entity,
                                                          PaperProgressStatusEventDto paperRequest) {


        return DematInternalEvent.builder()
                .requestId(entity.getRequestId())
                .iun(entity.getIun())
                .statusDetail(entity.getStatusDetail())
                .statusDescription(entity.getStatusDescription())
                .statusDateTime(paperRequest.getStatusDateTime())
                .statusCode(paperRequest.getStatusCode())
                .clientRequestTimeStamp(paperRequest.getClientRequestTimeStamp())
                .registeredLetterCode(paperRequest.getRegisteredLetterCode())
                .deliveryFailureCause(paperRequest.getDeliveryFailureCause())
                .discoveredAddress(AddressMapper.toPojo(paperRequest.getDiscoveredAddress()))
                .attachmentDetails(AttachmentMapper.fromAttachmentDetailsDto(paperRequest.getAttachments().get(0)))
                .build();

    }
}
