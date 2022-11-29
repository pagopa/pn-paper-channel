package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnPaperEventException;
import it.pagopa.pn.paperchannel.mapper.PreparePaperResponseMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperMessagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;

@Slf4j
@Service
public class PaperMessagesServiceImpl implements PaperMessagesService {
    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;

    @Override
    public Mono<SendEvent> preparePaperSync(String requestId, PrepareRequest prepareRequest){
        return requestDeliveryDAO.getByRequestId(requestId)
                .map(PreparePaperResponseMapper::fromResult)
                .onErrorResume(PnGenericException.class, ex -> {
                    if (ex.getExceptionType() == DELIVERY_REQUEST_NOT_EXIST){
                        log.info("Delivery request");
                        return requestDeliveryDAO.create(RequestDeliveryMapper.toEntity(prepareRequest))
                                .map(entity -> {
                                    throw new PnPaperEventException(PreparePaperResponseMapper.fromEvent(requestId));
                                });
                    }
                    return Mono.error(ex);
                });

    }





}
