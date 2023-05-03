package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.dao.common.TransactWriterInitializer;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;

import java.util.List;

public interface AddressDAO {
    Mono<PnAddress> create (PnAddress pnAddress);
    void createTransaction(TransactWriterInitializer transactWriterInitializer, PnAddress pnAddress);
    void createTransaction(TransactWriteItemsEnhancedRequest.Builder builder, PnAddress address);

    Mono<PnAddress> findByRequestId (String requestId);
    Mono<PnAddress> findByRequestId (String requestId, AddressTypeEnum addressTypeEnum);
    Mono<List<PnAddress>> findAllByRequestId (String requestId);
}
