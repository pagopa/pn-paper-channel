package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.exception.PnSendToDeliveryException;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
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
    private final EventDematDAO eventDematDAO;

    public RECAG008CMessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, EventDematDAO eventDematDAO) {
        super(sqsSender);
        this.eventMetaDAO = eventMetaDAO;
        this.eventDematDAO = eventDematDAO;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return eventMetaDAO.findAllByRequestId(buildMetaRequestId(paperRequest.getRequestId()))
                .collectList()
                .filter(this::correctPreviousEventMeta)
                    .doOnNext(pnEventMetas -> log.info("Found correct previous states for request {}", paperRequest.getRequestId()))

                // send to DeliveryPush
                .doOnNext(pnEventMetas -> super.handleMessage(entity, paperRequest))
                    .onErrorResume(throwable -> {
                        log.warn("Error on handleMessage", throwable);
                        return Mono.error(new PnSendToDeliveryException(throwable));
                    })

                // delete related Metas
                .then(eventMetaDAO.deleteBatch(buildMetaRequestId(paperRequest.getRequestId()), META_RECAG012_STATUS_CODE, META_PNAG012_STATUS_CODE))
                    .doOnNext(deletedEntity -> log.info("Deleted EventMetas request {}", paperRequest.getRequestId()))
                    .onErrorResume(throwable ->  {
                        if (throwable instanceof PnSendToDeliveryException)
                            return Mono.error(throwable);
                        else {
                            log.warn("Cannot delete EventMetas", throwable);
                            return Mono.empty();
                        }
                    })

                // delete related Demats
                .then(eventDematDAO.findAllByRequestId(buildDematRequestId(paperRequest.getRequestId())).collectList()
                        .flatMap(items ->
                                eventDematDAO.deleteBatch(buildDematRequestId(paperRequest.getRequestId()), items.stream().map(PnEventDemat::getDocumentTypeStatusCode).toArray(String[]::new)))
                                    .doOnNext(deletedEntity -> log.info("Deleted EventDemat: {}", deletedEntity))
                        )
                    .onErrorResume(throwable ->  {
                        if (throwable instanceof PnSendToDeliveryException)
                            return Mono.error(throwable);
                        else {
                            log.warn("Cannot delete EventDemats", throwable);
                            return Mono.empty();
                        }
                    })
                .then();
    }

    private Boolean correctPreviousEventMeta(List<PnEventMeta> pnEventMetas)
    {
        Optional<PnEventMeta> elRECAG012 = pnEventMetas.stream()
                .filter(pnEventMeta -> META_RECAG012_STATUS_CODE.equals(pnEventMeta.getStatusCode()))
                .findFirst();

        Optional<PnEventMeta> elPNAG012 = pnEventMetas.stream()
                .filter(pnEventMeta -> META_PNAG012_STATUS_CODE.equals(pnEventMeta.getStatusCode()))
                .findFirst();

        log.info("RECAG012 presence {}, PNAG012 presence {}", elRECAG012.isPresent(), elPNAG012.isPresent());

        return elRECAG012.isPresent() && elPNAG012.isPresent();
    }
}
