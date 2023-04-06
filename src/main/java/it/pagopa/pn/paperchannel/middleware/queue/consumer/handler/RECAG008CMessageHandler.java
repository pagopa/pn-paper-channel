package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

@Slf4j
public class RECAG008CMessageHandler extends SendToDeliveryPushHandler {

    static final String META_RECAG012_STATUS_CODE = buildMetaStatusCode(RECAG012_STATUS_CODE);
    static final String META_PNAG012_STATUS_CODE = buildMetaStatusCode(PNAG012_STATUS_CODE);

    private final EventMetaDAO eventMetaDAO;
    private final MetaDematCleaner metaDematCleaner;

    public RECAG008CMessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, MetaDematCleaner metaDematCleaner) {
        super(sqsSender);
        this.eventMetaDAO = eventMetaDAO;
        this.metaDematCleaner = metaDematCleaner;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return eventMetaDAO.findAllByRequestId(buildMetaRequestId(paperRequest.getRequestId()))
                .collectList()
                .filter(pnEventMetas -> this.correctPreviousEventMeta(pnEventMetas, paperRequest.getRequestId()))
                .doOnNext(pnEventMetas -> log.info("[{}] Found correct previous states", paperRequest.getRequestId()))

                // send to DeliveryPush (only if the needed metas are found)
                .flatMap(pnEventMetas -> super.handleMessage(entity, paperRequest).then(Mono.just(pnEventMetas)))

                // clean all related metas and demats (only if the needed metas are found)
                .flatMap(ignored -> metaDematCleaner.clean(paperRequest.getRequestId()));
    }

    private Boolean correctPreviousEventMeta(List<PnEventMeta> pnEventMetas, String requestId)
    {
        Optional<PnEventMeta> elRECAG012 = pnEventMetas.stream()
                .filter(pnEventMeta -> META_RECAG012_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode()))
                .findFirst();

        Optional<PnEventMeta> elPNAG012 = pnEventMetas.stream()
                .filter(pnEventMeta -> META_PNAG012_STATUS_CODE.equals(pnEventMeta.getMetaStatusCode()))
                .findFirst();

        log.info("[{}] RECAG012 presence {}, PNAG012 presence {}", requestId, elRECAG012.isPresent(), elPNAG012.isPresent());

        // presence check and error log
        final boolean ok = elRECAG012.isPresent() && elPNAG012.isPresent();
        if (!ok) {
            log.error("[{}] Problem with RECAG012 or PNAG012 presence!", requestId);
        }

        return ok;
    }
}
