package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;

public class GenericService {
    protected final SqsSender sqsSender;
    protected RequestDeliveryDAO requestDeliveryDAO;

    public GenericService(SqsSender sqsSender, RequestDeliveryDAO requestDeliveryDAO) {
        this.sqsSender = sqsSender;
        this.requestDeliveryDAO = requestDeliveryDAO;
    }

    protected Mono<PnDeliveryRequest> changeStatusDeliveryRequest(PnDeliveryRequest deliveryRequest, StatusDeliveryEnum status){
        RequestDeliveryMapper.changeState(
                deliveryRequest,
                status.getCode(),
                status.getDescription(),
                status.getDetail(),
                deliveryRequest.getProductType(), null);
        return this.requestDeliveryDAO.updateData(deliveryRequest).flatMap(Mono::just);
    }

    protected <T> void saveErrorAndPushError(String requestId, StatusDeliveryEnum status, T error, Function<T, Void> queuePush){
        this.requestDeliveryDAO.getByRequestId(requestId).publishOn(Schedulers.boundedElastic())
                .flatMap(entity -> changeStatusDeliveryRequest(entity, status)
                        .flatMap(updated -> {
                            queuePush.apply(error);
                            return Mono.just("");
                        })
                ).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }
}
