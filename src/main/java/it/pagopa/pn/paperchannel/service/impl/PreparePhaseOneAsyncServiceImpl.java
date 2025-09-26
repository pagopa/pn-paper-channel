package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.PnPrepareDelayerToPaperchannelPayload;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.*;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.*;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.model.*;
import it.pagopa.pn.paperchannel.service.*;
import it.pagopa.pn.paperchannel.utils.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Instant;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.SEND_TO_DELAYER;

@Service
@RequiredArgsConstructor
@CustomLog
public class PreparePhaseOneAsyncServiceImpl implements PreparePhaseOneAsyncService {

    private static final String PROCESS_NAME = "Prepare Async Phase One";
    private static final String VALIDATION_NAME = "Check and update address";

    private final RequestDeliveryDAO requestDeliveryDAO;
    private final AddressDAO addressDAO;
    private final PnPaperChannelConfig paperChannelConfig;
    private final PaperRequestErrorDAO paperRequestErrorDAO;
    private final PaperAddressService paperAddressService;
    private final PaperCalculatorUtils paperCalculatorUtils;
    private final AttachmentsConfigService attachmentsConfigService;
    private final PrepareFlowStarter prepareFlowStarter;
    private final PaperTenderService paperTenderService;
    private final PaperChannelDeliveryDriverDAO paperChannelDeliveryDriverDAO;
    private final SqsSender sqsSender;


    @Override
    public Mono<PnDeliveryRequest> preparePhaseOneAsync(PrepareNormalizeAddressEvent event) {
        log.logStartingProcess(PROCESS_NAME);

        final String requestId = event.getRequestId();
        Address addressFromNationalRegistry = event.getAddress();

        return requestDeliveryDAO.getByRequestId(requestId, false)
                .zipWhen(deliveryRequest -> checkAndUpdateAddress(deliveryRequest, addressFromNationalRegistry, event)
                        .onErrorResume(PnUntracebleException.class, ex -> this.handleUntraceableError(deliveryRequest, event, ex)))
                .flatMap(deliveryRequestWithAddress -> attachmentsConfigService
                        .filterAttachmentsToSend(deliveryRequestWithAddress.getT1(), AttachmentsConfigUtils.getAllAttachments(deliveryRequestWithAddress.getT1()), deliveryRequestWithAddress.getT2())
                        .thenReturn(deliveryRequestWithAddress))
                .flatMap(deliveryRequestWithAddress ->  Utility.isNational(deliveryRequestWithAddress.getT2().getCountry()) ?
                        prepareAndSendToPhaseOneOutput(deliveryRequestWithAddress) : sendToPhaseTwoQueue(deliveryRequestWithAddress))
                .flatMap(this::updateRequestInSendToDelayer)
                .doOnNext(deliveryRequest -> {
                    log.info("End of prepare async phase one");
                    log.logEndingProcess(PROCESS_NAME);
                })
                .onErrorResume(ex -> handlePrepareAsyncError(requestId, ex));
    }

    private Mono<PnDeliveryRequest> sendToPhaseTwoQueue(Tuple2<PnDeliveryRequest, PnAddress> deliveryRequestWithAddress) {
        PnPrepareDelayerToPaperchannelPayload payload = PnPrepareDelayerToPaperchannelPayload.builder()
                .requestId(deliveryRequestWithAddress.getT1().getRequestId())
                .iun(deliveryRequestWithAddress.getT1().getIun())
                .attempt(0)
                .build();
        return Mono.fromRunnable(() -> sqsSender.pushToDelayerToPaperchennelQueue(payload)).thenReturn(deliveryRequestWithAddress.getT1())
                .doOnNext(deliveryRequest -> log.info("Foreign address, sent to phase two queue"));
    }

