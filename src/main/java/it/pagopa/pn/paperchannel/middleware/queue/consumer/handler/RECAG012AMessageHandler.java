package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * RECAG012AMessageHandler is responsible for handling PnDeliveryRequest messages
 * related to the RECAG012 event type. This handler determines, based on the
 * {@code entity.getRefined()} flag, whether the RECAG012 event has already been
 * processed as feedback (meaning it has been sent to pn-delivery-push), or if it was
 * only saved in the database as an eventMeta and now needs to be converted into a RECAG012A
 * event and sent to pn-delivery-push with a status of PROGRESS.
 *
 * <p>This class extends {@code SendToDeliveryPushHandler} and overrides the
 * {@code handleMessage} method to implement the specific handling logic for RECAG012A events.</p>
 *
 * <p>The flow is as follows:
 * <ul>
 *   <li>The {@code entity} is filtered to process only those that have not been refined (i.e., {@code !entity.getRefined()}).</li>
 *   <li>If the {@code entity} is not refined:
 *     <ul>
 *       <li>This indicates that the RECAG012 event was saved as metadata and has not yet been processed as feedback.</li>
 *       <li>The {@code setRECAG012ADetails} method is called to convert the RECAG012 event into a RECAG012A event.</li>
 *       <li>The {@code handleMessage} method of the superclass {@code SendToDeliveryPushHandler} is called to send
 *       message to pn-delivery-push.</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 */
@Slf4j
@SuperBuilder
public class RECAG012AMessageHandler extends SendToDeliveryPushHandler {
    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] RECAG012A handler start", paperRequest.getRequestId());
        return Mono.just(entity)
                .filter(e -> e.getRefined() != Boolean.TRUE)
                .doOnNext(e -> log.info("[{}] Entity not refined, proceeding with RECAG012A handling",
                        paperRequest.getRequestId()))
                .flatMap(e -> {
                    setRECAG012ADetails(entity, paperRequest);
                    return super.handleMessage(entity, paperRequest);
                })
                .then();
    }

    /**
     * Converts a RECAG012 event into a RECAG012A event by updating the status details
     * of the {@code entity} and {@code paperRequest} to RECAG012A and setting the status
     * to PROGRESS.
     *
     * @param entity the PnDeliveryRequest entity to be updated
     * @param paperRequest the PaperProgressStatusEventDto entity to be updated
     */
    public void setRECAG012ADetails(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        entity.setStatusDetail(StatusCodeEnum.PROGRESS.getValue());
        entity.setStatusCode(ExternalChannelCodeEnum.RECAG012A.name());
        entity.setStatusDescription(ExternalChannelCodeEnum.RECAG012A.name());
        paperRequest.setStatusCode(ExternalChannelCodeEnum.RECAG012A.name());
        paperRequest.setStatusDescription(ExternalChannelCodeEnum.RECAG012A.name());
    }
}
