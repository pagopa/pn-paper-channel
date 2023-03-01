package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ExternalChannelClient {


    Mono<Void> sendEngageRequest(SendRequest request, List<AttachmentInfo> attachments);


}
