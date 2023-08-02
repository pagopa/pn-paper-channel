package it.pagopa.pn.paperchannel.middleware.queue.model;


import lombok.Data;

@Data
public class ManualRetryEvent {

    private String requestId;
    //retry da concatenare
    private String newPcRetry;

}
