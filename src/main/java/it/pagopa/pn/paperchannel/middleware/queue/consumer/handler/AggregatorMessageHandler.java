package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;

// Alla ricezione di questi tipi di eventi, che sono finali per lo specifico prodotto, paper-channel dovrà:
// recuperare l’evento di pre-esito correlato (mediante accesso puntuale su hashkey META##RequestID e sortKey META##statusCode)
// arricchire l’evento finale ricevuto con le eventuali informazioni aggiuntive reperite in tabella (in particolare,
// allo stato dell’arte, tali informazioni sono esclusivamente deliveryFailureCause e discoveredAddress)
// Inviare l’evento arricchito a delivery-push
// cancellate le righe in tabella per legate al requestId per le entità META e DEMAT

@Slf4j
public class AggregatorMessageHandler extends SendToDeliveryPushHandler {


    public AggregatorMessageHandler(SqsSender sqsSender) {
        super(sqsSender);
    }


    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        //TODO da completare
        //recuperare evento pre-esito da db
        //...
        //arricchire l'evento ricevuto con quello recuperato (deliveryFailureCause/discoveredAddress)
        enrichEvent(paperRequest);
        //invio dato su delivery-push
        super.handleMessage(entity, paperRequest);
        //cancellare righe per entità META e DEMAT
        //...
    }

    private void enrichEvent(PaperProgressStatusEventDto paperRequest) {

    }


}
