package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.SearchModeDto;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RaddAltClient {
    Mono<CheckCoverageResponseDto> checkCoverage(SearchModeDto searchMode, CheckCoverageRequestDto checkCoverageRequestDto, LocalDate searchDate);
}