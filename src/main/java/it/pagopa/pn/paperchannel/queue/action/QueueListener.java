package it.pagopa.pn.paperchannel.queue.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

//@Component
@Slf4j
public class QueueListener {

    //@Autowired
    //private ObjectMapper objectMapper;

    //@SqsListener(value = "${aws.ready-delivery-queue}",deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void pull( String node, Map<String,Object> headers){

       // convertPayload(node);
        log.info("BODY - {}",node);
        log.info("HEADERS - {}",headers);
    }

    private DeliveryPayload convertPayload(String json){
        /*
        try {
            return objectMapper.readValue(json,DeliveryPayload.class);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
         */
        return  null;
    }
}
