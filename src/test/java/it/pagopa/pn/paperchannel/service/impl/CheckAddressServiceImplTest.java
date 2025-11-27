package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CheckAddressResponse;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static it.pagopa.pn.paperchannel.utils.AddressTypeEnum.RECEIVER_ADDRESS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckAddressServiceImplTest {

    @Mock
    private AddressDAO addressDAO;

    @Mock
    private PcRetryServiceImpl pcRetryService;

    @InjectMocks
    private CheckAddressServiceImpl checkAddressService;

    @Test
    void checkAddressRequestReturnsFoundResponseWhenAddressExists() {
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_IUN-PROVA-prova.RECINDEX_0.ATTEMPT_0.PCRETRY_0";
        PnAddress address = new PnAddress();
        address.setTtl(Instant.now().getEpochSecond() + 3600);

        when(addressDAO.findByRequestId("PREPARE_ANALOG_DOMICILE.IUN_IUN-PROVA-prova.RECINDEX_0.ATTEMPT_0", RECEIVER_ADDRESS)).thenReturn(Mono.just(address));
        when(pcRetryService.getPrefixRequestId(anyString())).thenReturn("PREPARE_ANALOG_DOMICILE.IUN_IUN-PROVA-prova.RECINDEX_0.ATTEMPT_0");

        StepVerifier.create(checkAddressService.checkAddressRequest(requestId))
                .expectNextMatches(response -> {
                    Assertions.assertNotNull(response.getRequestId());
                    return response.getRequestId().equals(requestId) &&
                            response.getEndValidity() != null;
                })
                .verifyComplete();
    }

    @Test
    void checkAddressRequestReturnsNotFoundResponseWhenAddressDoesNotExist() {
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_IUN-PROVA-prova.RECINDEX_0.ATTEMPT_0.PCRETRY_0";

        when(addressDAO.findByRequestId("PREPARE_ANALOG_DOMICILE.IUN_IUN-PROVA-prova.RECINDEX_0.ATTEMPT_0", RECEIVER_ADDRESS)).thenReturn(Mono.empty());
        when(pcRetryService.getPrefixRequestId(anyString())).thenReturn("PREPARE_ANALOG_DOMICILE.IUN_IUN-PROVA-prova.RECINDEX_0.ATTEMPT_0");

        CheckAddressResponse checkAddressResponse = checkAddressService.checkAddressRequest(requestId).block();

        Assertions.assertNull(checkAddressResponse);
    }

    @Test
    void checkAddressRequestHandlesNullRequestIdGracefully() {

        when(addressDAO.findByRequestId(null, RECEIVER_ADDRESS)).thenReturn(Mono.empty());

        CheckAddressResponse checkAddressResponse = checkAddressService.checkAddressRequest(null).block();

        Assertions.assertNull(checkAddressResponse);
    }
}