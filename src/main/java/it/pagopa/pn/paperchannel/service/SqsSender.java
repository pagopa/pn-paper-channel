package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;


public interface SqsSender {


    void pushSendEvent(SendEvent event);
    void pushPrepareEvent(PrepareEvent event);
}
