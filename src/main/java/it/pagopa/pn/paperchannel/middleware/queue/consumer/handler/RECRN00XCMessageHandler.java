package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

@Slf4j
public class RECRN00XCMessageHandler extends SendToDeliveryPushHandler {
    private final EventMetaDAO eventMetaDAO;

    public RECRN00XCMessageHandler(SqsSender sqsSender, EventMetaDAO eventMetaDAO) {
        super(sqsSender);
        this.eventMetaDAO = eventMetaDAO;
    }


    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        return this.eventMetaDAO.getDeliveryEventMeta(buildMetaRequestId(entity.getRequestId()), buildMetaStatusCode(RECRN011_STATUS_CODE))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[{}] Missing EventMeta for {}", paperRequest.getRequestId(), paperRequest);
                    return Mono.just(new PnEventMeta());
                }))
                .then();
    }
}
