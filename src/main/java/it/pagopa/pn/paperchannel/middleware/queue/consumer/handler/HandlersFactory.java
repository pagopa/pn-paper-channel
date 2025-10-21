package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.*;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN003CMessageHandler;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN004CMessageHandler;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN00XC.RECRN005CMessageHandler;
import it.pagopa.pn.paperchannel.middleware.queue.producer.OcrProducer;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.PcRetryUtils;
import it.pagopa.pn.paperchannel.utils.SendProgressMetaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum.*;

/**
 * TODO Refactor this class using Abstract Factory pattern dividing factories per business family (e.g. 890, AR)
 * */
@Component
@RequiredArgsConstructor
@Slf4j
public class HandlersFactory {

    private final PcRetryUtils pcRetryUtils;

    private final PaperRequestErrorDAO paperRequestErrorDAO;

    private final PnPaperChannelConfig pnPaperChannelConfig;

    private final SqsSender sqsSender;

    private final EventMetaDAO eventMetaDAO;

    private final EventDematDAO eventDematDAO;

    private final MetaDematCleaner metaDematCleaner;

    private final RequestDeliveryDAO requestDeliveryDAO;

    private final PnEventErrorDAO pnEventErrorDAO;

    private final SendProgressMetaConfig sendProgressMetaConfig;

    private final OcrProducer ocrProducer;

    private final SafeStorageClient safeStorageClient;

    private final PaperChannelDeliveryDriverDAO deliveryDriverDAO;

    private ConcurrentHashMap<String, MessageHandler> map;


    private final LogMessageHandler logExtChannelsMessageHandler = LogMessageHandler.builder().build();

