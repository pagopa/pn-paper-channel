package it.pagopa.pn.paperchannel.middleware.queue.model.delayer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DelayerToPaperChannelEventPayload {
    private String requestId;
    private String iun;
    private String attemptRetry;
}
