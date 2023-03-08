package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;

public interface MessageHandler {

    void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest);
}
