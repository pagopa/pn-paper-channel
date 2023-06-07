package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import reactor.core.publisher.Mono;

public interface PaperTenderService {



    Mono<String> getZoneFromCountry(String country);

    Mono<CostDTO> getCostFrom(String cap, String zone, String productType);

}
