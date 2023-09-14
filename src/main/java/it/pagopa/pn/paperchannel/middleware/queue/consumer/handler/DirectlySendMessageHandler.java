package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.dao.PnClientDAO;
import it.pagopa.pn.paperchannel.service.SqsSender;

public class DirectlySendMessageHandler extends SendToDeliveryPushHandler {
    public DirectlySendMessageHandler(SqsSender sqsSender, PnClientDAO pnClientDAO) {
        super(sqsSender, pnClientDAO);
    }
}
