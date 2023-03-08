package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;

// handler salva metadati (vedere Gestione eventi di pre-esito)
public class SaveMetadataMessageHandler implements MessageHandler {


    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        //salva il dato richiamando il dao
        //TODO save in DB
    }
}
