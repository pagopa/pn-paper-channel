package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.msclient.RaddAltClient;
import it.pagopa.pn.paperchannel.model.RaddSearchMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class RaddAltServiceImplTest {
    @Mock
    private RaddAltClient raddAltClient;

    @InjectMocks
    private RaddAltServiceImpl service;

    @Test
    void isAreaCoveredReturnsTrueWhenCoverageExists() {
        RaddSearchMode searchMode = RaddSearchMode.LIGHT;
        PnAddress address = new PnAddress();
        address.setCap("00100");
        address.setCity("Rome");
        address.setRequestId("req-1");
        Instant searchDate = Instant.now();

        CheckCoverageResponseDto responseDto = new CheckCoverageResponseDto();
        responseDto.setHasCoverage(true);

        Mockito.when(raddAltClient.checkCoverage(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(Mono.just(responseDto));

        StepVerifier.create(service.isAreaCovered(searchMode, address, searchDate))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void isAreaCoveredReturnsFalseWhenCoverageDoesNotExist() {
        RaddSearchMode searchMode = RaddSearchMode.LIGHT;
        PnAddress address = new PnAddress();
        address.setCap("00100");
        address.setCity("Rome");
        address.setRequestId("req-2");
        Instant searchDate = Instant.now();

        CheckCoverageResponseDto responseDto = new CheckCoverageResponseDto();
        responseDto.setHasCoverage(false);

        Mockito.when(raddAltClient.checkCoverage(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(Mono.just(responseDto));

        StepVerifier.create(service.isAreaCovered(searchMode, address, searchDate))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isAreaCoveredEmitsErrorWhenClientThrows() {
        RaddSearchMode searchMode = RaddSearchMode.LIGHT;
        PnAddress address = new PnAddress();
        address.setCap("00100");
        address.setCity("Rome");
        address.setRequestId("req-3");
        Instant searchDate = Instant.now();

        Mockito.when(raddAltClient.checkCoverage(
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        )).thenReturn(Mono.error(new RuntimeException("Client error")));

        StepVerifier.create(service.isAreaCovered(searchMode, address, searchDate))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Client error"))
                .verify();
    }
}