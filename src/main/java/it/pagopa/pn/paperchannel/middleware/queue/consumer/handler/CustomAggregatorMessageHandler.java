package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;


import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaRequestId;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.buildMetaStatusCode;

@Slf4j
public class CustomAggregatorMessageHandler extends AggregatorMessageHandler {
    private final EventMetaDAO eventMetaDAO;

    public CustomAggregatorMessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, MetaDematCleaner metaDematCleaner) {
        super(sqsSender, eventMetaDAO, metaDematCleaner);
        this.eventMetaDAO = eventMetaDAO;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        final String metaStatus = buildMetaStatusCode(getAEvent(paperRequest.getStatusCode()));

        return eventMetaDAO.getDeliveryEventMeta(buildMetaRequestId(paperRequest.getRequestId()), metaStatus)
                .switchIfEmpty(Mono.defer(() -> {
                    //FIXME - throw exception
                    log.warn("[{}] Missing EventMeta for {}", paperRequest.getRequestId(), paperRequest);
                    return Mono.just(new PnEventMeta());
                }))
                .flatMap(relatedMeta -> settingStatus(entity, relatedMeta.getDeliveryFailureCause(), paperRequest))
                .flatMap(newEntity -> super.handleMessage(newEntity, paperRequest));

    }


    private Mono<PnDeliveryRequest> settingStatus(PnDeliveryRequest deliveryRequest, String deliveryFailureCause, PaperProgressStatusEventDto paperRequest){
        if (StringUtils.equals("M02", deliveryFailureCause) || StringUtils.equals("M05", deliveryFailureCause)){
            RequestDeliveryMapper.changeState(
                    deliveryRequest,
                    deliveryRequest.getStatusCode(),
                    StatusCodeEnum.OK.getValue(),
                    StatusCodeEnum.OK.getValue(),
                    paperRequest.getProductType(),
                    null);
        }
        if (StringUtils.equals("M06", deliveryFailureCause) || StringUtils.equals("M07", deliveryFailureCause) ||
                StringUtils.equals("M08", deliveryFailureCause) || StringUtils.equals("M09", deliveryFailureCause) ) {
            RequestDeliveryMapper.changeState(
                    deliveryRequest,
                    deliveryRequest.getStatusCode(),
                    StatusCodeEnum.KO.getValue(),
                    StatusCodeEnum.KO.getValue(),
                    paperRequest.getProductType(),
                    null);
        }
        return Mono.just(deliveryRequest);
    }


    private String getAEvent(String eventReceived){
        return eventReceived.substring(0, eventReceived.length() - 1) + "A";
    }


}