    @PostConstruct
    public void initializeHandlers() {

        SaveDematMessageHandler saveDematMessageHandler = SaveDematMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventDematDAO(eventDematDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        RetryableErrorMessageHandler retryableErrorExtChannelsMessageHandler = RetryableErrorMessageHandler.builder()
                .sqsSender(sqsSender)
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pcRetryUtils(pcRetryUtils)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        NotRetryableErrorMessageHandler notRetryableErrorMessageHandler = NotRetryableErrorMessageHandler.builder()
                .sqsSender(sqsSender)
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        NotRetriableWithoutSendErrorMessageHandler notRetriableWithoutSendErrorMessageHandler = NotRetriableWithoutSendErrorMessageHandler.builder()
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .build();

        CustomAggregatorMessageHandler customAggregatorMessageHandler = CustomAggregatorMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .metaDematCleaner(metaDematCleaner)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        SendToOcrProxyHandler customAggregatorOcrProxyMessageHandler = SendToOcrProxyHandler.builder()
                .sqsSender(sqsSender)
                .messageHandler(customAggregatorMessageHandler)
                .paperChannelConfig(pnPaperChannelConfig)
                .eventMetaDAO(eventMetaDAO)
                .eventDematDAO(eventDematDAO)
                .safeStorageClient(safeStorageClient)
                .deliveryDriverDAO(deliveryDriverDAO)
                .build();

        AggregatorMessageHandler aggregatorMessageHandler = AggregatorMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .metaDematCleaner(metaDematCleaner)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        SendToOcrProxyHandler aggregatorOcrProxyMessageHandler = SendToOcrProxyHandler.builder()
                .sqsSender(sqsSender)
                .messageHandler(aggregatorMessageHandler)
                .paperChannelConfig(pnPaperChannelConfig)
                .eventMetaDAO(eventMetaDAO)
                .eventDematDAO(eventDematDAO)
                .safeStorageClient(safeStorageClient)
                .deliveryDriverDAO(deliveryDriverDAO)
                .build();

        SendToDeliveryPushHandler sendToDeliveryPushHandler = SendToDeliveryPushHandler.builder()
                .sqsSender(sqsSender)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        PNAG012MessageHandler pnag012MessageHandler = PNAG012MessageHandler.builder()
                .sqsSender(sqsSender)
                .eventDematDAO(eventDematDAO)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        RECAGSimplifiedPostLogicHandler recagSimplifiedPostLogicHandler = RECAGSimplifiedPostLogicHandler.builder()
                .sqsSender(sqsSender)
                .eventDematDAO(eventDematDAO)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        ChainedMessageHandler recagxxxbMessageHandler = ChainedMessageHandler.builder()
                .handlers(List.of(saveDematMessageHandler, recagSimplifiedPostLogicHandler))
                .build();

        RECAG012MessageHandler recag012SaveMetaMessageHandler = RECAG012MessageHandler.builder()
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();

        RECAG012AMessageHandler recag012AMessageHandler = RECAG012AMessageHandler.builder()
                .sqsSender(sqsSender)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        ChainedMessageHandler recag012MessageHandler = ChainedMessageHandler.builder()
                .handlers(
                        Stream.of(
                                        recag012SaveMetaMessageHandler,
                                        recagSimplifiedPostLogicHandler,
                                        // SendProgressMeta feature flag (PN-12284)
                                        sendProgressMetaConfig.isRECAG012AEnabled() ? recag012AMessageHandler : null
                                )
                                .filter(Objects::nonNull)
                                .toList()
                )
                .build();

        OldRECAG012MessageHandler baseOldRECAG012MessageHandler = OldRECAG012MessageHandler.builder()
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnag012MessageHandler(pnag012MessageHandler)
                .build();

        MessageHandler oldRECAG012MessageHandler = ChainedMessageHandler.builder()
                .handlers(
                        Stream.of(
                                        baseOldRECAG012MessageHandler,
                                        // SendProgressMeta feature flag (PN-12284)
                                        sendProgressMetaConfig.isRECAG012AEnabled() ? recag012AMessageHandler : null
                                )
                                .filter(Objects::nonNull)
                                .toList()
                )
                .build();

        RECAG011AMessageHandler recag011AMessageHandler = RECAG011AMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();

        RECAG011BMessageHandler recag011BMessageHandler = RECAG011BMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventDematDAO(eventDematDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnag012MessageHandler(pnag012MessageHandler)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        RECAG008CMessageHandler recag008CMessageHandler = RECAG008CMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        Complex890MessageHandler complex890MessageHandler = Complex890MessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        RECRN003CMessageHandler recrn003cMessageHandler = RECRN003CMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        SendToOcrProxyHandler recrn003cOcrProxyMessageHandler = SendToOcrProxyHandler.builder()
                .sqsSender(sqsSender)
                .messageHandler(recrn003cMessageHandler)
                .paperChannelConfig(pnPaperChannelConfig)
                .eventMetaDAO(eventMetaDAO)
                .eventDematDAO(eventDematDAO)
                .safeStorageClient(safeStorageClient)
                .deliveryDriverDAO(deliveryDriverDAO)
                .build();

        RECRN004CMessageHandler recrn004cMessageHandler = RECRN004CMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .build();

        SendToOcrProxyHandler recrn004cOcrProxyMessageHandler = SendToOcrProxyHandler.builder()
                .sqsSender(sqsSender)
                .messageHandler(recrn004cMessageHandler)
                .paperChannelConfig(pnPaperChannelConfig)
                .eventMetaDAO(eventMetaDAO)
                .eventDematDAO(eventDematDAO)
                .safeStorageClient(safeStorageClient)
                .deliveryDriverDAO(deliveryDriverDAO)
                .build();

        RECRN005CMessageHandler recrn005cMessageHandler = RECRN005CMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .build();

        SendToOcrProxyHandler recrn005cOcrProxyMessageHandler = SendToOcrProxyHandler.builder()
                .sqsSender(sqsSender)
                .messageHandler(recrn005cMessageHandler)
                .paperChannelConfig(pnPaperChannelConfig)
                .eventMetaDAO(eventMetaDAO)
                .eventDematDAO(eventDematDAO)
                .safeStorageClient(safeStorageClient)
                .deliveryDriverDAO(deliveryDriverDAO)
                .build();

        RECRN011MessageHandler recrn011cMessageHandler = RECRN011MessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();

        Simple890MessageHandler simple890MessageHandler = Simple890MessageHandler.builder()
                .sqsSender(sqsSender)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        Proxy890MessageHandler proxy890MessageHandler = Proxy890MessageHandler.builder()
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .recag008CMessageHandler(recag008CMessageHandler)
                .complex890MessageHandler(complex890MessageHandler)
                .simple890MessageHandler(simple890MessageHandler)
                .pnEventErrorDAO(pnEventErrorDAO)
                .pnEventErrorDAO(pnEventErrorDAO)
                .build();

        ProxyCON996MessageHandler proxyCON996MessageHandler = ProxyCON996MessageHandler.builder()
                .retryableErrorMessageHandler(retryableErrorExtChannelsMessageHandler)
                .notRetryableErrorMessageHandler(notRetryableErrorMessageHandler)
                .requestDeliveryDAO(requestDeliveryDAO)
                .build();

        // Metadata handlers
        SaveMetadataMessageHandler baseSaveMetadataMessageHandler = SaveMetadataMessageHandler.builder()
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();

        ChainedMessageHandler saveMetadataMessageHandler = ChainedMessageHandler.builder()
                .handlers(
                        Stream.of(
                                        baseSaveMetadataMessageHandler,
                                        // SendProgressMeta feature flag (PN-12284)
                                        sendProgressMetaConfig.isMetaEnabled() ? sendToDeliveryPushHandler : null
                                )
                                .filter(Objects::nonNull)
                                .toList()
                )
                .build();

        map = new ConcurrentHashMap<>();

        addRetryableErrorStatusCodes(map, retryableErrorExtChannelsMessageHandler);
        addNotRetryableErrorStatusCodes(map, notRetryableErrorMessageHandler);
        addNotRetryableErrorStatusCodeWithoutSend(map, notRetriableWithoutSendErrorMessageHandler);
        addSaveMetadataStatusCodes(map, saveMetadataMessageHandler);
        addSaveDematStatusCodes(map, saveDematMessageHandler);
        addRecagxxxbSaveDematStatusCodes(map, recagxxxbMessageHandler);
        addAggregatorStatusCodes(map, aggregatorMessageHandler);
        addAggregatorSendToOcrStatusCodes(map, aggregatorOcrProxyMessageHandler);
        addDirectlySendStatusCodes(map, sendToDeliveryPushHandler);
        addCustomAggregatorStatusCodes(map, customAggregatorMessageHandler);
        addCustomAggregatorSendToOcrStatusCodes(map, customAggregatorOcrProxyMessageHandler);

        //casi 890
        map.put("RECAG011A", recag011AMessageHandler);

        map.put(RECAG012.name(), recag012MessageHandler);

        map.put(RECAG005C.name(), proxy890MessageHandler);
        map.put(RECAG006C.name(), proxy890MessageHandler);
        map.put(RECAG007C.name(), proxy890MessageHandler);
        map.put(RECAG008C.name(), proxy890MessageHandler);

        map.put(RECRN011.name(), recrn011cMessageHandler);

        map.put(RECRN003C.name(), recrn003cOcrProxyMessageHandler);
        map.put(RECRN004C.name(), recrn004cOcrProxyMessageHandler);
        map.put(RECRN005C.name(), recrn005cOcrProxyMessageHandler);
        map.put(PNAG012.name(), pnag012MessageHandler);

        if (pnPaperChannelConfig.isEnableRetryCon996()) {
            map.put(CON996.name(), proxyCON996MessageHandler);
        }

        /* Override mapping handlers before simple 890 (PN-10501) - Remove when feature flag will be not necessary */
        if (!pnPaperChannelConfig.isEnableSimple890Flow()) {

            log.info("Using old 890 handlers because feature flag is disabled");

            /* TODO use abstract factory to reduce comple-simple build complexity */
            proxy890MessageHandler = ComplexProxy890MessageHandler.builder()
                    .pnPaperChannelConfig(pnPaperChannelConfig)
                    .recag008CMessageHandler(recag008CMessageHandler)
                    .complex890MessageHandler(complex890MessageHandler)
                    .simple890MessageHandler(simple890MessageHandler)
                    .pnEventErrorDAO(pnEventErrorDAO)
                    .build();

            map.put(RECAG012.name(), oldRECAG012MessageHandler);

            map.put(RECAG005B.name(), saveDematMessageHandler);
            map.put(RECAG006B.name(), saveDematMessageHandler);
            map.put(RECAG007B.name(), saveDematMessageHandler);
            map.put(RECAG008B.name(), saveDematMessageHandler);
            map.put(RECAG011B.name(), recag011BMessageHandler);

            map.put(RECAG005C.name(), proxy890MessageHandler);
            map.put(RECAG006C.name(), proxy890MessageHandler);
            map.put(RECAG007C.name(), proxy890MessageHandler);
            map.put(RECAG008C.name(), proxy890MessageHandler);
        }
        // SendProgressMeta feature flag (PN-12284)
        if(sendProgressMetaConfig.isCON018Enabled()){
            map.put(CON018.name(), sendToDeliveryPushHandler);
        }

        if(pnPaperChannelConfig.isSendCon020()){
            map.put(CON020.name(), sendToDeliveryPushHandler);
        }

    }

    private void addRetryableErrorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, RetryableErrorMessageHandler handler) {
        map.put(ExternalChannelCodeEnum.RECRS006.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN006.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG004.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI005.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRSI005.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS013.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN013.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG013.name(), handler);
    }

    private void addNotRetryableErrorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, NotRetryableErrorMessageHandler handler) {
        map.put(ExternalChannelCodeEnum.CON998.name(), handler);
        map.put(ExternalChannelCodeEnum.CON997.name(), handler);
        if(!pnPaperChannelConfig.isEnableRetryCon996()) {
            map.put(ExternalChannelCodeEnum.CON996.name(), handler);
        }
        map.put(ExternalChannelCodeEnum.CON995.name(), handler);
        map.put(ExternalChannelCodeEnum.CON993.name(), handler);
    }

