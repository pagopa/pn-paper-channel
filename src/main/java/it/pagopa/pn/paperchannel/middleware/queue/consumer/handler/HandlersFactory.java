package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
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

    private ConcurrentHashMap<String, MessageHandler> map;


    private final LogMessageHandler logExtChannelsMessageHandler = new LogMessageHandler();

    @PostConstruct
    public void initializeHandlers() {
        var saveMetadataMessageHandler = new SaveMetadataMessageHandler(eventMetaDAO, pnPaperChannelConfig.getTtlExecutionDaysMeta());
        var saveDematMessageHandler = new SaveDematMessageHandler(sqsSender, eventDematDAO, pnPaperChannelConfig.getTtlExecutionDaysDemat());
        var retryableErrorExtChannelsMessageHandler = new RetryableErrorMessageHandler(sqsSender, externalChannelClient, addressDAO, paperRequestErrorDAO, pnPaperChannelConfig);
        var notRetryableErrorMessageHandler = new NotRetryableErrorMessageHandler(paperRequestErrorDAO);
        var aggregatorMessageHandler = new AggregatorMessageHandler(sqsSender, eventMetaDAO, metaDematCleaner);
        var directlySendMessageHandler = new DirectlySendMessageHandler(sqsSender);
        var recag012MessageHandler = new RECAG012MessageHandler(eventMetaDAO, pnPaperChannelConfig.getTtlExecutionDaysMeta());
        var recag011AMessageHandler =  new RECAG011AMessageHandler(sqsSender, eventMetaDAO, pnPaperChannelConfig.getTtlExecutionDaysMeta());
        var recag011BMessageHandler = new RECAG011BMessageHandler(sqsSender, eventDematDAO, pnPaperChannelConfig.getTtlExecutionDaysDemat(), eventMetaDAO, pnPaperChannelConfig.getTtlExecutionDaysMeta());
        var recag008CMessageHandler = new RECAG008CMessageHandler(sqsSender, eventMetaDAO, metaDematCleaner);
        var complex890MessageHandler = new Complex890MessageHandler(sqsSender, eventMetaDAO, metaDematCleaner);
        var recrn00xcMessageHandler = new RECRN00XCMessageHandler(sqsSender, eventMetaDAO, metaDematCleaner);
        var recrn011cMessageHandler = new RECRN011MessageHandler(sqsSender, eventMetaDAO, pnPaperChannelConfig.getTtlExecutionDaysMeta());

        map = new ConcurrentHashMap<>();

        addRetryableErrorStatusCodes(map, retryableErrorExtChannelsMessageHandler);
        addNotRetryableErrorStatusCodes(map, notRetryableErrorMessageHandler);
        addSaveMetadataStatusCodes(map, saveMetadataMessageHandler);
        addSaveDematStatusCodes(map, saveDematMessageHandler);
        addAggregatorStatusCodes(map, aggregatorMessageHandler);
        addDirectlySendStatusCodes(map, directlySendMessageHandler);


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
        map.put(ExternalChannelCodeEnum.CON996.name(), handler);
        map.put(ExternalChannelCodeEnum.CON995.name(), handler);
        map.put(ExternalChannelCodeEnum.CON993.name(), handler);
    }

    private void addSaveMetadataStatusCodes(ConcurrentHashMap<String, MessageHandler> map, SaveMetadataMessageHandler handler) {
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

//    private void addDemaSavedStatusCodes(ConcurrentHashMap<DematKey, MessageHandler> map, DematMessageHandler handler) {
//
//        map.put(new DematKey("RECAG002B", "CAN"), handler);
//
//    }


//    private void addDematDeliveryPushStatusCodes(ConcurrentHashMap<DematKey, MessageHandler> map, SaveDematMessageHandler handler) {
//        map.put(new DematKey("RECRS002B", "Plico"), handler);
//        map.put(new DematKey("RECRS002E", "Plico"), handler);
//        map.put(new DematKey("RECRS004B", "Plico"), handler);
//        map.put(new DematKey("RECRS005B", "Plico"), handler);
//        map.put(new DematKey("RECRN001B", "AR"), handler);
//        map.put(new DematKey("RECRN002B", "Plico"), handler);
//        map.put(new DematKey("RECRN002E", "Plico"), handler);
//        map.put(new DematKey("RECRN002E", "Indagine"), handler);
//        map.put(new DematKey("RECRN003B", "AR"), handler);
//        map.put(new DematKey("RECRN004B", "Plico"), handler);
//        map.put(new DematKey("RECRN005B", "Plico"), handler);
//        map.put(new DematKey("RECAG001B", "23L"), handler);
//        map.put(new DematKey("RECAG002B", "23L"), handler); //CAN è una raccomandata semplice che, se consegnata, non ha materialità da dematerializzare. in caso non fosse recapitata avrà una dematerializzazione del plico di tipo CAN
//        map.put(new DematKey("RECAG003B", "Plico"), handler);
//        map.put(new DematKey("RECAG003E", "Plico"), handler);
//        map.put(new DematKey("RECAG003E", "Indagine"), handler);
//        map.put(new DematKey("RECRI003B", "AR"), handler);
//        map.put(new DematKey("RECRI004B", "Plico"), handler);
//        map.put(new DematKey("RECRSI004B", "Plico"), handler);
//
//    }

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
        map.put(ExternalChannelCodeEnum.RECAG005B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG006B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG007B.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG008B.name(), handler);


    }

    private void addDirectlySendStatusCodes(ConcurrentHashMap<String, MessageHandler> map, DirectlySendMessageHandler handler) {


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
    }

    private void addAggregatorStatusCodes(ConcurrentHashMap<String, MessageHandler> map, AggregatorMessageHandler handler) {
        map.put(ExternalChannelCodeEnum.RECRS002C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS002F.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS004C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRS005C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN001C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN002C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN002F.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN003C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN004C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRN005C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG001C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG002C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECAG003F.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI003C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRI004C.name(), handler);
        map.put(ExternalChannelCodeEnum.RECRSI003C.name(), handler); //L’evento potrebbe non essere mai generato in quanto non garantito su tutte i diversi paesi internazionali
        map.put(ExternalChannelCodeEnum.RECRSI004C.name(), handler);
    }

    public MessageHandler getHandler(@NotNull String code) {
        return map.getOrDefault(code, logExtChannelsMessageHandler);
    }

}
