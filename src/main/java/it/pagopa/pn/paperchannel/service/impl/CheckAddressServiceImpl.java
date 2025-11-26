package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CheckAddressResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.service.CheckAddressService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

import static it.pagopa.pn.paperchannel.utils.AddressTypeEnum.RECEIVER_ADDRESS;

@CustomLog
@Service
@RequiredArgsConstructor
public class CheckAddressServiceImpl implements CheckAddressService {

    private final AddressDAO addressDAO;
    private final PcRetryServiceImpl pcRetryService;


    @Override
    public Mono<CheckAddressResponse> checkAddressRequest(String requestId){
        log.info("Finding address for requestId {}", requestId);
        return addressDAO.findByRequestId(pcRetryService.getPrefixRequestId(requestId), RECEIVER_ADDRESS)
                .flatMap(address -> buildAddressResponse(requestId, address.getTtl()));
    }

    private Mono<CheckAddressResponse> buildAddressResponse(String requestId, Long ttl) {
        CheckAddressResponse response = new CheckAddressResponse();
        response.setRequestId(requestId);
        if (Objects.nonNull(ttl)) {
            response.setEndValidity(Instant.ofEpochSecond(ttl));
        }
        return Mono.just(response);
    }

}