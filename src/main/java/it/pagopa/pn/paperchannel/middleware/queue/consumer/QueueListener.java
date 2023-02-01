package it.pagopa.pn.paperchannel.middleware.queue.consumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader;
import it.pagopa.pn.paperchannel.model.ExternalChannelError;
import it.pagopa.pn.paperchannel.model.NationalRegistryError;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.function.Function;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.PN_EVENT_HEADER_EVENT_TYPE;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_ATTEMPT;
import static it.pagopa.pn.paperchannel.middleware.queue.model.InternalEventHeader.PN_EVENT_HEADER_EXPIRED;

@Component
@Slf4j
public class QueueListener {
    @Autowired
    private QueueListenerService queueListenerService;
    @Autowired
    private PnPaperChannelConfig paperChannelConfig;
    @Autowired
    private SqsSender sqsSender;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @SqsListener(value = "${pn.paper-channel.queue-internal}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void pullFromInternalQueue(@Payload String node, @Headers Map<String, Object> headers){
        log.info("Headers : {}", headers);
        InternalEventHeader internalEventHeader = toInternalEventHeader(headers);
        if (internalEventHeader == null) return;

        if (internalEventHeader.getEventType().equals(EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name())){

            boolean noAttempt = paperChannelConfig.getAttemptQueueNationalRegistries() != -1 || paperChannelConfig.getAttemptQueueNationalRegistries() < internalEventHeader.getAttempt();
            NationalRegistryError error = convertToObject(node, NationalRegistryError.class);
            execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), NationalRegistryError.class,
                    entity -> {
                        paperRequestErrorDAO.created(entity.getRequestId(), "ERROR WITH RETRIEVE ADDRESS", EventTypeEnum.NATIONAL_REGISTRIES_ERROR.name())
                                .subscribe();
                        return null;
                    },
                    entityAndAttempt -> {
                        this.queueListenerService.nationalRegistriesErrorListener(entityAndAttempt.getFirst(), entityAndAttempt.getSecond());
                        return null;
                    });

        } else if (internalEventHeader.getEventType().equals(EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())){

            boolean noAttempt = paperChannelConfig.getAttemptQueueExternalChannel() != -1 || paperChannelConfig.getAttemptQueueExternalChannel() < internalEventHeader.getAttempt();
            ExternalChannelError error = convertToObject(node, ExternalChannelError.class);
            execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), ExternalChannelError.class,
                    entity -> {
                        paperRequestErrorDAO.created(
                                entity.getAnalogMail().getRequestId(),
                                EXTERNAL_CHANNEL_LISTENER_EXCEPTION.getMessage(),
                                EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name()
                        ).subscribe();
                        return null;
                    },
                    entityAndAttempt -> {
                        SingleStatusUpdateDto dto = new SingleStatusUpdateDto();
                        dto.setAnalogMail(entityAndAttempt.getFirst().getAnalogMail());
                        this.queueListenerService.externalChannelListener(dto, entityAndAttempt.getSecond());
                        return null;
                    });

        } else if (internalEventHeader.getEventType().equals(EventTypeEnum.SAFE_STORAGE_ERROR.name())){

            boolean noAttempt = paperChannelConfig.getAttemptQueueSafeStorage() != -1 || paperChannelConfig.getAttemptQueueSafeStorage() < internalEventHeader.getAttempt();
            PrepareAsyncRequest error = convertToObject(node, PrepareAsyncRequest.class);
            execution(error, noAttempt, internalEventHeader.getAttempt(), internalEventHeader.getExpired(), PrepareAsyncRequest.class,
                    entity -> {
                        paperRequestErrorDAO.created(
                                        entity.getRequestId(),
                                        DOCUMENT_NOT_DOWNLOADED.getMessage(),
                                        EventTypeEnum.SAFE_STORAGE_ERROR.name())
                                .subscribe();
                        return null;
                    },
                    entityAndAttempt -> {
                        this.queueListenerService.internalListener(entityAndAttempt.getFirst(), entityAndAttempt.getSecond());
                        return null;
                    });

        } else if (internalEventHeader.getEventType().equals(EventTypeEnum.PREPARE_ASYNC_FLOW.name())){
            PrepareAsyncRequest request = convertToObject(node, PrepareAsyncRequest.class);
            this.queueListenerService.internalListener(request, internalEventHeader.getAttempt());
        }

    }

    @SqsListener(value = "${pn.paper-channel.queue-national-registries}")
    public void pullNationalRegistries(@Payload String node, @Headers Map<String, Object> headers){
        AddressSQSMessageDto dto = convertToObject(node, AddressSQSMessageDto.class);
        this.queueListenerService.nationalRegistriesResponseListener(dto);
    }

    @SqsListener(value = "${pn.paper-channel.queue-external-channel}")
    public void pullExternalChannel(@Payload String node, @Headers Map<String,Object> headers){
        SingleStatusUpdateDto body = convertToObject(node, SingleStatusUpdateDto.class);
        this.queueListenerService.externalChannelListener(body, 0);
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
            } catch (NumberFormatException | DateTimeParseException ignored ){

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



}