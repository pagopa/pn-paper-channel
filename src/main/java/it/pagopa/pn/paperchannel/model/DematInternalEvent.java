package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AttachmentDetails;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@Builder
public class DematInternalEvent {
    
    private String requestId; //PnDeliveryRequest.getRequestId
    private String statusDetail;//PnDeliveryRequest.getStatusDetail
    private String statusDescription;//PnDeliveryRequest.getStatusDescription
    private OffsetDateTime statusDateTime; //PaperProgressStatusEventDto.getStatusDateTime
    private String statusCode;//PaperProgressStatusEventDto.getStatusCode
    private OffsetDateTime clientRequestTimeStamp; //PaperProgressStatusEventDto.getClientRequestTimeStamp()
    private String registeredLetterCode; //PaperProgressStatusEventDto.getRegisteredLetterCode
    private String deliveryFailureCause; //PaperProgressStatusEventDto.getDeliveryFailureCause
    private AnalogAddress discoveredAddress; //PaperProgressStatusEventDto.getDiscoveredAddress
    private AttachmentDetails attachmentDetails; //PaperProgressStatusEventDto.getAttachments.get(0)
    private int attemptRetry;
}