    private Mono<PnDeliveryRequest> prepareAndSendToPhaseOneOutput(Tuple2<PnDeliveryRequest, PnAddress> deliveryRequestWithAddress) {
        return paperTenderService.getSimplifiedCost(deliveryRequestWithAddress.getT2().getCap(), deliveryRequestWithAddress.getT1().getProductType())
                .doOnNext(pnPaperChannelCostDTO -> deliveryRequestWithAddress.getT1().setTenderCode(pnPaperChannelCostDTO.getTenderId()))
                .flatMap(cost -> paperChannelDeliveryDriverDAO.getByDeliveryDriverId(cost.getDeliveryDriverId()))
                .map(PaperChannelDeliveryDriver::getUnifiedDeliveryDriver)
                .doOnNext(unifiedDeliveryDriver -> prepareFlowStarter.pushPreparePhaseOneOutput(deliveryRequestWithAddress.getT1(), deliveryRequestWithAddress.getT2(), unifiedDeliveryDriver))
                .thenReturn(deliveryRequestWithAddress.getT1());
    }

    private Mono<PnDeliveryRequest> updateRequestInSendToDelayer(PnDeliveryRequest pnDeliveryRequest){
        RequestDeliveryMapper.changeState(
                pnDeliveryRequest,
                SEND_TO_DELAYER.getCode(),
                SEND_TO_DELAYER.getDescription(),
                SEND_TO_DELAYER.getDetail(),
                pnDeliveryRequest.getProductType(),
                null
        );

        return this.requestDeliveryDAO.updateDataWithoutGet(pnDeliveryRequest, false);
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

    private Mono<PnAddress> handleUntraceableError(PnDeliveryRequest deliveryRequest, PrepareNormalizeAddressEvent request, PnUntracebleException ex) {
        log.error("UNTRACEABLE Error prepare async requestId {}, {}", deliveryRequest.getRequestId(), ex.getMessage(), ex);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.UNTRACEABLE;

        RequestDeliveryMapper.changeState(
                deliveryRequest,
                statusDeliveryEnum.getCode(),
                statusDeliveryEnum.getDescription(),
                statusDeliveryEnum.getDetail(),
                null,
                null
        );

        return updateStatus(deliveryRequest.getRequestId(), statusDeliveryEnum)
                .doOnNext(requestIdString -> {
                    sendUnreachableEvent(deliveryRequest, request.getClientId(), ex.getKoReason());
                    log.logEndingProcess(PROCESS_NAME);
                })
                .flatMap(entity -> Mono.error(ex));
    }

    private Mono<PnDeliveryRequest> handlePrepareAsyncError(String requestId, Throwable ex) {
        log.error("Error prepare async requestId {}, {}", requestId, ex.getMessage(), ex);
        if (ex instanceof PnAddressFlowException || ex instanceof PnUntracebleException) return Mono.error(ex);

        StatusDeliveryEnum statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;


        if(ex instanceof PnGenericException) {
            statusDeliveryEnum = StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR;
        }

        return updateStatus(requestId, statusDeliveryEnum)
                .doOnNext(o -> log.logEndingProcess(PROCESS_NAME))
                .flatMap(entity -> Mono.error(ex));
    }


    private Mono<String> updateStatus(String requestId, StatusDeliveryEnum status ){
        String processName = "Update Status";
        log.logStartingProcess(processName);

        var statusCode = status.getCode();
        var completeDescription = new StringBuilder(status.getCode());
        var statusDate = DateUtils.formatDate(Instant.now());
        var statusDetail = status.getDetail();

        if (StringUtils.isNotBlank(status.getDescription())) {
            completeDescription.append(" - ").append(status.getDescription());
        }

        return this.requestDeliveryDAO.updateStatus(requestId, statusCode, completeDescription.toString(), statusDetail, statusDate).thenReturn(requestId)
                .doOnSuccess(requestIdString -> log.logEndingProcess(processName));

    }

    private void sendUnreachableEvent(PnDeliveryRequest request, String clientId, KOReason koReason){
        log.debug("Send Unreachable Event request id - {}, iun - {}", request.getRequestId(), request.getIun());
        prepareFlowStarter.pushResultPrepareEvent(request, null, clientId, StatusCodeEnum.KO, koReason);
    }

}
