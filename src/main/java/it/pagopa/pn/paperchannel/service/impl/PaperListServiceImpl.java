package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.mapper.CapMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CapDAO;
import it.pagopa.pn.paperchannel.service.PaperListService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@CustomLog
@Service
@RequiredArgsConstructor
public class PaperListServiceImpl implements PaperListService {

    private final CapDAO capDAO;

    @Override
    public Mono<CapResponseDto> getAllCap(String value) {
        return capDAO.getAllCap(value)
                .map(CapMapper::toResponse);
    }
}
