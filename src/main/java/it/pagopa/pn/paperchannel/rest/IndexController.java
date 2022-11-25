package it.pagopa.pn.paperchannel.rest;

import it.pagopa.pn.paperchannel.service.SqsQueueSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/")
public class IndexController {

    @Autowired
    private SqsQueueSender sqsQueueSender;

    @GetMapping()
    public ResponseEntity<Void> index(){
        sqsQueueSender.pushEvent();
        return null;
    }
}
