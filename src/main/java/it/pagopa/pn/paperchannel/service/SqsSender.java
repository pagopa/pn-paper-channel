package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.queue.model.DeliveryEvent;
import it.pagopa.pn.paperchannel.queue.model.EventTypeEnum;

public interface SqsSender {


    void pushEvent(EventTypeEnum eventType, DeliveryAsyncModel entity);
}
