package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;

// Tutti gli eventi, fatta eccezione di quelli evidenziati (*), dovranno essere memorizzati nella tabella come entità DEMAT.
// Gli altri eventi di demat, quelli (*), vengono gestiti da DematDeliveryPushExtChannelsMessageHandler
public class DematMessageHandler implements MessageHandler {


    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        //salva il dato richiamando il DAO
    }
}
