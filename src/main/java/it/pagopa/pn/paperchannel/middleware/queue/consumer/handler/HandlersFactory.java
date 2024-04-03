package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.*;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.MetaDematCleaner;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class HandlersFactory {

    private final ExternalChannelClient externalChannelClient;

    private final AddressDAO addressDAO;

    private final PaperRequestErrorDAO paperRequestErrorDAO;

    private final PnPaperChannelConfig pnPaperChannelConfig;

    private final SqsSender sqsSender;

    private final EventMetaDAO eventMetaDAO;

    private final EventDematDAO eventDematDAO;

    private final MetaDematCleaner metaDematCleaner;

    private final RequestDeliveryDAO requestDeliveryDAO;

    private ConcurrentHashMap<String, MessageHandler> map;


    private final LogMessageHandler logExtChannelsMessageHandler = LogMessageHandler.builder().build();

    @PostConstruct
    public void initializeHandlers() {
        
        MessageHandler saveMetadataMessageHandler = SaveMetadataMessageHandler.builder()
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
        
        MessageHandler saveDematMessageHandler = SaveDematMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventDematDAO(eventDematDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
        
        MessageHandler retryableErrorExtChannelsMessageHandler = RetryableErrorMessageHandler.builder()
                .sqsSender(sqsSender)
                .externalChannelClient(externalChannelClient)
                .addressDAO(addressDAO)
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
        
        MessageHandler notRetryableErrorMessageHandler = NotRetryableErrorMessageHandler.builder()
                .sqsSender(sqsSender)
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .build();
        
        MessageHandler notRetriableWithoutSendErrorMessageHandler = NotRetriableWithoutSendErrorMessageHandler.builder()
                .paperRequestErrorDAO(paperRequestErrorDAO)
                .build();
        
        MessageHandler customAggregatorMessageHandler = CustomAggregatorMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .metaDematCleaner(metaDematCleaner)
                .requestDeliveryDAO(requestDeliveryDAO)
                .build();
        
        MessageHandler aggregatorMessageHandler = AggregatorMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .metaDematCleaner(metaDematCleaner)
                .requestDeliveryDAO(requestDeliveryDAO)
                .build();
         
        MessageHandler directlySendMessageHandler = DirectlySendMessageHandler.builder()
                .sqsSender(sqsSender)
                .requestDeliveryDAO(requestDeliveryDAO)
                .build();
         
        PNAG012MessageHandler pnag012MessageHandler = PNAG012MessageHandler.builder()
                .sqsSender(sqsSender)
                .eventDematDAO(eventDematDAO)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
         
        MessageHandler recag012MessageHandler = RECAG012MessageHandler.builder()
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnag012MessageHandler(pnag012MessageHandler)
                .build();
        
        MessageHandler recag011AMessageHandler = RECAG011AMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();
        
        MessageHandler recag011BMessageHandler = RECAG011BMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventDematDAO(eventDematDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .pnag012MessageHandler(pnag012MessageHandler)
                .build();
        
        MessageHandler recag008CMessageHandler = RECAG008CMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .build();
        
        MessageHandler complex890MessageHandler = Complex890MessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();

        MessageHandler recrn00xcMessageHandler = RECRN00XCMessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .requestDeliveryDAO(requestDeliveryDAO)
                .metaDematCleaner(metaDematCleaner)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();

        MessageHandler recrn011cMessageHandler = RECRN011MessageHandler.builder()
                .sqsSender(sqsSender)
                .eventMetaDAO(eventMetaDAO)
                .pnPaperChannelConfig(pnPaperChannelConfig)
                .build();


        map = new ConcurrentHashMap<>();

        addRetryableErrorStatusCodes(map, retryableErrorExtChannelsMessageHandler);
        addNotRetryableErrorStatusCodes(map, notRetryableErrorMessageHandler);
        addNotRetryableErrorStatusCodeWithoutSend(map, notRetriableWithoutSendErrorMessageHandler);
        addSaveMetadataStatusCodes(map, saveMetadataMessageHandler);
        addSaveDematStatusCodes(map, saveDematMessageHandler);
        addAggregatorStatusCodes(map, aggregatorMessageHandler);
        addDirectlySendStatusCodes(map, directlySendMessageHandler);
        addCustomAggregatorStatusCodes(map, customAggregatorMessageHandler);


        //casi 890
        map.put("RECAG012", recag012MessageHandler);
        map.put("RECAG011A", recag011AMessageHandler);
        map.put("RECAG011B", recag011BMessageHandler);
        map.put("RECAG008C", recag008CMessageHandler);

        map.put("RECAG005C", complex890MessageHandler);
        map.put("RECAG006C", complex890MessageHandler);
        map.put("RECAG007C", complex890MessageHandler);
        map.put("RECRN011", recrn011cMessageHandler);

        map.put("RECRN003C", recrn00xcMessageHandler);
        map.put("RECRN004C", recrn00xcMessageHandler);
        map.put("RECRN005C", recrn00xcMessageHandler);
        map.put("PNAG012", pnag012MessageHandler);
    }

    private void addRetryableErrorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, MessageHandler handler) {
        map.put(ExternalChannelCodeEnum.RECRS006.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN006.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG004.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI005.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRSI005.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS013.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN013.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG013.name(), handler);
    }

    private void addNotRetryableErrorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, MessageHandler handler) {
        map.put(ExternalChannelCodeEnum.CON998.name(), handler);
        map.put(ExternalChannelCodeEnum.CON997.name(), handler);
        map.put(ExternalChannelCodeEnum.CON996.name(), handler);
        map.put(ExternalChannelCodeEnum.CON995.name(), handler);
        map.put(ExternalChannelCodeEnum.CON993.name(), handler);
    }

    private void addNotRetryableErrorStatusCodeWithoutSend(ConcurrentHashMap<String, MessageHandler> map, MessageHandler handler){
        map.put(ExternalChannelCodeEnum.P010.name(), handler);
    }

    private void addSaveMetadataStatusCodes(ConcurrentHashMap<String, MessageHandler> map, MessageHandler handler) {
        map.put("RECRS002A", handler);
        map.put("RECRS002D", handler);
        map.put("RECRS004A", handler);
        map.put("RECRS005A", handler);
        map.put("RECRN001A", handler);
        map.put("RECRN002A", handler);
        map.put("RECRN002D", handler);
        map.put("RECRN003A", handler);
        map.put("RECRN004A", handler);
        map.put("RECRN005A", handler);
        map.put("RECAG001A", handler);
        map.put("RECAG002A", handler);
        map.put("RECAG003A", handler);
        map.put("RECAG003D", handler);
        map.put("RECAG005A", handler);
        map.put("RECAG006A", handler);
        map.put("RECAG007A", handler);
        map.put("RECAG008A", handler);
        map.put("RECRI003A", handler);
        map.put("RECRI004A", handler);
        map.put("RECRSI004A", handler);
    }


    private void addSaveDematStatusCodes(ConcurrentHashMap<String, MessageHandler> map, MessageHandler handler) {
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
        map.put(ExternalChannelCodeEnum.RECAG005B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG006B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG007B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG008B.name(), handler);


    }

    private void addDirectlySendStatusCodes(ConcurrentHashMap<String, MessageHandler> map, MessageHandler handler) {


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
        map.put(ExternalChannelCodeEnum.RECRN010.name(), handler);
    }

    private void addAggregatorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, MessageHandler handler) {
        map.put(ExternalChannelCodeEnum.RECRS002C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS002F.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS004C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS005C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN001C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN002F.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN003C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN004C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN005C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG001C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG002C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003F.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI003C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI004C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRSI003C.name(), handler); //L’evento potrebbe non essere mai generato in quanto non garantito su tutte i diversi paesi internazionali
        map.put(ExternalChannelCodeEnum.RECRSI004C.name(), handler);
    }

    private void addCustomAggregatorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, MessageHandler handler){
        map.put(ExternalChannelCodeEnum.RECRN002C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003C.name(), handler);
    }

    public MessageHandler getHandler(@NotNull String code) {
        return map.getOrDefault(code, logExtChannelsMessageHandler);
    }

}
