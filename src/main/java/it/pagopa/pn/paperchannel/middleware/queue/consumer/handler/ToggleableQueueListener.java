package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.queue.model.AttemptEventHeader;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.PN_EVENT_HEADER_EVENT_TYPE;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ADDRESS_MANAGER_ERROR;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.MAPPER_ERROR;
import static it.pagopa.pn.paperchannel.middleware.queue.model.AttemptEventHeader.PN_EVENT_HEADER_ATTEMPT;

@Configuration
@ConditionalOnProperty(name = "pn.paper-channel.enable-prepare-phase-one", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class ToggleableQueueListener {

    private final QueueListenerService queueListenerService;
    private final PnPaperChannelConfig paperChannelConfig;
    private final PaperRequestErrorDAO paperRequestErrorDAO;
    private final ObjectMapper objectMapper;

    @SqsListener(value = "${pn.paper-channel.queue-normalize-address}", acknowledgementMode = SqsListenerAcknowledgementMode.ALWAYS)
    public void pullFromNormalizeAddressQueue(@Payload String node, @Headers Map<String, Object> headers) {
        setMDCContext(headers);

        if (log.isDebugEnabled()) {
            log.debug("Message from pullFromNormalizeAddressQueue, headers={}, payload: {}", headers, node);
        } else {
            log.info("Message from pullFromNormalizeAddressQueue, payload: {}", node);
        }

        AttemptEventHeader attemptEventHeader = toAttemptEventHeader(headers);

        if (attemptEventHeader == null) return;

        switch (EventTypeEnum.valueOf(attemptEventHeader.getEventType())) {
            case PREPARE_ASYNC_FLOW:
                this.handlePreparePhaseOneAsyncFlowEvent(attemptEventHeader, node);
                break;
            case NATIONAL_REGISTRIES_ERROR:
                this.handleNationalRegistriesErrorEvent(attemptEventHeader, node);
                break;
            case ADDRESS_MANAGER_ERROR:
                this.handleAddressManagerErrorEventFromPreparePhaseOne(attemptEventHeader, node);
                break;
            default:
                log.error("Event type not allowed in Prepare Async Phase One Flow: {}", attemptEventHeader.getEventType());
        }

    }

    private void handleNationalRegistriesErrorEvent(AttemptEventHeader attemptEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueNationalRegistries() - 1) < attemptEventHeader.getAttempt();
        NationalRegistryError entity = convertToObject(node, NationalRegistryError.class);
        if (noAttempt) {
            PnLogAudit pnLogAudit = new PnLogAudit();
            pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry to National Registry", entity.getRequestId()));

            PnRequestError pnRequestError = PnRequestError.builder()
                    .requestId(entity.getRequestId())
                    .error("ERROR WITH RETRIEVE ADDRESS")
                    .flowThrow(EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name())
                    .build();

            paperRequestErrorDAO.created(pnRequestError).subscribe();

            pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry to National Registry", entity.getRequestId()));
        } else {
            this.queueListenerService.nationalRegistriesErrorListener(entity, attemptEventHeader.getAttempt());
        }

    }

    private void handleAddressManagerErrorEventFromPreparePhaseOne(AttemptEventHeader attemptEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueAddressManager() - 1) < attemptEventHeader.getAttempt();
        PrepareNormalizeAddressEvent entity = convertToObject(node, PrepareNormalizeAddressEvent.class);
        if (noAttempt) {
            PnLogAudit pnLogAudit = new PnLogAudit();
            pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry address manager error ?", entity.getRequestId()));

            PnRequestError pnRequestError = PnRequestError.builder()
                    .requestId(entity.getRequestId())
                    .error(ADDRESS_MANAGER_ERROR.getMessage())
                    .flowThrow(EventTypeEnum.ADDRESS_MANAGER_ERROR.name())
                    .build();

            paperRequestErrorDAO.created(pnRequestError).subscribe();

            pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry address manager error", entity.getRequestId()));
        } else {
            this.queueListenerService.normalizeAddressListener(entity, attemptEventHeader.getAttempt());
        }
    }

    private void handlePreparePhaseOneAsyncFlowEvent(AttemptEventHeader internalEventHeader, String node) {
        log.info("Push prepare phase one queue - first time");
        PrepareNormalizeAddressEvent request = convertToObject(node, PrepareNormalizeAddressEvent.class);
        this.queueListenerService.normalizeAddressListener(request, internalEventHeader.getAttempt());
    }

    private <T> T convertToObject(String body, Class<T> tClass) {
        T entity = Utility.jsonToObject(this.objectMapper, body, tClass);
        if (entity == null) throw new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        return entity;
    }

    private AttemptEventHeader toAttemptEventHeader(Map<String, Object> headers) {
        if (headers.containsKey(PN_EVENT_HEADER_EVENT_TYPE) &&
                headers.containsKey(PN_EVENT_HEADER_ATTEMPT)) {

            String headerEventType = headers.get(PN_EVENT_HEADER_EVENT_TYPE) instanceof String headerEventTypeString ? headerEventTypeString : "";

            int headerAttempt = 0;
            try {
                headerAttempt = Integer.parseInt((String) headers.get(PN_EVENT_HEADER_ATTEMPT));
            } catch (NumberFormatException | DateTimeParseException ex) {
                log.warn("QueueListener#toInternalEventHeader - Ignoring exception: {}", ex.getClass().getCanonicalName());
            }
            return AttemptEventHeader.builder()
                    .attempt(headerAttempt)
                    .eventType(headerEventType)
                    .build();

        }
        return null;
    }

    private void setMDCContext(Map<String, Object> headers) {
        MDCUtils.clearMDCKeys();

        if (headers.containsKey("id")) {
            String awsMessageId = headers.get("id").toString();
            MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, awsMessageId);
        }

        if (headers.containsKey("AWSTraceHeader")) {
            String traceId = headers.get("AWSTraceHeader").toString();
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        } else {
            MDC.put(MDCUtils.MDC_TRACE_ID_KEY, String.valueOf(UUID.randomUUID()));
        }
    }
}
