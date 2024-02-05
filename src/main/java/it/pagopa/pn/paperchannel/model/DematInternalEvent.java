package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AttachmentDetails;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Internal Demat event that is currently used to manage demats that contain ZIPs in the attachments.
 * The object of type DematInternalEvent is created from {@link it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto}
 * and {@link it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest}, thanks to the {@link it.pagopa.pn.paperchannel.mapper.DematInternalEventMapper}
 */
@Data
@Builder
public class DematInternalEvent {
    
    private String requestId; //PnDeliveryRequest.getRequestId
    private String iun; //PnDeliveryRequest.getIun
    private String statusDetail;//PnDeliveryRequest.getStatusDetail
    private String statusDescription;//PnDeliveryRequest.getStatusDescription
    private String extChannelRequestId; //PaperProgressStatusEventDto.getRequestId
    private OffsetDateTime statusDateTime; //PaperProgressStatusEventDto.getStatusDateTime
    private String statusCode;//PaperProgressStatusEventDto.getStatusCode
    private OffsetDateTime clientRequestTimeStamp; //PaperProgressStatusEventDto.getClientRequestTimeStamp()
    private String registeredLetterCode; //PaperProgressStatusEventDto.getRegisteredLetterCode
    private String deliveryFailureCause; //PaperProgressStatusEventDto.getDeliveryFailureCause
    private AnalogAddress discoveredAddress; //PaperProgressStatusEventDto.getDiscoveredAddress
    private AttachmentDetails attachmentDetails; //PaperProgressStatusEventDto.getAttachments.get(0)
    private int attemptRetry;
    private String errorMessage;
}
