package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.api.CoveragePrivateApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.SearchModeDto;
import it.pagopa.pn.paperchannel.middleware.msclient.RaddAltClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RaddAltClientImpl implements RaddAltClient {
    private final CoveragePrivateApi coveragePrivateApi;

    public Mono<CheckCoverageResponseDto> checkCoverage(SearchModeDto searchMode, CheckCoverageRequestDto checkCoverageRequestDto, LocalDate searchDate) {
        return coveragePrivateApi.checkCoverage(searchMode, checkCoverageRequestDto, searchDate);
    }
}
