package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import reactor.core.publisher.Mono;

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
     * @param queueModel queuemodel corrente
     * @return PnDeliveryRequest aggiornata
     */
    Mono<PnDeliveryRequest> preparePDF(PnDeliveryRequest request, PrepareAsyncRequest queueModel);

}
