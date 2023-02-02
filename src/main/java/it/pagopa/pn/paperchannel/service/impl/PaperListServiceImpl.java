package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.rest.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.service.PaperListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class PaperListServiceImpl implements PaperListService {


    @Override
    public Mono<CapResponseDto> getAllCap(String cap) {
        return null;
    }
}
