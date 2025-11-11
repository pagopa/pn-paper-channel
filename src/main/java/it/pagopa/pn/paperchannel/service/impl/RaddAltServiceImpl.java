package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.CheckCoverageResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnraddalt.v1.dto.SearchModeDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.msclient.RaddAltClient;
import it.pagopa.pn.paperchannel.model.RaddSearchMode;
import it.pagopa.pn.paperchannel.service.RaddAltService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaddAltServiceImpl implements RaddAltService {
    private final RaddAltClient raddAltClient;
    @Override
    public Mono<Boolean> isAreaCovered(RaddSearchMode searchMode, PnAddress pnAddress, Instant searchDate) {
        LocalDate searchLocalDate = LocalDateTime.ofInstant(searchDate, ZoneOffset.UTC).toLocalDate();
        SearchModeDto searchModeDto = searchMode.toClientSearchMode();

        CheckCoverageRequestDto checkCoverageRequestDto = new CheckCoverageRequestDto();
        checkCoverageRequestDto.setCap(pnAddress.getCap());
        checkCoverageRequestDto.setCity(pnAddress.getCity());
        return raddAltClient.checkCoverage(searchModeDto,  checkCoverageRequestDto, searchLocalDate)
                .map(CheckCoverageResponseDto::getHasCoverage)
                .doOnNext(coverage -> log.info("Address for requestId {} covered: {}", pnAddress.getRequestId(), coverage))
                .doOnError(error -> log.error("Error checking coverage for requestId {}: {}", pnAddress.getRequestId(), error.getMessage()));
    }
}
