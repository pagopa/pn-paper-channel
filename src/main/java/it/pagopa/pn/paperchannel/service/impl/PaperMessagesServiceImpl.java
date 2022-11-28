package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.mapper.PreparePaperResponseMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PaperMessagesServiceImpl implements PaperMessagesService {
    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;

    @Override
    public Mono<SendEvent> preparePaperSync(String requestId, Mono<PrepareRequest> prepareRequest){
        return prepareRequest.flatMap(request -> requestDeliveryDAO
                        .create(RequestDeliveryMapper.toEntity(requestId, request.getReceiverFiscalCode(), "hash address"))
                )
                .map(entity -> PreparePaperResponseMapper.fromResult());

    }





}
