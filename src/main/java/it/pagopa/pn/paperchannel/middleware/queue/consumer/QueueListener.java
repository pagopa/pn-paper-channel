package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEvent;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader;
import it.pagopa.pn.paperchannel.middleware.queue.model.ManualRetryEvent;
import it.pagopa.pn.paperchannel.model.DelayerToPaperChannelEventPayload;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.util.Pair;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.PN_EVENT_HEADER_EVENT_TYPE;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_ATTEMPT;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_EXPIRED;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueListener {
    private final QueueListenerService queueListenerService;
    private final PnPaperChannelConfig paperChannelConfig;
    private final SqsSender sqsSender;
    private final ObjectMapper objectMapper;
    private final PaperRequestErrorDAO paperRequestErrorDAO;

    @SqsListener(value = "${pn.paper-channel.queue-internal}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void pullFromInternalQueue(@Payload String node, @Headers Map<String, Object> headers){
        log.info("Headers : {}", headers);
        setMDCContext(headers);
        InternalEventHeader internalEventHeader = toInternalEventHeader(headers);

        if (internalEventHeader == null) return;

        if (internalEventHeader.getEventType().equals(EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name())) {
            this.handleNationalRegistriesErrorEvent(internalEventHeader, node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.MANUAL_RETRY_EXTERNAL_CHANNEL.name())) {
            this.handleManualRetryExternalChannelEvent(node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())) {
            this.handleExternalChannelErrorEvent(internalEventHeader, node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.SAFE_STORAGE_ERROR.name())) {
            this.handleSafeStorageErrorEvent(internalEventHeader, node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.ADDRESS_MANAGER_ERROR.name())) {
            this.handleAddressManagerErrorEvent(internalEventHeader, node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.F24_ERROR.name())) {
            this.handleF24ErrorEvent(internalEventHeader, node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.ZIP_HANDLE_ERROR.name())) {
            this.handleZipErrorEvent(internalEventHeader, node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.PREPARE_ASYNC_FLOW.name())) {
            this.handlePrepareAsyncFlowEvent(internalEventHeader, node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.SEND_ZIP_HANDLE.name())) {
            this.handleSendZipEvent(internalEventHeader, node);
        }

        else if (internalEventHeader.getEventType().equals(EventTypeEnum.REDRIVE_PAPER_PROGRESS_STATUS.name())) {
            this.handleRedrivePaperProgressStatus(internalEventHeader, node);
        }

    }

    @SqsListener(value = "${pn.paper-channel.queue-national-registries}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void pullNationalRegistries(@Payload String node, @Headers Map<String, Object> headers){
        AddressSQSMessageDto dto = convertToObject(node, AddressSQSMessageDto.class);
        setMDCContext(headers);
        this.queueListenerService.nationalRegistriesResponseListener(dto);
    }

    @SqsListener(value = "${pn.paper-channel.queue-external-channel}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pullExternalChannel(@Payload String node, @Headers Map<String,Object> headers){
        SingleStatusUpdateDto body = convertToObject(node, SingleStatusUpdateDto.class);
        setMDCContext(headers);
        this.queueListenerService.externalChannelListener(body, 0);
    }

    @SqsListener(value = "${pn.paper-channel.queue-f24}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pullF24(@Payload String node, @Headers Map<String,Object> headers){
        PnF24PdfSetReadyEvent.Detail body = convertToObject(node, PnF24PdfSetReadyEvent.Detail.class);
        setMDCContext(headers);
        this.queueListenerService.f24ResponseListener(body);
    }

    @SqsListener(value = "${pn.paper-channel.queue-radd-alt}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pullRaddAlt(@Payload String node, @Headers Map<String,Object> headers){
        var body = convertToObject(node, PnAttachmentsConfigEventPayload.class);
        setMDCContext(headers);
        log.debug("Handle message from raddAltListener with header {}, body:{}", headers, body);
        this.queueListenerService.raddAltListener(body);
    }

    @SqsListener(value = "${pn.paper-channel.delayer-to-paperchannel}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void pullDelayerMessages(@Payload String node, @Headers Map<String,Object> headers){
        var body = convertToObject(node, DelayerToPaperChannelEventPayload.class);
        setMDCContext(headers);
        log.debug("Handle message from delayerListener with header {}, body:{}", headers, body);
        this.queueListenerService.delayerListener(body);
    }

    private void handleNationalRegistriesErrorEvent(InternalEventHeader internalEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueNationalRegistries()-1) < internalEventHeader.getAttempt();
        NationalRegistryError error = convertToObject(node, NationalRegistryError.class);
        execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), NationalRegistryError.class,
                entity -> {
                    PnLogAudit pnLogAudit = new PnLogAudit();
                    pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry to National Registry", entity.getRequestId()));

                    PnRequestError pnRequestError = PnRequestError.builder()
                            .requestId(entity.getRequestId())
                            .error("ERROR WITH RETRIEVE ADDRESS")
                            .flowThrow(EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name())
                            .build();

                    paperRequestErrorDAO.created(pnRequestError).subscribe();

                    pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry to National Registry", entity.getRequestId()));
                    return null;
                },
                entityAndAttempt -> {
                    this.queueListenerService.nationalRegistriesErrorListener(entityAndAttempt.getFirst(), entityAndAttempt.getSecond());
                    return null;
                });
    }

    private void handleManualRetryExternalChannelEvent(String node) {
        ManualRetryEvent manualRetryEvent = convertToObject(node, ManualRetryEvent.class);
        this.queueListenerService.manualRetryExternalChannel(manualRetryEvent.getRequestId(), manualRetryEvent.getNewPcRetry());
    }

    private void handleExternalChannelErrorEvent(InternalEventHeader internalEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueExternalChannel()-1) < internalEventHeader.getAttempt();
        ExternalChannelError error = convertToObject(node, ExternalChannelError.class);
        execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), ExternalChannelError.class,
                entity -> {
                    PnLogAudit pnLogAudit = new PnLogAudit();
                    pnLogAudit.addsBeforeDiscard(entity.getAnalogMail().getIun(), String.format("requestId = %s finish retry to External Channel", entity.getAnalogMail().getRequestId()));

                    PnRequestError pnRequestError = PnRequestError.builder()
                            .requestId(entity.getAnalogMail().getRequestId())
                            .error(EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage())
                            .flowThrow(EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())
                            .build();

                    paperRequestErrorDAO.created(pnRequestError).subscribe();

                    pnLogAudit.addsSuccessDiscard(entity.getAnalogMail().getIun(), String.format("requestId = %s finish retry to External Channel", entity.getAnalogMail().getRequestId()));
                    return null;
                },
                entityAndAttempt -> {
                    SingleStatusUpdateDto dto = new SingleStatusUpdateDto();
                    dto.setAnalogMail(entityAndAttempt.getFirst().getAnalogMail());
                    this.queueListenerService.externalChannelListener(dto, entityAndAttempt.getSecond());
                    return null;
                });
    }

    private void handleSafeStorageErrorEvent(InternalEventHeader internalEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueSafeStorage()-1) < internalEventHeader.getAttempt();
        PrepareAsyncRequest error = convertToObject(node, PrepareAsyncRequest.class);
        execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), PrepareAsyncRequest.class,
                entity -> {
                    PnLogAudit pnLogAudit = new PnLogAudit();
                    pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry to Safe Storage", entity.getRequestId()));

                    PnRequestError pnRequestError = PnRequestError.builder()
                            .requestId(entity.getRequestId())
                            .error(DOCUMENT_NOT_DOWNLOADED.getMessage())
                            .flowThrow(EventTypeEnum.SAFE_STORAGE_ERROR.name())
                            .build();

                    paperRequestErrorDAO.created(pnRequestError).subscribe();

                    pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry to Safe Storage", entity.getRequestId()));
                    return null;
                },
                entityAndAttempt -> {
                    this.queueListenerService.internalListener(entityAndAttempt.getFirst(), entityAndAttempt.getSecond());
                    return null;
                });
    }

    private void handleAddressManagerErrorEvent(InternalEventHeader internalEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueAddressManager()-1) < internalEventHeader.getAttempt();
        PrepareAsyncRequest error = convertToObject(node, PrepareAsyncRequest.class);
        execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), PrepareAsyncRequest.class,
                entity -> {
                    PnLogAudit pnLogAudit = new PnLogAudit();
                    pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry address manager error ?", entity.getRequestId()));

                    PnRequestError pnRequestError = PnRequestError.builder()
                            .requestId(entity.getRequestId())
                            .error(ADDRESS_MANAGER_ERROR.getMessage())
                            .flowThrow(EventTypeEnum.ADDRESS_MANAGER_ERROR.name())
                            .build();

                    paperRequestErrorDAO.created(pnRequestError).subscribe();

                    pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry address manager error", entity.getRequestId()));
                    return null;
                },
                entityAndAttempt -> {
                    this.queueListenerService.internalListener(entityAndAttempt.getFirst(), entityAndAttempt.getSecond());
                    return null;
                });
    }

    private void handleF24ErrorEvent(InternalEventHeader internalEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueF24()-1) < internalEventHeader.getAttempt();
        F24Error error = convertToObject(node, F24Error.class);
        execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), F24Error.class,
                entity -> {
                    PnLogAudit pnLogAudit = new PnLogAudit();
                    pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry f24 error ?", entity.getRequestId()));

                    PnRequestError pnRequestError = PnRequestError.builder()
                            .requestId(entity.getRequestId())
                            .error(entity.getMessage())
                            .flowThrow(EventTypeEnum.F24_ERROR.name())
                            .build();

                    paperRequestErrorDAO.created(pnRequestError).subscribe();

                    pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry f24 error", entity.getRequestId()));
                    return null;
                },
                entityAndAttempt -> {
                    this.queueListenerService.f24ErrorListener(entityAndAttempt.getFirst(), entityAndAttempt.getSecond());
                    return null;
                });
    }

    private void handleZipErrorEvent(InternalEventHeader internalEventHeader, String node) {

        boolean noAttempt = (paperChannelConfig.getAttemptQueueZipHandle() -1 ) < internalEventHeader.getAttempt();
        var error = convertToObject(node, DematInternalEvent.class);
        execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), DematInternalEvent.class,
                entity -> {
                    PnLogAudit pnLogAudit = new PnLogAudit();
                    pnLogAudit.addsBeforeDiscard(entity.getIun(), String.format("requestId = %s finish retry zip handle error ?", entity.getRequestId()));

                    PnRequestError pnRequestError = PnRequestError.builder()
                            .requestId(entity.getRequestId())
                            .error(entity.getErrorMessage())
                            .flowThrow(EventTypeEnum.ZIP_HANDLE_ERROR.name())
                            .build();

                    paperRequestErrorDAO.created(pnRequestError).subscribe();

                    pnLogAudit.addsSuccessDiscard(entity.getIun(), String.format("requestId = %s finish retry zip handle error", entity.getRequestId()));
                    return null;
                },
                entityAndAttempt -> {
                    this.queueListenerService.dematZipInternalListener(entityAndAttempt.getFirst(), entityAndAttempt.getSecond());
                    return null;
                });
    }

    private void handlePrepareAsyncFlowEvent(InternalEventHeader internalEventHeader, String node) {
        log.info("Push internal queue - first time");
        PrepareAsyncRequest request = convertToObject(node, PrepareAsyncRequest.class);
        this.queueListenerService.internalListener(request, internalEventHeader.getAttempt());
    }

    private void handleSendZipEvent(InternalEventHeader internalEventHeader, String node) {
        log.info("Push dematZipInternal queue - first time");
        var request = convertToObject(node, DematInternalEvent.class);
        this.queueListenerService.dematZipInternalListener(request, internalEventHeader.getAttempt());
    }

    private void handleRedrivePaperProgressStatus(InternalEventHeader internalEventHeader, String node) {
        log.info("Push redrive paper progress status queue - first time");
        var request = convertToObject(node, SingleStatusUpdateDto.class);
        this.queueListenerService.externalChannelListener(request, internalEventHeader.getAttempt());
    }

    private InternalEventHeader toInternalEventHeader(Map<String, Object> headers){
        if (headers.containsKey(PN_EVENT_HEADER_EVENT_TYPE) &&
                headers.containsKey(PN_EVENT_HEADER_EXPIRED) &&
                headers.containsKey(PN_EVENT_HEADER_ATTEMPT)){
            String headerEventType = headers.get(PN_EVENT_HEADER_EVENT_TYPE) instanceof String ? (String) headers.get(PN_EVENT_HEADER_EVENT_TYPE) : "";

            int headerAttempt = 0;
            Instant headerExpired = null;
            try {
                headerAttempt = Integer.parseInt((String) headers.get(PN_EVENT_HEADER_ATTEMPT));
                headerExpired = Instant.parse((String)headers.get(PN_EVENT_HEADER_EXPIRED));
            } catch (NumberFormatException | DateTimeParseException ex ){
                log.warn("QueueListener#toInternalEventHeader - Ignoring exception: {}", ex.getClass().getCanonicalName());
            }
            return InternalEventHeader.builder()
                    .expired(headerExpired)
                    .attempt(headerAttempt)
                    .eventType(headerEventType)
                    .build();

        }
        return null;
    }

    private <T> void execution(T entity, boolean noAttempt, int attempt, Instant expired, Class<T> tClass,
                               Function<T, Void> traceErrorDB,
                               Function<Pair<T, Integer>, Void> pushQueue){
        if (noAttempt) {
            traceErrorDB.apply(entity);
            return;
        }

        if (expired.isAfter(Instant.now())){
            Mono.delay(Duration.ofMillis(1)).publishOn(Schedulers.boundedElastic())
                    .map(i -> {
                        this.sqsSender.rePushInternalError(entity, attempt, expired, tClass);
                        return "";
                    }).subscribe();
            return;
        }
        pushQueue.apply(Pair.of(entity, attempt));
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