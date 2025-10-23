package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CheckAddressResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.service.CheckAddressService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static it.pagopa.pn.paperchannel.utils.AddressTypeEnum.RECEIVER_ADDRESS;

@CustomLog
@Service
@RequiredArgsConstructor
public class CheckAddressServiceImpl implements CheckAddressService {

    private final AddressDAO addressDAO;

    @Override
    public Mono<CheckAddressResponse> checkAddressRequest(String requestId){
        log.info("Finding address for requestId {}", requestId);
        return addressDAO.findByRequestId(requestId, RECEIVER_ADDRESS)
                .flatMap(address -> buildAddressResponse(requestId, true, address.getTtl()))
                .switchIfEmpty(buildAddressResponse(requestId, false, null));
    }

    private Mono<CheckAddressResponse> buildAddressResponse(String requestId, boolean found, Long ttl) {
        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(found);
        response.setRequestId(requestId);
        if (found) {
            response.setEndValidity(Instant.ofEpochSecond(ttl));
        }
        return Mono.just(response);
    }

}