package it.pagopa.pn.paperchannel.model;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrepareRequestInt {
    private CommunicationType communicationType;
    private String clientId;
    private List<String> attachmentUrls = new ArrayList<>();
    private PrintType printType;
    private ProposalTypeEnum proposalProductType;
    private String requestId;
    private String receiverFiscalCode;
    private String receiverType;
    private AnalogAddress receiverAddress;
    private String iun;
    private String relatedRequestId;
    private AnalogAddress discoveredAddress;
    private Instant notificationSentAt;
    private String senderPaId;
    private Boolean aarWithRadd;
}
