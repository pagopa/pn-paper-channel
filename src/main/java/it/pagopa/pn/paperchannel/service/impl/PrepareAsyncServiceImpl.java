package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.pojo.Address;
import it.pagopa.pn.paperchannel.queue.model.DeliveryPayload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

@Service
public class PrepareAsyncServiceImpl {


    /**
     *
     * @param correlationId passato nel caso in cui recuperiamo indirizzo da NationalRegistry
     * @param requestId passato dal service in caso di indirizzo già presente
     * @param address sempre presente
     * @return DeliveryPayload, oggetto da mandare sulla coda di DeliveryPush
     */
    public Mono<DeliveryPayload> prepareAsync(String correlationId, String requestId, @NotNull Address address){

        // recuperare l'entity in base al correlation o request id

        // salviamo in tabella Address l'indirizzo associato al requestID

        // recuperiamo il contratto per conoscere il costo della spedizione

        // recuperiamo gli allegati da Safe Storage e ci calcoliamo il prezzo per pagina dei documenti

        return Mono.empty();
    }
}
