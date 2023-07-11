package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

// Alla ricezione di questi tipi di eventi, che sono finali per lo specifico prodotto, paper-channel dovrà:
// recuperare l’evento di pre-esito correlato (mediante accesso puntuale su hashkey META##RequestID e sortKey META##statusCode)
// arricchire l’evento finale ricevuto con le eventuali informazioni aggiuntive reperite in tabella (in particolare,
// allo stato dell’arte, tali informazioni sono esclusivamente deliveryFailureCause e discoveredAddress)
// Inviare l’evento arricchito a delivery-push
// cancellate le righe in tabella per legate al requestId per le entità META e DEMAT

@Slf4j
public class AggregatorMessageHandler extends SendToDeliveryPushHandler {
    private final EventMetaDAO eventMetaDAO;
    private final MetaDematCleaner metaDematCleaner;


    public AggregatorMessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO, MetaDematCleaner metaDematCleaner) {
        super(sqsSender);

        this.eventMetaDAO = eventMetaDAO;
        this.metaDematCleaner = metaDematCleaner;
    }

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        final String preClosingMetaStatus = preCloseMetaStatusCode(buildMetaStatusCode(paperRequest.getStatusCode()));

        // recuperare evento pre-esito da db e arricchire l'evento ricevuto con quello recuperato (deliveryFailureCause/discoveredAddress)
        return eventMetaDAO.getDeliveryEventMeta(buildMetaRequestId(paperRequest.getRequestId()),
                        preClosingMetaStatus)
                .switchIfEmpty(Mono.defer(() -> {
                            log.warn("[{}] Missing EventMeta for {}", paperRequest.getRequestId(), paperRequest);
                            //FIXME - throw exception
                            return Mono.just(new PnEventMeta());
                }))
                .map(relatedMeta -> enrichEvent(paperRequest, relatedMeta))

                // invio dato su delivery-push, che ci sia stato arricchimento o meno)
                .flatMap(enrichedRequest -> super.handleMessage(entity, enrichedRequest))

                // clean all related metas and demats (che sia stato trovato il meta o meno)
                .then(metaDematCleaner.clean(paperRequest.getRequestId()));
    }

    private PaperProgressStatusEventDto enrichEvent(PaperProgressStatusEventDto paperRequest, PnEventMeta pnEventMeta) {
        paperRequest.setDeliveryFailureCause(pnEventMeta.getDeliveryFailureCause());

        if (pnEventMeta.getDiscoveredAddress() != null)
        {
            DiscoveredAddressDto discoveredAddressDto =
                    new BaseMapperImpl<>(PnDiscoveredAddress.class, DiscoveredAddressDto.class)
                            .toDTO(pnEventMeta.getDiscoveredAddress());
            paperRequest.setDiscoveredAddress(discoveredAddressDto);

            log.info("[{}] Discovered Address in EventMeta for {}", paperRequest.getRequestId(), pnEventMeta);
        }
        paperRequest.setDeliveryFailureCause(pnEventMeta.getDeliveryFailureCause());

        return paperRequest;
    }

    private String preCloseMetaStatusCode(String closingEventStatus) {
        String lastLetter = closingEventStatus.substring(closingEventStatus.length() - 1);

        if (lastLetter.equals("C")) {
            closingEventStatus = closingEventStatus.substring(0, closingEventStatus.length() - 1) + "A";
        } else if (lastLetter.equals("F")) {
            closingEventStatus = closingEventStatus.substring(0, closingEventStatus.length() - 1) + "D";
        }
        // lascia l'originale altrimenti

        return closingEventStatus;
    }
}