    private void addNotRetryableErrorStatusCodeWithoutSend(ConcurrentHashMap<String, MessageHandler> map, NotRetriableWithoutSendErrorMessageHandler handler){
        map.put(ExternalChannelCodeEnum.P008.name(), handler);
        map.put(ExternalChannelCodeEnum.P010.name(), handler);
        map.put(ExternalChannelCodeEnum.P011.name(), handler);
        map.put(ExternalChannelCodeEnum.P012.name(), handler);
        map.put(ExternalChannelCodeEnum.P013.name(), handler);
        map.put(ExternalChannelCodeEnum.P014.name(), handler);
    }

    private void addSaveMetadataStatusCodes(ConcurrentHashMap<String, MessageHandler> map, ChainedMessageHandler handler) {
        map.put(ExternalChannelCodeEnum.RECRS002A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS002D.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS004A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS005A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN010.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN001A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN002A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN002D.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN003A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN004A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN005A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG001A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG002A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003D.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG005A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG006A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG007A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG008A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI003A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI004A.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRSI004A.name(), handler);
    }


    private void addSaveDematStatusCodes(ConcurrentHashMap<String, MessageHandler> map, SaveDematMessageHandler handler) {
        map.put(ExternalChannelCodeEnum.RECRS002B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS002E.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS004B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS005B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN001B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN002B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN002E.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN003B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN004B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN005B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG001B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG002B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003E.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI003B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI004B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRSI004B.name(), handler);
    }



