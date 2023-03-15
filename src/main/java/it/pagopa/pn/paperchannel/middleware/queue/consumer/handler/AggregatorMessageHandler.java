package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.exception.PnSendToDeliveryException;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

// Alla ricezione di questi tipi di eventi, che sono finali per lo specifico prodotto, paper-channel dovrà:
// recuperare l’evento di pre-esito correlato (mediante accesso puntuale su hashkey META##RequestID e sortKey META##statusCode)
// arricchire l’evento finale ricevuto con le eventuali informazioni aggiuntive reperite in tabella (in particolare,
// allo stato dell’arte, tali informazioni sono esclusivamente deliveryFailureCause e discoveredAddress)
// Inviare l’evento arricchito a delivery-push
// cancellate le righe in tabella per legate al requestId per le entità META e DEMAT

@Slf4j
public class AggregatorMessageHandler extends SendToDeliveryPushHandler {

    private static final String METADATA_PREFIX = "META";
    private static final String DEMAT_PREFIX = "DEMAT";

    private static final String DELIMITER = "##";

    private final EventMetaDAO eventMetaDAO;
    private final EventDematDAO eventDematDAO;

    public AggregatorMessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, EventDematDAO eventDematDAO) {
        super(sqsSender);

        this.eventMetaDAO = eventMetaDAO;
        this.eventDematDAO = eventDematDAO;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        // recuperare evento pre-esito da db e arricchire l'evento ricevuto con quello recuperato (deliveryFailureCause/discoveredAddress)
        return eventMetaDAO.getDeliveryEventMeta(METADATA_PREFIX + DELIMITER + paperRequest.getRequestId(),
                        METADATA_PREFIX + DELIMITER + paperRequest.getStatusCode())
                .switchIfEmpty(Mono.defer(() -> {
                            log.warn("Missing EventMeta for: {}", paperRequest);
                            return Mono.just(new PnEventMeta());
                }))
                .doOnNext(relatedMeta -> enrichEvent(paperRequest, relatedMeta))

                // invio dato su delivery-push, che ci sia stato arricchimento o meno)
                .then(super.handleMessage(entity, paperRequest))
                .onErrorResume(throwable -> {
                    log.warn("", throwable);
                    return Mono.error(new PnSendToDeliveryException(throwable));
                })

                .then(eventMetaDAO.deleteEventMeta(METADATA_PREFIX + DELIMITER + paperRequest.getRequestId(),
                                METADATA_PREFIX + DELIMITER + paperRequest.getStatusCode())
                        .doOnNext(deletedEntity -> log.info("Deleted EventMeta: {}", deletedEntity))
                ).then(eventDematDAO.findAllByRequestId(DEMAT_PREFIX + DELIMITER + paperRequest.getRequestId())
                        .flatMap(foundItem ->
                                eventDematDAO.deleteEventDemat(foundItem.getDematRequestId(), foundItem.getDocumentTypeStatusCode())
                                        .doOnNext(deletedEntity -> log.info("Deleted EventDemat: {}", deletedEntity))
                        ).collectList())
                .onErrorResume(throwable ->  {
                    if (throwable instanceof PnSendToDeliveryException)
                        return Mono.error(throwable);
                    else {
                        log.warn("Cannot delete MAT or DEMAT", throwable);
                        return Mono.empty();
                    }
                })
                .then();
    }

    private void enrichEvent(PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMeta) {
        paperRequest.setDeliveryFailureCause(pnEventMeta.getDeliveryFailureCause());

        if (pnEventMeta.getDiscoveredAddress() != null )
        {
            DiscoveredAddressDto discoveredAddressDto =
                    new BaseMapperImpl<>(PnDiscoveredAddress.class, DiscoveredAddressDto.class)
                            .toDTO(pnEventMeta.getDiscoveredAddress());
            paperRequest.setDiscoveredAddress(discoveredAddressDto);
        }
        paperRequest.setDeliveryFailureCause(pnEventMeta.getDeliveryFailureCause());
    }
}
