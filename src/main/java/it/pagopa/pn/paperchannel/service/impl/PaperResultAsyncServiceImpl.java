package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.HandlersFactory;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.MessageHandler;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DATA_NULL_OR_INVALID;

@CustomLog
@Service
public class PaperResultAsyncServiceImpl extends BaseService implements PaperResultAsyncService {

    private final HandlersFactory handlersFactory;

    private final String processName = "Result Async Background";

    public PaperResultAsyncServiceImpl(PnAuditLogBuilder auditLogBuilder, RequestDeliveryDAO requestDeliveryDAO,
                                       NationalRegistryClient nationalRegistryClient, SqsSender sqsSender,
                                       HandlersFactory handlersFactory) {
        super(auditLogBuilder, requestDeliveryDAO, null, nationalRegistryClient, sqsSender);
        this.handlersFactory = handlersFactory;
    }

    @Override
    public Mono<Void> resultAsyncBackground(SingleStatusUpdateDto singleStatusUpdateDto, Integer attempt) {
        log.logStartingProcess(processName);
        if (singleStatusUpdateDto == null || singleStatusUpdateDto.getAnalogMail() == null || StringUtils.isBlank(singleStatusUpdateDto.getAnalogMail().getRequestId())) {
            log.error("the message sent from external channel, includes errors. It cannot be processing");
            return Mono.error(new PnGenericException(DATA_NULL_OR_INVALID, DATA_NULL_OR_INVALID.getMessage()));
        }

        if(singleStatusUpdateDto.getAnalogMail().getStatusCode().equals("P000") ){
            log.debug("Received P000 from EC for {}", singleStatusUpdateDto.getAnalogMail().getRequestId());
            return Mono.empty();
        }

        MessageHandler handler = handlersFactory.getHandler(singleStatusUpdateDto.getAnalogMail().getStatusCode());

        String requestId = getPrefixRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId());
        return requestDeliveryDAO.getByRequestId(requestId)
                .doOnNext(entity -> logEntity(entity, singleStatusUpdateDto))
                .flatMap(entity -> updateEntityResult(singleStatusUpdateDto, entity))
                .flatMap(entity -> handler.handleMessage(entity, singleStatusUpdateDto.getAnalogMail()))
                .doOnError(ex ->  log.error("Error in retrieve EC from queue", ex));

    }

    private void logEntity(PnDeliveryRequest entity, SingleStatusUpdateDto singleStatusUpdateDto) {
        log.info("GETTED ENTITY: {}", entity.getRequestId());
        SingleStatusUpdateDto logDto = singleStatusUpdateDto;
        DiscoveredAddressDto discoveredAddressDto = logDto.getAnalogMail().getDiscoveredAddress();
        logDto.getAnalogMail().setDiscoveredAddress(null);
        pnLogAudit.addsBeforeReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel", entity.getRequestId()));
        pnLogAudit.addsSuccessReceive(entity.getIun(), String.format("prepare requestId = %s Response %s from external-channel status code %s", entity.getRequestId(), logDto.toString().replaceAll("\n", ""), entity.getStatusCode()));
        logDto.getAnalogMail().setDiscoveredAddress(discoveredAddressDto);
    }

    private String getPrefixRequestId(String requestId) {
        requestId = Utility.getRequestIdWithoutPrefixClientId(requestId);
        if (requestId.contains(Const.RETRY)) {
            requestId = requestId.substring(0, requestId.indexOf(Const.RETRY));
        }
        return requestId;
    }

    private Mono<PnDeliveryRequest> updateEntityResult(SingleStatusUpdateDto singleStatusUpdateDto, PnDeliveryRequest pnDeliveryRequestMono) {
        RequestDeliveryMapper.changeState(
                pnDeliveryRequestMono,
                singleStatusUpdateDto.getAnalogMail().getStatusCode(),
                singleStatusUpdateDto.getAnalogMail().getStatusDescription(),
                ExternalChannelCodeEnum.getStatusCode(singleStatusUpdateDto.getAnalogMail().getStatusCode()),
                pnDeliveryRequestMono.getProductType(),
                singleStatusUpdateDto.getAnalogMail().getStatusDateTime().toInstant()
        );
        log.logEndingProcess(processName);
        return requestDeliveryDAO.updateData(pnDeliveryRequestMono);
    }

}
