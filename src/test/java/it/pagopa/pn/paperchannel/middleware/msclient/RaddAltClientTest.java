package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.SearchModeDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

class RaddAltClientTest extends BaseTest.WithMockServer {
    @Autowired
    private RaddAltClient raddAltClient;

    @Test
    void testCoverageOK(){
        CheckCoverageRequestDto requestDto = new CheckCoverageRequestDto();
        requestDto.setCap("cap");
        requestDto.setCity("city");
        CheckCoverageResponseDto response = raddAltClient.checkCoverage(SearchModeDto.LIGHT, requestDto, LocalDate.now()).block();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(Boolean.TRUE, response.getHasCoverage());
    }

    @Test
    void testCoverageKO(){
        Mono<CheckCoverageResponseDto> mono = raddAltClient.checkCoverage(SearchModeDto.LIGHT, new CheckCoverageRequestDto(), LocalDate.now());
        StepVerifier.create(mono)
                .expectErrorMatches(throwable -> throwable instanceof WebClientResponseException wex && wex.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }
}