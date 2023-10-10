package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnf24.v1.dto.RequestAcceptedDto;
import reactor.core.publisher.Mono;

public interface F24Client {

    Mono<RequestAcceptedDto> preparePDF(String requestId, String setId, String recipientIndex, Integer cost);
}
