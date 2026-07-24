package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEvent;
import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.queue.model.AttemptEventHeader;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.F24Error;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.PN_EVENT_HEADER_EVENT_TYPE;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_ATTEMPT;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueListener {
    private final QueueListenerService queueListenerService;
    private final PnPaperChannelConfig paperChannelConfig;
    private final ObjectMapper objectMapper;
    private final PaperRequestErrorDAO paperRequestErrorDAO;

    @SqsListener(value = "${pn.paper-channel.queue-national-registries}", acknowledgementMode = SqsListenerAcknowledgementMode.ALWAYS)
    public void pullNationalRegistries(@Payload String node, @Headers Map<String, Object> headers){
        AddressSQSMessageDto dto = convertToObject(node, AddressSQSMessageDto.class);
        setMDCContext(headers);
        this.queueListenerService.nationalRegistriesResponseListener(dto);
    }

    @SqsListener(value = "${pn.paper-channel.queue-external-channel}", acknowledgementMode = SqsListenerAcknowledgementMode.ON_SUCCESS)
    public void pullExternalChannel(@Payload String node, @Headers Map<String,Object> headers){
        SingleStatusUpdateDto body = convertToObject(node, SingleStatusUpdateDto.class);
        setMDCContext(headers);
        this.queueListenerService.externalChannelListener(body, 0);
    }

    @SqsListener(value = "${pn.paper-channel.queue-f24}", acknowledgementMode = SqsListenerAcknowledgementMode.ON_SUCCESS)
    public void pullF24(@Payload String node, @Headers Map<String,Object> headers){
        PnF24PdfSetReadyEvent.Detail body = convertToObject(node, PnF24PdfSetReadyEvent.Detail.class);
        setMDCContext(headers);
        this.queueListenerService.f24ResponseListener(body);
    }

    @SqsListener(value = "${pn.paper-channel.queue-delayer-to-paperchannel}", acknowledgementMode = SqsListenerAcknowledgementMode.ALWAYS)
    public void pullDelayerMessages(@Payload String node, @Headers Map<String,Object> headers){
        setMDCContext(headers);

        if(log.isTraceEnabled()){ //TODO cambiare da TRACE a DEBUG una volta introdotto il delayer (perchè senza il delayer, stampa l'indirizzo normalizzato)
            log.trace("Message from pullDelayerMessages, headers={}, payload: {}", headers, node);
        }
        else {
            log.info("Message from pullDelayerMessages, payload: {}", node);
        }

        AttemptEventHeader attemptEventHeader = toAttemptEventHeader(headers);
        if(attemptEventHeader == null) {
            //evento che viene dal delayer
            this.handlePreparePhaseTwoAsyncFlowEvent(null, node);
        }
        else {
            //evento che viene da paper-channel stesso
            switch (EventTypeEnum.valueOf(attemptEventHeader.getEventType())) {
                case PREPARE_ASYNC_FLOW:  this.handlePreparePhaseTwoAsyncFlowEvent(attemptEventHeader, node); break; // evento inviato dal consumer di f24
                case F24_ERROR:  this.handleF24ErrorEvent(attemptEventHeader, node); break;
                case SAFE_STORAGE_ERROR: this.handleSafeStorageErrorEventFromPreparePhaseTwo(attemptEventHeader, node); break;
                default: log.error("Event type not allowed in Prepare Async Phase Two Flow: {}", attemptEventHeader.getEventType());
            }
        }

    }

    private void handleSafeStorageErrorEventFromPreparePhaseTwo(AttemptEventHeader attemptEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueSafeStorage()-1) < attemptEventHeader.getAttempt();
        PnPrepareDelayerToPaperchannelPayload error = convertToObject(node, PnPrepareDelayerToPaperchannelPayload.class);
        if(noAttempt) {
            PnLogAudit pnLogAudit = new PnLogAudit();
            pnLogAudit.addsBeforeDiscard(error.getIun(), String.format("requestId = %s finish retry to Safe Storage from PREPARE phase 2", error.getRequestId()));

            PnRequestError pnRequestError = PnRequestError.builder()
                    .requestId(error.getRequestId())
                    .error(DOCUMENT_NOT_DOWNLOADED.getMessage())
                    .flowThrow(EventTypeEnum.SAFE_STORAGE_ERROR.name())
                    .build();

            paperRequestErrorDAO.created(pnRequestError).subscribe();

            pnLogAudit.addsSuccessDiscard(error.getIun(), String.format("requestId = %s finish retry to Safe Storage from PREPARE phase 2", error.getRequestId()));
        }
        else {
            this.queueListenerService.delayerListener(error, attemptEventHeader.getAttempt());
        }
    }

    private void handleF24ErrorEvent(AttemptEventHeader internalEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueF24()-1) < internalEventHeader.getAttempt();
        F24Error error = convertToObject(node, F24Error.class);
        if(noAttempt) {
            PnLogAudit pnLogAudit = new PnLogAudit();
            pnLogAudit.addsBeforeDiscard(error.getIun(), String.format("requestId = %s finish retry f24 error ?", error.getRequestId()));

            PnRequestError pnRequestError = PnRequestError.builder()
                    .requestId(error.getRequestId())
                    .error(error.getMessage())
                    .flowThrow(EventTypeEnum.F24_ERROR.name())
                    .build();

            paperRequestErrorDAO.created(pnRequestError).subscribe();

            pnLogAudit.addsSuccessDiscard(error.getIun(), String.format("requestId = %s finish retry f24 error", error.getRequestId()));
        }
        else {
            this.queueListenerService.f24ErrorListener(error, internalEventHeader.getAttempt());
        }
    }

    private void handlePreparePhaseTwoAsyncFlowEvent(AttemptEventHeader attemptEventHeader, String node) {
        var body = convertToObject(node, PnPrepareDelayerToPaperchannelPayload.class);
        int attempt;
        if(attemptEventHeader == null) {
            log.info("Push prepare phase two queue from delayer");
            attempt = 0;
        }
        else {
            attempt = attemptEventHeader.getAttempt();
            log.info("Push prepare phase two queue from internal, attempt = {}", attempt);
        }

        this.queueListenerService.delayerListener(body, attempt);

    }

    private AttemptEventHeader toAttemptEventHeader(Map<String, Object> headers){
        if (headers.containsKey(PN_EVENT_HEADER_EVENT_TYPE) &&
                headers.containsKey(PN_EVENT_HEADER_ATTEMPT)){

            String headerEventType = headers.get(PN_EVENT_HEADER_EVENT_TYPE) instanceof String headerEventTypeString ? headerEventTypeString : "";

            int headerAttempt = 0;
            try {
                headerAttempt = Integer.parseInt((String) headers.get(PN_EVENT_HEADER_ATTEMPT));
            } catch (NumberFormatException | DateTimeParseException ex ){
                log.warn("QueueListener#toInternalEventHeader - Ignoring exception: {}", ex.getClass().getCanonicalName());
            }
            return AttemptEventHeader.builder()
                    .attempt(headerAttempt)
                    .eventType(headerEventType)
                    .build();

        }
        return null;
    }

    private <T> T convertToObject(String body, Class<T> tClass){
        T entity = Utility.jsonToObject(this.objectMapper, body, tClass);
        if (entity == null) throw new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        return entity;
    }

    private void setMDCContext(Map<String, Object> headers){
        MDCUtils.clearMDCKeys();

        if (headers.containsKey("id")){
            String awsMessageId = headers.get("id").toString();
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }

        if (headers.containsKey("AWSTraceHeader")){
            String traceId = headers.get("AWSTraceHeader").toString();
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }
    }

}