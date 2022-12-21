package it.pagopa.pn.paperchannel.middleware.queue.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.paperchannel.middleware.queue.model.DeliveryPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class QueueListener {

    @Autowired
    private ObjectMapper objectMapper;

    //@SqsListener(value = "${pn.paper-channel.queue-external-channel}",deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pull(@Payload String node, @Headers Map<String,Object> headers){
        //convertPayload(node);
        log.info("BODY - {}",node);
        log.info("HEADERS - {}",headers);
    }

    private DeliveryPayload convertPayload(String json){
        try {
            return objectMapper.readValue(json,DeliveryPayload.class);

        } catch (JsonProcessingException e) {
            log.error("Error in convertPayload ", e.getMessage());
            return null;
        }
    }
}
