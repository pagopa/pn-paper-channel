package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.mapper.CapMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CapDAO;
import it.pagopa.pn.paperchannel.rest.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.service.PaperListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class PaperListServiceImpl implements PaperListService {

    @Autowired
    private CapDAO capDAO;

    @Override
    public Mono<CapResponseDto> getAllCap(String cap) {
        return capDAO.getAllCap(cap)
                .map(list -> CapMapper.toResponse(list));
    }
}
