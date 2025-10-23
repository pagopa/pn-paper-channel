package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static it.pagopa.pn.paperchannel.utils.AddressTypeEnum.RECEIVER_ADDRESS;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckAddressServiceImplTest {

    @Mock
    private AddressDAO addressDAO;

    @InjectMocks
    private CheckAddressServiceImpl checkAddressService;

    @Test
    void checkAddressRequestReturnsFoundResponseWhenAddressExists() {
        String requestId = "12345";
        PnAddress address = new PnAddress();
        address.setTtl(Instant.now().getEpochSecond() + 3600);

        when(addressDAO.findByRequestId(requestId, RECEIVER_ADDRESS)).thenReturn(Mono.just(address));

        StepVerifier.create(checkAddressService.checkAddressRequest(requestId))
                .expectNextMatches(response -> response.getFound() &&
                        response.getRequestId().equals(requestId) &&
                        response.getEndValidity() != null)
                .verifyComplete();
    }

    @Test
    void checkAddressRequestReturnsNotFoundResponseWhenAddressDoesNotExist() {
        String requestId = "12345";

        when(addressDAO.findByRequestId(requestId, RECEIVER_ADDRESS)).thenReturn(Mono.empty());

        StepVerifier.create(checkAddressService.checkAddressRequest(requestId))
                .expectNextMatches(response -> !response.getFound() &&
                        response.getRequestId().equals(requestId) &&
                        response.getEndValidity() == null)
                .verifyComplete();
    }

    @Test
    void checkAddressRequestHandlesNullRequestIdGracefully() {
        String requestId = null;

        when(addressDAO.findByRequestId(requestId, RECEIVER_ADDRESS)).thenReturn(Mono.empty());

        StepVerifier.create(checkAddressService.checkAddressRequest(requestId))
                .expectNextMatches(response -> !response.getFound() &&
                        response.getRequestId() == null &&
                        response.getEndValidity() == null)
                .verifyComplete();
    }
}