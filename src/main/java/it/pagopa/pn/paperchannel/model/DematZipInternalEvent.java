package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@Builder
public class DematZipInternalEvent {
    
    private String requestId; //PaperProgressStatusEventDto.getRequestId
    private OffsetDateTime statusDateTime; //PaperProgressStatusEventDto.getStatusDateTime
    private String statusCode;//PaperProgressStatusEventDto.getStatusCode
    private OffsetDateTime clientRequestTimeStamp; //PaperProgressStatusEventDto.getClientRequestTimeStamp()
    private String deliveryFailureCause; //PaperProgressStatusEventDto.getDeliveryFailureCause
    private AnalogAddress discoveredAddress; //PaperProgressStatusEventDto.getDiscoveredAddress
    private String attachmentDocumentType; //AttachmentDetailsDto.getDocumentType
    private OffsetDateTime attachmentDate; //AttachmentDetailsDto.getDate
    private String attachmentUri; //AttachmentDetailsDto.getUri
    private int attemptRetry;
}
