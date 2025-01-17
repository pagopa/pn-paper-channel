package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.*;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.*;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PrepareNormalizeAddressEvent;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@CustomLog
public class PreparePhaseOneAsyncServiceImpl implements PreparePhaseOneAsyncService{

    private static final String PROCESS_NAME = "Prepare Async Phase One";
    private static final String VALIDATION_NAME = "Check and update address";

    private final RequestDeliveryDAO requestDeliveryDAO;
    private final AddressDAO addressDAO;
    private final PnPaperChannelConfig paperChannelConfig;
    private final PaperRequestErrorDAO paperRequestErrorDAO;
    private final PaperAddressService paperAddressService;
    private final PaperCalculatorUtils paperCalculatorUtils;
    private final AttachmentsConfigService attachmentsConfigService;


    @Override
    public Mono<PnDeliveryRequest> preparePhaseOneAsync(PrepareNormalizeAddressEvent prepareNormalizeAddressEvent) {
        log.logStartingProcess(PROCESS_NAME);

        final String requestId = prepareNormalizeAddressEvent.getRequestId();
        Address addressFromNationalRegistry = prepareNormalizeAddressEvent.getAddress();

        return requestDeliveryDAO.getByRequestId(requestId, true)
                .flatMap(deliveryRequest ->
                        checkAndUpdateAddress(deliveryRequest, addressFromNationalRegistry, prepareNormalizeAddressEvent)
                                .flatMap(pnAddress -> attachmentsConfigService.filterAttachmentsToSend(deliveryRequest, deliveryRequest.getAttachments(), pnAddress))
                );

    }

    private Mono<PnAddress> checkAndUpdateAddress(PnDeliveryRequest pnDeliveryRequest, Address fromNationalRegistries, PrepareNormalizeAddressEvent queueModel){
        return this.paperAddressService.getCorrectAddress(pnDeliveryRequest, fromNationalRegistries, queueModel.getAttempt())
                .flatMap(newAddress -> {
                    log.logCheckingOutcome(VALIDATION_NAME, true);
                    pnDeliveryRequest.setAddressHash(newAddress.convertToHash());
                    pnDeliveryRequest.setProductType(this.paperCalculatorUtils.getProposalProductType(newAddress, pnDeliveryRequest.getProposalProductType()));

                    //set flowType per TTL
                    newAddress.setFlowType(Const.PREPARE);
                    return addressDAO.create(AddressMapper.toEntity(newAddress, pnDeliveryRequest.getRequestId(), AddressTypeEnum.RECEIVER_ADDRESS, paperChannelConfig));
                })
                .onErrorResume(PnGenericException.class, ex -> handleAndThrowAgainError(ex, pnDeliveryRequest.getRequestId()));
    }

    private Mono<PnAddress> handleAndThrowAgainError(PnGenericException ex, String requestId) {
        if(ex instanceof PnUntracebleException) {
            // se l'eccezione PnGenericException è di tipo UNTRACEABLE, ALLORA NON SCRIVO L'ERRORE SU DB
            return Mono.error(ex);
        } else {
            // ALTRIMENTI SCRIVO L'ERRORE SU DB
            return traceError(requestId, ex, "CHECK_ADDRESS_FLOW").then(Mono.error(ex));
        }
    }

    private Mono<Void> traceError(String requestId, PnGenericException ex, String flowType){
        String geokey = ex instanceof StopFlowSecondAttemptException stopExc ? stopExc.getGeokey() : null;

        PnRequestError pnRequestError = PnRequestError.builder()
                .requestId(requestId)
                .error(ex.getMessage())
                .flowThrow(flowType)
                .geokey(geokey)
                .build();

        return this.paperRequestErrorDAO.created(pnRequestError).then();
    }
}
