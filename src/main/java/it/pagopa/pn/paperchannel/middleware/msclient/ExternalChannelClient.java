package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;

import reactor.core.publisher.Mono;

import java.util.List;

public interface ExternalChannelClient {


    Mono<Void> sendEngageRequest(SendRequest request, List<AttachmentInfo> attachments, Boolean applyRasterization);

    Mono<Void> patchRequestMetadata(String requestIdx, boolean isOpenReworkRequest);

}
