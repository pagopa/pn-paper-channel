package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;

import java.util.ArrayList;
import java.util.List;

//Tutti gli eventi, fatta eccezione di quelli evidenziati (*), dovranno essere memorizzati nella tabella come entità DEMAT.
// Tutti gli eventi che contengono dematerializzazioni del tipo (Plico, 23L, Indagine, AR) dovranno essere inviati come
// eventi PROGRESS verso delivery-push (oltre che salvati a db)
public class SaveDematMessageHandler extends SendToDeliveryPushHandler {

    private static final List<String> ATTACHMENT_TYPES_SEND_TO_DELIVERY_PUSH = List.of(
            "Plico",
            "AR",
            "Indagine",
            "23L"
    );

    public SaveDematMessageHandler(SqsSender sqsSender) {
        super(sqsSender);
    }


    @Override
    public void handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        //negli eventi DEMAT è presente sempre almeno un attachment
        var attachments = new ArrayList<>(paperRequest.getAttachments());
        attachments.forEach(attachmentDetailsDto -> {
            //salvo in DB
            if(sendToDeliveryPush(attachmentDetailsDto.getDocumentType())) {
                //invio a delivery push lo stesso evento quanti sono gli attachment
                //ogni evento inviato avrà l'attachment diverso
                paperRequest.setAttachments(List.of(attachmentDetailsDto));
                super.handleMessage(entity, paperRequest);
            }
        });

    }

    private boolean sendToDeliveryPush(String attachmentType) {
        return ATTACHMENT_TYPES_SEND_TO_DELIVERY_PUSH.contains(attachmentType);
    }

}
