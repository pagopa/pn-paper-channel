package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import it.pagopa.pn.paperchannel.queue.model.EventTypeEnum;
public interface SqsSender {


    void pushEvent(EventTypeEnum eventType, DeliveryPayload entity);
}
