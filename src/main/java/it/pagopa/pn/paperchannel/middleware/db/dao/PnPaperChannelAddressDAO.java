package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelAddress;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PnPaperChannelAddressDAO {

    Mono<PnPaperChannelAddress> create (PnPaperChannelAddress pnPaperChannelAddress);

    Mono<PnPaperChannelAddress> findByRequestId (String requestId);
    Mono<PnPaperChannelAddress> findByRequestId (String requestId, AddressTypeEnum addressTypeEnum);
    Mono<PnPaperChannelAddress> getPaperChannelAddress(String requestId, AddressTypeEnum addressTypeEnum, boolean consistentRead);
    Mono<List<PnPaperChannelAddress>> findAllByRequestId (String requestId);
}