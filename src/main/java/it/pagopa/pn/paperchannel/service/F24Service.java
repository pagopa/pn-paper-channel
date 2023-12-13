package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public interface F24Service {

    /**
     * Controlla se necessario ingaggiare f24
     *
     * @param request request corrente
     * @return true se presente un url f24 da processare
     */
    boolean checkDeliveryRequestAttachmentForF24(PnDeliveryRequest request);

    /**
     * Ingaggia pn-F24, da invocare solo se presente un url di f24set:// tra gli attachments
     *
     * @param request request corrente
     * @return PnDeliveryRequest aggiornata
     */
    Mono<PnDeliveryRequest> preparePDF(PnDeliveryRequest request);

    /**
     * Sistema gli allegati con quelli generati da F24 e ingaggia nuovamente la prepare per proseguire
     *
     * @param requestId requestId
     * @param generatedUrls lista url generati
     * @return request aggiornata
     */
    Mono<PnDeliveryRequest> arrangeF24AttachmentsAndReschedulePrepare(String requestId, List<String> generatedUrls);

}
