package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;

//effettuare PUT di una nuova riga in accordo con le specifiche
//in caso di RECAG012 la document date coincide con la data di produzione dell’evento
//In caso di ricezione di RECAG012 il documentType non sarà presente, il campo DOCUMENT_TYPE  va quindi popolato con una stringa fissa
public class RECAG012MessageHandler implements MessageHandler {

    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        //TODO da completare
    }
}
