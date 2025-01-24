package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.api.dto.events.PnPreparePaperchannelToDelayerPayload;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.model.DematInternalEvent;
import it.pagopa.pn.paperchannel.model.F24Error;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;


import java.time.Instant;


public interface SqsSender {

    void pushSendEvent(SendEvent event);
    void pushPrepareEvent(PrepareEvent event);
    void pushToInternalQueue(PrepareAsyncRequest prepareAsyncRequest);
    void pushToNormalizeAddressQueue(PrepareNormalizeAddressEvent prepareNormalizeAddressEvent);
    void pushToPaperchannelToDelayerQueue(PnPreparePaperchannelToDelayerPayload payload);
    void pushDematZipInternalEvent(DematInternalEvent dematZipInternalEvent);
    void pushSingleStatusUpdateEvent(SingleStatusUpdateDto singleStatusUpdateDto);
    void pushToDelayerToPaperchennelQueue(PnPrepareDelayerToPaperchannelPayload payload);

    void pushSendEventOnEventBridge(String clientId, SendEvent event);
    void pushPrepareEventOnEventBridge(String clientId, PrepareEvent event);

    <T> void pushInternalError(T entity, int attempt, Class<T> tClass);
    <T> void rePushInternalError(T entity, int attempt, Instant expired, Class<T> tClass);

    <T> void redrivePreparePhaseOneAfterError(T entity, int attempt, Class<T> tClass);
    void pushErrorDelayerToPaperChannelAfterSafeStorageErrorQueue(PnPrepareDelayerToPaperchannelPayload entity);
    void pushF24ErrorDelayerToPaperChannelQueue(F24Error entity);
}
