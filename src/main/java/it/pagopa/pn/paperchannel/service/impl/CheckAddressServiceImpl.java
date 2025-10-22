package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CheckAddressResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.service.CheckAddressService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
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
                .flatMap(address -> buildAddressFoundResponse(address, requestId))
                .switchIfEmpty(buildAddressNotFoundResponse(requestId));
    }

    private Mono<CheckAddressResponse> buildAddressFoundResponse(PnAddress address, String requestId) {
        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(true);
        response.setRequestId(requestId);
        response.setEndValidity(Instant.ofEpochSecond(address.getTtl()));
        return Mono.just(response);
    }

    private Mono<CheckAddressResponse> buildAddressNotFoundResponse(String requestId) {
        CheckAddressResponse response = new CheckAddressResponse();
        response.setFound(false);
        response.setRequestId(requestId);
        return Mono.just(response);
    }

}