    private void addRecagxxxbSaveDematStatusCodes(ConcurrentHashMap<String, MessageHandler> map, ChainedMessageHandler handler) {
        map.put(ExternalChannelCodeEnum.RECAG005B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG006B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG007B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG008B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG011B.name(), handler);
    }

    private void addDirectlySendStatusCodes(ConcurrentHashMap<String, MessageHandler> map, SendToDeliveryPushHandler handler) {


        //caso CON080, RECRI001, RECRI002
        map.put(ExternalChannelCodeEnum.CON080.name(), handler); // progress
        map.put(ExternalChannelCodeEnum.RECRI001.name(), handler); // progress
        map.put(ExternalChannelCodeEnum.RECRI002.name(), handler); // progress

        // casi particolari di addAggregatorStatusCodes, in cui non c'è un meta precedente e vanno direttamente inviati
        map.put(ExternalChannelCodeEnum.RECRS001C.name(), handler); // iniziale e finale, no meta e demat prima; ok
        map.put(ExternalChannelCodeEnum.RECRS003C.name(), handler); // iniziale e finale, no meta e demat prima; ok

        map.put(ExternalChannelCodeEnum.RECRS015.name(), handler); // progress
        map.put(ExternalChannelCodeEnum.RECRN015.name(), handler); // progress

        map.put(ExternalChannelCodeEnum.RECAG015.name(), handler); // progress

        // send "inesito" events to delivery push as progresses
        map.put(ExternalChannelCodeEnum.RECAG010.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS010.name(), handler);
    }

    private void addAggregatorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, AggregatorMessageHandler handler) {
        map.put(ExternalChannelCodeEnum.RECRS002C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS002F.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS004C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS005C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG001C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG002C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003F.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI003C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI004C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRSI003C.name(), handler); //L’evento potrebbe non essere mai generato in quanto non garantito su tutte i diversi paesi internazionali
        map.put(ExternalChannelCodeEnum.RECRSI004C.name(), handler);
    }

    private void addAggregatorSendToOcrStatusCodes(ConcurrentHashMap<String, MessageHandler> map, SendToOcrProxyHandler proxyHandler) {
        map.put(ExternalChannelCodeEnum.RECRN001C.name(), proxyHandler);
        map.put(ExternalChannelCodeEnum.RECRN002F.name(), proxyHandler);
    }

    private void addCustomAggregatorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, CustomAggregatorMessageHandler handler){
        map.put(ExternalChannelCodeEnum.RECAG003C.name(), handler);
    }

    private void addCustomAggregatorSendToOcrStatusCodes(ConcurrentHashMap<String, MessageHandler> map, SendToOcrProxyHandler proxyHandler){
        map.put(ExternalChannelCodeEnum.RECRN002C.name(), proxyHandler);
    }

    public MessageHandler getHandler(@NotNull String code) {
        return map.getOrDefault(code, logExtChannelsMessageHandler);
    }

}
