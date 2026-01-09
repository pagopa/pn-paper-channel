package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.dto.PaperAddressResponseDto;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnPaperChannelAddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperChannelAddress;
import it.pagopa.pn.paperchannel.middleware.msclient.DataVaultClient;
import it.pagopa.pn.paperchannel.service.PaperChannelAddressService;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ADDRESS_NOT_EXIST;
import static it.pagopa.pn.paperchannel.utils.Utility.logAuditBeforeLogic;
import static it.pagopa.pn.paperchannel.utils.Utility.logAuditSuccessLogic;

@Slf4j
@Service
public class PaperChannelAddressServiceImpl implements PaperChannelAddressService {

    private final AddressDAO addressDAO;
    private final PnPaperChannelAddressDAO paperChannelAddressDAO;
    private final DataVaultClient dataVaultClient;

    public PaperChannelAddressServiceImpl(AddressDAO addressDAO, DataVaultClient dataVaultClient, PnPaperChannelAddressDAO paperChannelAddressDAO) {
        this.addressDAO = addressDAO;
        this.paperChannelAddressDAO = paperChannelAddressDAO;
        this.dataVaultClient = dataVaultClient;
    }

    private Mono<PnPaperChannelAddress> getCorrectAddress(PnDeliveryRequest deliveryRequest, boolean normalized) {
        PnLogAudit pnLogAudit = new PnLogAudit();
        logAuditBeforeLogic("prepare requestId = %s, relatedRequestId = %s Is Receiver Address First Attempt present ?", deliveryRequest, pnLogAudit);
        var requestIdFirstAttempt = Utility.getRequestIdFirstAttempt(deliveryRequest);
        return this.addressDAO.findByRequestId(requestIdFirstAttempt, AddressTypeEnum.RECEIVER_ADDRESS)
                .switchIfEmpty(Mono.defer(() -> {
                    logAuditSuccessLogic("prepare requestId = %s, relatedRequestId = %s Receiver address First Attempt is not present on DB", deliveryRequest, pnLogAudit);
                    log.error("Receiver Address for {} request id not found on DB", deliveryRequest.getRequestId());
                    throw new PnGenericException(ADDRESS_NOT_EXIST, ADDRESS_NOT_EXIST.getMessage());
                }))
                .flatMap(address -> createPaperAddress(deliveryRequest.getIun(), address, normalized)
                        .zipWith(Mono.just(address)))
                .flatMap(tuple -> saveAddress(tuple.getT1(), tuple.getT2()));
    }

    private Mono<PaperAddressResponseDto> createPaperAddress(String iun, PnAddress address, boolean normalized) {
        log.info("Creating paper address in Data Vault for requestId: {}", address.getRequestId());
        return dataVaultClient.createPaperAddress(iun, AddressMapper.toPaperAddressRequestInternalDto(address, normalized), AddressTypeEnum.RECEIVER_ADDRESS);
    }

    private Mono<PnPaperChannelAddress> saveAddress(PaperAddressResponseDto addressResponse, PnAddress address) {
        log.info("Saving paper channel address on pn-PaperChannelAddress for requestId: {}", address.getRequestId());
        return paperChannelAddressDAO.create(AddressMapper.getPnPaperChannelAddress(addressResponse, address));
    }

}
