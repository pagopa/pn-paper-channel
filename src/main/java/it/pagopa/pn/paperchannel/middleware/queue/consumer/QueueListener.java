package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.service.impl.SubscriberPrepare;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.util.Map;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.UNTRACEABLE_ADDRESS;

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


//    @SqsListener(value = "${pn.paper-channel.queue-national-registries}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pullNationalRegistries(@Payload String node, @Headers Map<String, Object> headers){
        Mono.just(node)
                .doOnNext(message -> log.info("Do On Next: {}", message))
                .map(json -> {
                    AddressSQSMessageDto dto = Utility.jsonToObject(this.objectMapper, json, AddressSQSMessageDto.class);
                    if (dto != null && StringUtils.isNotBlank(dto.getCorrelationId()) && dto.getPhysicalAddress() != null) {
                        String correlationId = dto.getCorrelationId();
                        Address address = AddressMapper.fromNationalRegistry(dto.getPhysicalAddress());
                        return Tuples.of(correlationId, address);
                    }
                    throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
                })
                .flatMap(correlationAndAddress -> {
                    //Call service Background
                    return null;
                })
                .doOnSuccess(address -> log.info("Success : {}", address))
                .doOnError(throwable -> {
                    log.error(throwable.getMessage());
                    throw new PnGenericException(UNTRACEABLE_ADDRESS, UNTRACEABLE_ADDRESS.getMessage());
                })
                .block();
    }

//    @SqsListener(value = "${pn.paper-channel.queue-external-channel}",deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pullExternalChannel(@Payload String node, @Headers Map<String,Object> headers){
        convertSingleStatusUpdateDto(node);
        log.info("BODY - {}",node);
        log.info("HEADERS - {}",headers);
    }

    private void convertSingleStatusUpdateDto(String json) {
        paperResultAsyncService.resultAsyncBackground(Utility.jsonToObject(this.objectMapper, json, SingleStatusUpdateDto.class))
                .block();
    }

}