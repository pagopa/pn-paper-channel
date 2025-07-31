package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.SendRequestMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.PcRetryUtils;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXTERNAL_CHANNEL_API_EXCEPTION;

// handler per stati gialli: Retry su ExtCh con suffisso +1, invio dello stato in progress verso DP
@Slf4j
@SuperBuilder
public class RetryableErrorMessageHandler extends SendToDeliveryPushHandler {

    private static final String REQUEST_TO_EXTERNAL_CHANNEL = "prepare requestId = %s, trace_id = %s  request to External Channel";

    private final ExternalChannelClient externalChannelClient;
    private final AddressDAO addressDAO;
    private final PaperRequestErrorDAO paperRequestErrorDAO;
    private final PcRetryUtils pcRetryUtils;

    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {

        if (pcRetryUtils.hasOtherAttempt(paperRequest.getRequestId())) {
            //invio di nuovo la richiesta a ext-channels
            return sendEngageRequest(entity, pcRetryUtils.setRetryRequestId(paperRequest.getRequestId()))
                    .flatMap(pnDeliveryRequest -> super.handleMessage(entity, paperRequest));
        } else {

            PnRequestError pnRequestError = PnRequestError.builder()
                    .requestId(entity.getRequestId())
                    .paId(entity.getRequestPaId())
                    .error(EXTERNAL_CHANNEL_API_EXCEPTION.getMessage())
                    .flowThrow(EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name())
                    .build();

            return paperRequestErrorDAO
                    .created(pnRequestError)
                    .flatMap(requestError -> super.handleMessage(entity, paperRequest));
        }

    }

    private Mono<PnDeliveryRequest> sendEngageRequest(PnDeliveryRequest pnDeliveryRequest, String requestId) {

        return addressDAO.findAllByRequestId(pnDeliveryRequest.getRequestId())
                .flatMap(pnAddresses -> callExternalChannel(pnAddresses, pnDeliveryRequest, requestId));

    }

    private Mono<PnDeliveryRequest> callExternalChannel(List<PnAddress> pnAddresses, PnDeliveryRequest pnDeliveryRequest, String requestId) {
        PnLogAudit pnLogAudit = new PnLogAudit();

        SendRequest sendRequest = SendRequestMapper.toDto(pnAddresses, pnDeliveryRequest);
        sendRequest.setRequestId(requestId);
        pnLogAudit.addsBeforeSend(sendRequest.getIun(), String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)));

        List<AttachmentInfo> attachmentInfos = pnDeliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList();

        return externalChannelClient.sendEngageRequest(sendRequest, attachmentInfos, pnDeliveryRequest.getApplyRasterization())
                .doOnSuccess(unused -> pnLogAudit.addsSuccessSend(sendRequest.getIun(),
                        String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)))
                )
                .doOnError(ex ->
                    pnLogAudit.addsWarningSend(sendRequest.getIun(), String.format(REQUEST_TO_EXTERNAL_CHANNEL, sendRequest.getRequestId(), MDC.get(MDCUtils.MDC_TRACE_ID_KEY)))
                )
                .thenReturn(pnDeliveryRequest);

    }

}
