package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.service.SqsSender;

public class DirectlySendMessageHandler extends SendToDeliveryPushHandler {
    public DirectlySendMessageHandler(SqsSender sqsSender) {
        super(sqsSender);
    }
}
