package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

@Slf4j
public class Complex890MessageHandler extends SendToDeliveryPushHandler {

    private static final String META_RECAG012_STATUS_CODE = buildMetaStatusCode(RECAG012_STATUS_CODE);

    private static final String META_PNAG012_STATUS_CODE = buildMetaStatusCode(PNAG012_STATUS_CODE);

    private final EventMetaDAO eventMetaDAO;

    private final Long ttlDaysMeta;

    public Complex890MessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, Long ttlDaysMeta) {
        super(sqsSender);
        this.eventMetaDAO = eventMetaDAO;
        this.ttlDaysMeta = ttlDaysMeta;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        String pkFilter = buildMetaRequestId(paperRequest.getRequestId());
        return eventMetaDAO.findAllByRequestId(pkFilter)
                .collectList()
                .doOnNext(pnEventMetas -> log.info("[{}] Result of findAllByRequestId: {}", paperRequest.getRequestId(), pnEventMetas))
                .flatMap(pnEventMetas -> checkValidResult(pnEventMetas, entity, paperRequest))
                .then();
    }

    private Mono<Void> checkValidResult(List<PnEventMeta> pnEventMetas, PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        boolean containsPNAG012 = false;
        boolean containsRECAG012 = false;
        PnEventMeta pnEventMetaRECAG012 = null;

        for (PnEventMeta pnEventMeta: pnEventMetas) {
            if(META_PNAG012_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode())) {
                containsPNAG012 = true;
            }
            if(META_RECAG012_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode())) {
                containsRECAG012 = true;
                pnEventMetaRECAG012 = pnEventMeta;
            }
        }

        if(CollectionUtils.isEmpty(pnEventMetas)) {
            entity.setStatusCode(StatusCodeEnum.OK.getValue());
            return super.handleMessage(entity, paperRequest);
        }

        if (containsPNAG012 && (!containsRECAG012)) {  // presente META##PNAG012 ma NON META##RECAG012
            throw new RuntimeException();
        }
        else if (containsPNAG012 && containsRECAG012) { // presenti META##RECAG012  e META##PNAG012
            entity.setStatusCode(StatusCodeEnum.PROGRESS.getValue());
            return super.handleMessage(entity, paperRequest);
        }
        else if ( (!containsPNAG012) && containsRECAG012) { // presente META##RECAG012  e non META##PNAG012
            PnEventMeta metaForPNAG012Event = createMETAForPNAG012Event(paperRequest, pnEventMetaRECAG012, ttlDaysMeta);
            return eventMetaDAO.createOrUpdate(metaForPNAG012Event)
                    .doOnNext(pnEventMeta -> editPnDeliveryRequestForPNAG012(entity))
                    .flatMap(pnEventMeta -> super.handleMessage(entity, paperRequest));
        }

        return Mono.empty();
    }

    private Mono<Void> delete(List<PnEventMeta> pnEventMetas) {
        return Flux.fromIterable(pnEventMetas)
                .flatMap(pnEventMeta -> eventMetaDAO.deleteEventMeta(pnEventMeta.getMetaRequestId(), pnEventMeta.getMetaStatusCode()))
                .then();
    }
}
