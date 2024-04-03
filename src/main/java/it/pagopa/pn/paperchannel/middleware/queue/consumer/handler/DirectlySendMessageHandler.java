package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import lombok.experimental.SuperBuilder;

/**
 * Possiamo eliminare questa classe rendendo {@link SendToDeliveryPushHandler} non astratta?
 * */
@SuperBuilder
public class DirectlySendMessageHandler extends SendToDeliveryPushHandler { }
