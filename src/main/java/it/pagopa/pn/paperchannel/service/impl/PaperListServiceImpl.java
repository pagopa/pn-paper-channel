package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.mapper.CapMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CapDAO;
import it.pagopa.pn.paperchannel.rest.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.service.PaperListService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@CustomLog
@Service
public class PaperListServiceImpl implements PaperListService {

    @Autowired
    private CapDAO capDAO;

    @Override
    public Mono<CapResponseDto> getAllCap(String value) {
        String processName = "Get All Cap";
        log.logStartingProcess(processName);
        return capDAO.getAllCap(value)
                .map(paperCosts -> {
                    log.logEndingProcess(processName);
                    return CapMapper.toResponse(paperCosts);
                });
    }
}
