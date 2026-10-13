package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.model.PnPaperChannelCostDTO;
import reactor.core.publisher.Mono;


public interface PaperTenderService {
    Mono<PnPaperChannelCostDTO> getSimplifiedCost(String geokey, String productType);
    Mono<PnPaperChannelCostDTO> getCostFromTenderId(String tenderId, String geokey, String productType);
}