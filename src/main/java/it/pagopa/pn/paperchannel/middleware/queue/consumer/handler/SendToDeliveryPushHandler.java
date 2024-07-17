package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.InvalidEventOrderException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.SendEventMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnEventErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PaperProgressStatusEventOriginalMessageInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventError;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@SuperBuilder
public abstract class SendToDeliveryPushHandler implements MessageHandler {

    protected final SqsSender sqsSender;
    protected final RequestDeliveryDAO requestDeliveryDAO;
    protected final PnPaperChannelConfig pnPaperChannelConfig;
    protected final PnEventErrorDAO pnEventErrorDAO;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return StatusCodeEnum.PROGRESS.getValue().equals(entity.getStatusDetail())
                ? handleProgressMessage(entity, paperRequest)
                : handleFeedbackMessage(entity, paperRequest);
    }

    /**
     * Handle progress messages decoupling its logic from feedback logic
     *
     * @param pnDeliveryRequest the delivery request
     * @param paperRequest      the paper request containing original event
     *
     * */
    private Mono<Void> handleProgressMessage(PnDeliveryRequest pnDeliveryRequest, PaperProgressStatusEventDto paperRequest) {
        return Mono.just(pnDeliveryRequest)
                .doOnNext(r -> this.pushSendEvent(pnDeliveryRequest, paperRequest))
                .then();
    }

    /**
     * Handle final messages decoupling its logic from progress logic
     *
     * @param pnDeliveryRequest the delivery request
     * @param paperRequest      the paper request containing original event
     *
     * */
    private Mono<Void> handleFeedbackMessage(PnDeliveryRequest pnDeliveryRequest, PaperProgressStatusEventDto paperRequest) {
        if(pnDeliveryRequest.getFeedbackStatusCode() != null) {
            return Mono.error(
                InvalidEventOrderException.from(pnDeliveryRequest, paperRequest,
                    "[{" + paperRequest.getRequestId() + "}] Wrong feedback event detected for {"
                            + paperRequest + "}")
            );
        }

        return Mono.just(pnDeliveryRequest)
                .doOnNext(r -> this.pushSendEvent(pnDeliveryRequest, paperRequest))
                .flatMap(r -> this.updateRefinedDeliveryRequest(pnDeliveryRequest, paperRequest))
                .flatMap(r -> this.processPnEventErrorsRedrive(r, paperRequest))
                .then();
    }

    /**
     * Update delivery request attributes related to feedbacks
     *
     * @param pnDeliveryRequest request to update
     *
     * @return Mono containing {@link PnDeliveryRequest} object
     * */
    private Mono<PnDeliveryRequest> updateRefinedDeliveryRequest(PnDeliveryRequest pnDeliveryRequest, PaperProgressStatusEventDto paperRequest) {
            pnDeliveryRequest.setRefined(Boolean.TRUE);
            pnDeliveryRequest.setFeedbackStatusCode(paperRequest.getStatusCode());
            pnDeliveryRequest.setFeedbackDeliveryFailureCause(paperRequest.getDeliveryFailureCause());
            pnDeliveryRequest.setFeedbackStatusDateTime(paperRequest.getStatusDateTime().toInstant());

            return this.requestDeliveryDAO
                    .updateData(pnDeliveryRequest, Boolean.TRUE)
                    .doOnError(ex -> log.warn("[{}] Error while updating pnDeliveryRequest", pnDeliveryRequest.getRequestId(), ex));
    }

    /**
     * This method checks if there are out of order events which are possible to automatically redrive reading from PnEventError Dynamo table.
     * It also applies a whitelist processing logic, working on a specific set of event codes (RECAGXXXC) defined in {@link PnPaperChannelConfig}.
     *
     * @param pnDeliveryRequest the delivery request
     * @param paperRequest      the paper request containing original event
     *
     * */
    private Mono<Void> processPnEventErrorsRedrive(PnDeliveryRequest pnDeliveryRequest, PaperProgressStatusEventDto paperRequest) {
        if (StatusCodeEnum.OK.getValue().equals(pnDeliveryRequest.getStatusDetail())) {
            log.info("[{}] Processing PnEventErrors for request", pnDeliveryRequest.getRequestId());

            List<String> allowedRedriveProgressStatusCodes = pnPaperChannelConfig.getAllowedRedriveProgressStatusCodes();

            if (!CollectionUtils.isEmpty(allowedRedriveProgressStatusCodes)) {
                return pnEventErrorDAO.findEventErrorsByRequestId(paperRequest.getRequestId())
                        .filter(pnEventError -> allowedRedriveProgressStatusCodes.contains(pnEventError.getStatusCode()))
                        .doOnDiscard(PnEventError.class, pnEventError -> log.debug("[{}] PnEventErrors for request is not on allowedRedriveProgressStatusCodes PnEventError={}", pnDeliveryRequest.getRequestId(), pnEventError))
                        .doOnNext(pnEventError -> log.info("[{}] Sending to pushSingleStatusUpdateEvent PnEventError={}", pnDeliveryRequest.getRequestId(), pnEventError))
                        .map(pnEventError -> Tuples.of(
                            pnEventError,
                            mapPnEventErrorToSingleStatusUpdate(pnEventError)
                        ))

                        .doOnNext(pnEventErrorWithSingleStatusUpdate -> sqsSender.pushSingleStatusUpdateEvent(pnEventErrorWithSingleStatusUpdate.getT2()))
                        .doOnNext(pnEventErrorWithSingleStatusUpdate -> log.debug("[{}] PnEventErrors sent SingleStatusUpdateDto={}", pnDeliveryRequest.getRequestId(), pnEventErrorWithSingleStatusUpdate.getT2()))
                        .flatMap(pnEventErrorWithSingleStatusUpdate -> {
                            PnEventError pnEventError = pnEventErrorWithSingleStatusUpdate.getT1();
                            return pnEventErrorDAO.deleteItem(pnEventError.getRequestId(), pnEventError.getStatusBusinessDateTime());
                        })

                        // trap the exception to avoid failures related to this plus redrive mechanism
                        .doOnError(ex -> log.error("[{}] PnEventErrors FAILED redrive", pnDeliveryRequest.getRequestId(), ex))
                        .onErrorResume(ex -> Mono.empty())
                        .then();
            }
        } else
            log.debug("[{}] Event is not OK, skipping processing on PnEventErrors for request", pnDeliveryRequest.getRequestId());

        return Mono.empty();
    }

    @NotNull
    private static SingleStatusUpdateDto mapPnEventErrorToSingleStatusUpdate(PnEventError pnEventError) {

        if (pnEventError.getOriginalMessageInfo() instanceof PaperProgressStatusEventOriginalMessageInfo paperProgressStatusEventOriginalMessageInfo) {

            SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
            PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
            paperProgressStatusEventDto.setRequestId(pnEventError.getRequestId());
            paperProgressStatusEventDto.setStatusDateTime(paperProgressStatusEventOriginalMessageInfo.getStatusDateTime().atOffset(ZoneOffset.UTC));
            paperProgressStatusEventDto.setStatusCode(paperProgressStatusEventOriginalMessageInfo.getStatusCode());
            paperProgressStatusEventDto.setStatusDescription(paperProgressStatusEventOriginalMessageInfo.getStatusDescription());
            paperProgressStatusEventDto.setRegisteredLetterCode(paperProgressStatusEventOriginalMessageInfo.getRegisteredLetterCode());
            paperProgressStatusEventDto.setProductType(paperProgressStatusEventOriginalMessageInfo.getProductType());
            paperProgressStatusEventDto.setClientRequestTimeStamp(paperProgressStatusEventOriginalMessageInfo.getClientRequestTimeStamp().atOffset(ZoneOffset.UTC));

            singleStatusUpdateDto.setAnalogMail(paperProgressStatusEventDto);
            return singleStatusUpdateDto;
        } else {
            log.error("PnEventError is not istanceof PaperProgressStatusEventOriginalMessageInfo, skipping redrive PnEventError={}", pnEventError);
            throw new PnGenericException(ExceptionTypeEnum.MAPPER_ERROR, "PnEventError is not and instance of PaperProgressStatusEventOriginalMessageInfo");
        }
    }

    /**
     * Send event to event bridge or delivery push based on delivery request id
     *
     * @param pnDeliveryRequest delivery request
     * @param paperRequest      progress event coming from ext-channel
     * */
    private void pushSendEvent(PnDeliveryRequest pnDeliveryRequest, PaperProgressStatusEventDto paperRequest) {
        log.debug("[{}] Sending to delivery-push or event-bridge", paperRequest.getRequestId());
        log.debug("[{}] Response of ExternalChannel from request id {}", paperRequest.getRequestId(), paperRequest);

        SendEvent sendEvent = SendEventMapper.createSendEventMessage(pnDeliveryRequest, paperRequest);

        if (Utility.isCallCenterEvoluto(pnDeliveryRequest.getRequestId())){
            String clientId = MDC.get(Const.CONTEXT_KEY_CLIENT_ID);
            log.debug("[{}] clientId from context", clientId);
            sqsSender.pushSendEventOnEventBridge(clientId, sendEvent);
            log.info("[{}] Sent to event-bridge: {}", paperRequest.getRequestId(), sendEvent);
        } else {
            sqsSender.pushSendEvent(sendEvent);
            log.info("[{}] Sent to delivery-push: {}", paperRequest.getRequestId(), sendEvent);
        }
    }
}
