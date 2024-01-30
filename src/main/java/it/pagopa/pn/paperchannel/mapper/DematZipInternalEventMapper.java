package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.model.DematZipInternalEvent;

public class DematZipInternalEventMapper {

    private DematZipInternalEventMapper() {}


    public static DematZipInternalEvent toDematZipInternalEvent(PaperProgressStatusEventDto paperRequest,
                                                                AttachmentDetailsDto attachmentDetailsDto) {


        return DematZipInternalEvent.builder()
                .requestId(paperRequest.getRequestId())
                .statusDateTime(paperRequest.getStatusDateTime())
                .statusCode(paperRequest.getStatusCode())
                .clientRequestTimeStamp(paperRequest.getClientRequestTimeStamp())
                .deliveryFailureCause(paperRequest.getDeliveryFailureCause())
                .discoveredAddress(AddressMapper.toPojo(paperRequest.getDiscoveredAddress()))
                .attachmentDocumentType(attachmentDetailsDto.getDocumentType())
                .attachmentDate(attachmentDetailsDto.getDate())
                .attachmentUri(attachmentDetailsDto.getUri())
                .build();

    }
}
