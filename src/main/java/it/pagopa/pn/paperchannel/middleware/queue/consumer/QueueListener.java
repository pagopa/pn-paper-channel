package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

@Component
@Slf4j
public class QueueListener {

    @Autowired
    private PaperAsyncService paperAsyncService;
    @Autowired
    private SqsSender sender;
    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private PaperResultAsyncService paperResultAsyncService;
    @Autowired
    private ObjectMapper objectMapper;

    @SqsListener(value = "${pn.paper-channel.queue-internal}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pullFromInternalQueue(@Payload String node, @Headers Map<String, Object> headers) {
        Mono.just(node)
                .doOnNext(message -> log.info("Do On Next: {}", message))
                .mapNotNull(json -> Utility.jsonToObject(this.objectMapper, json, PrepareAsyncRequest.class))
                .switchIfEmpty(Mono.error(new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage())))
                .flatMap(prepareRequest -> this.paperAsyncService.prepareAsync(prepareRequest))
                .doOnSuccess(resultFromAsync -> {
                    log.info("End of prepare async internal");
                })
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    throw new PnGenericException(PREPARE_ASYNC_LISTENER_EXCEPTION, PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage());
                })
                .block();
    }

    @SqsListener(value = "${pn.paper-channel.queue-national-registries}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pullNationalRegistries(@Payload String node, @Headers Map<String, Object> headers){
        Mono.just(node)
                .doOnNext(message -> log.info("Do On Next: {}", message))
                .map(json -> {
                    AddressSQSMessageDto dto = Utility.jsonToObject(this.objectMapper, json, AddressSQSMessageDto.class);
                   if (dto==null || StringUtils.isBlank(dto.getCorrelationId())) throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
                   String correlationId = dto.getCorrelationId();
                   Address address =null;
                   if (dto.getPhysicalAddress()!=null)
                        address=AddressMapper.fromNationalRegistry(dto.getPhysicalAddress());
                   return Tuples.of(correlationId, address);
                })
                .doOnSuccess(correlationAndAddress -> {
                    PrepareAsyncRequest prepareAsyncRequest =new PrepareAsyncRequest(null, correlationAndAddress.getT1(), correlationAndAddress.getT2(), false);
                    this.sender.pushToInternalQueue(prepareAsyncRequest);
                })
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
                })
                .block();
    }

    @SqsListener(value = "${pn.paper-channel.queue-external-channel}",deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pullExternalChannel(@Payload String node, @Headers Map<String,Object> headers){
        log.info("Receive msg from external-channel with BODY - {}",node);
        log.info("Receive msg from external-channel with HEADERS - {}",headers);
        convertSingleStatusUpdateDto(node);

    }

    private void convertSingleStatusUpdateDto(String json) {
        paperResultAsyncService.resultAsyncBackground(Utility.jsonToObject(this.objectMapper, json, SingleStatusUpdateDto.class))
                .block();
    }



}