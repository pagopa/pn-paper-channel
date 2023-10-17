package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.F24Client;
import it.pagopa.pn.paperchannel.model.F24Error;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.service.F24Service;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.PaperCalculatorUtils;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ADDRESS_NOT_EXIST;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.F24_WAITING;

@CustomLog
@Service
public class F24ServiceImpl extends GenericService implements F24Service {

    private static final String URL_PROTOCOL_F24 = "f24set";
    private static final String DOCUMENT_TYPE_F24_SET = "PN_F24_SET";
    private static final String SAFESTORAGE_PREFIX = "safestorage://";

    private final F24Client f24Client;
    private final PaperCalculatorUtils paperCalculatorUtils;
    private final AddressDAO addressDAO;


    public F24ServiceImpl(PnAuditLogBuilder auditLogBuilder, F24Client f24Client,
                          SqsSender sqsQueueSender,
                          PaperCalculatorUtils paperCalculatorUtils, AddressDAO addressDAO, RequestDeliveryDAO requestDeliveryDAO) {
        super(auditLogBuilder, sqsQueueSender, requestDeliveryDAO);
        this.f24Client = f24Client;
        this.paperCalculatorUtils = paperCalculatorUtils;
        this.addressDAO = addressDAO;
        this.requestDeliveryDAO = requestDeliveryDAO;
    }

    @Override
    public boolean checkDeliveryRequestAttachmentForF24(PnDeliveryRequest deliveryRequest) {
        Optional<PnAttachmentInfo> optF24Attachment = getF24PnAttachmentInfo(deliveryRequest);
        return optF24Attachment.isPresent();
    }

    @NotNull
    private Optional<PnAttachmentInfo> getF24PnAttachmentInfo(PnDeliveryRequest deliveryRequest) {
        return deliveryRequest.getAttachments().stream().filter(x -> x.getFileKey().startsWith(URL_PROTOCOL_F24)).findFirst();
    }

    @Override
    public Mono<PnDeliveryRequest> preparePDF(PnDeliveryRequest deliveryRequest) {
        Optional<PnAttachmentInfo> optF24Attachment = getF24PnAttachmentInfo(deliveryRequest);

        return optF24Attachment.map(pnAttachmentInfo -> parseF24URL(pnAttachmentInfo.getFileKey())
                    .flatMap(f24AttachmentInfo -> enrichWithAnalogCostIfNeeded(deliveryRequest, f24AttachmentInfo))
                    .doOnSuccess(f24AttachmentInfo -> logAuditBefore("preparePDF requestId = %s, relatedRequestId = %s engaging F24 ", deliveryRequest))
                    .flatMap(f24AttachmentInfo -> f24Client.preparePDF(deliveryRequest.getRequestId(), f24AttachmentInfo.getSetId(), f24AttachmentInfo.getRecipientIndex(), sumCostAndAnalogCost(f24AttachmentInfo)).thenReturn(f24AttachmentInfo))
                    .doOnNext(f24AttachmentInfo -> logAuditSuccess("preparePDF requestId = %s, relatedRequestId = %s successfully sent to F24", deliveryRequest))
                    .map(f24AttachmentInfo -> {
                        RequestDeliveryMapper.changeState(
                                deliveryRequest,
                                F24_WAITING.getCode(),
                                F24_WAITING.getDescription(),
                                F24_WAITING.getDetail(),
                                deliveryRequest.getProductType(),
                                null
                        );
                        deliveryRequest.setCost(f24AttachmentInfo.getAnalogCost());
                        getF24PnAttachmentInfo(deliveryRequest).ifPresent(d -> d.setDocumentType(DOCUMENT_TYPE_F24_SET));

                        return deliveryRequest;
                    })
                    .flatMap(this.requestDeliveryDAO::updateData)
                    .onErrorResume(ex -> catchThrowableAndThrowPnF24FlowException(ex, deliveryRequest.getIun(), deliveryRequest.getRequestId(), deliveryRequest.getRelatedRequestId())))
                .orElseGet(() -> {
                    log.fatal("URL f24set is required and should exist");
                    return Mono.error(new PnInternalException("missing URL f24set on f24serviceImpl", PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR));
                });
    }

    @Override
    public Mono<PnDeliveryRequest> arrangeF24AttachmentsAndReschedulePrepare(String requestId, List<String> generatedUrls) {
        // sistemo gli allegati sostituendoli all'originale, salvo e faccio ripartire l'evento di prepare
        final List<String> normalizedFilekeys = normalizeGeneratedUrls(generatedUrls);
        return requestDeliveryDAO.getByRequestId(requestId)
                        .map(pnDeliveryRequest -> arrangeAttachments(pnDeliveryRequest, normalizedFilekeys))
                        .flatMap(requestDeliveryDAO::updateData)
                        .flatMap(deliveryRequest -> {
                            PrepareAsyncRequest request = new PrepareAsyncRequest(deliveryRequest.getRequestId(), deliveryRequest.getIun(), false, 0);
                            request.setF24ResponseFlow(true);
                            this.sqsSender.pushToInternalQueue(request);
                            return Mono.just(deliveryRequest);
                        })
                .doOnSuccess(deliveryRequest -> f24ResponseLogAuditSuccess(deliveryRequest, normalizedFilekeys))
                .switchIfEmpty(Mono.error(new PnGenericException(DELIVERY_REQUEST_NOT_EXIST, DELIVERY_REQUEST_NOT_EXIST.getMessage())))
                .doOnError(deliveryRequest -> f24ResponseLogAuditFailure(requestId, normalizedFilekeys));

    }

    private PnDeliveryRequest arrangeAttachments(PnDeliveryRequest pnDeliveryRequest, List<String> generatedUrls){
        List<PnAttachmentInfo> attachments = new ArrayList<>();
        pnDeliveryRequest.getAttachments().forEach(pnAttachmentInfo -> {
            if (pnAttachmentInfo.getDocumentType() != null && pnAttachmentInfo.getDocumentType().equals(DOCUMENT_TYPE_F24_SET))
            {
                // nel caso l'attachement fosse di tipo DOCUMENT_TYPE_F24_SET, vado a sovrascriverlo con la lista tornata da f24.
                generatedUrls.forEach(url -> {
                    PnAttachmentInfo newAttachment = new PnAttachmentInfo();
                    newAttachment.setUrl(null);
                    newAttachment.setFileKey(url);
                    newAttachment.setGeneratedFrom(pnAttachmentInfo.getFileKey()); // url originale f24
                    attachments.add(newAttachment);
                });
            }
            else
                attachments.add(pnAttachmentInfo);

        });
        pnDeliveryRequest.setAttachments(attachments);
        return pnDeliveryRequest;
    }

    private List<String> normalizeGeneratedUrls(List<String> generatedUrls){
        return generatedUrls.stream().map(x -> x.startsWith(SAFESTORAGE_PREFIX)?x:(SAFESTORAGE_PREFIX +x)).toList();
    }

    private Integer sumCostAndAnalogCost(F24AttachmentInfo f24AttachmentInfo) {
        return f24AttachmentInfo.getCost()==null? null: f24AttachmentInfo.getCost() + f24AttachmentInfo.getAnalogCost();
    }

    private Mono<F24AttachmentInfo> enrichWithAnalogCostIfNeeded(PnDeliveryRequest deliveryRequest, F24AttachmentInfo attachmentInfo){
        logAuditBefore("preparePDF requestId = %s, relatedRequestId = %s prepare F24", deliveryRequest);

        if (attachmentInfo.getCost() != null && attachmentInfo.getCost() > 0)
        {
            return addressDAO.findByRequestId(deliveryRequest.getRequestId())
                    .switchIfEmpty(Mono.defer(() -> {
                        logAuditSuccess("preparePDF requestId = %s, relatedRequestId = %s Receiver address is not present on DB!!", deliveryRequest);
                        log.error("Receiver Address for {} request id not found on DB", deliveryRequest.getRequestId());
                        throw new PnInternalException(ADDRESS_NOT_EXIST.name(), ADDRESS_NOT_EXIST.getMessage());
                    }))
                    .map(AddressMapper::toDTO)
                    .flatMap(receiverAddress -> paperCalculatorUtils.calculator(deliveryRequest.getAttachments().stream().map(AttachmentMapper::fromEntity).toList(),
                            receiverAddress,
                            ProductTypeEnum.fromValue(deliveryRequest.getProductType()),
                            StringUtils.equals(deliveryRequest.getPrintType(), Const.BN_FRONTE_RETRO)))
                    .map(analogCost -> {
                        // aggiorno il costo
                        attachmentInfo.setAnalogCost(Utility.toCentsFormat(analogCost));
                        return  attachmentInfo;
                    })
                    .doOnNext(pnAddress -> logAuditSuccess("preparePDF requestId = %s, relatedRequestId = %s Receiver address is present on DB, computed cost " + attachmentInfo.getAnalogCost(), deliveryRequest));
        }
        else
            return Mono.just(attachmentInfo)
                    .doOnNext(pnAddress -> logAuditSuccess("preparePDF requestId = %s, relatedRequestId = %s Receiver no need to compute cost, using 0 ", deliveryRequest));
    }

    private Mono<F24AttachmentInfo> parseF24URL(String f24url) {
        try {
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(f24url).build();
            MultiValueMap<String, String> parameters = uriComponents.getQueryParams();
            Integer cost = parameters.containsKey("cost")?Integer.parseInt(parameters.get("cost").get(0)):null;

            return Mono.just(F24AttachmentInfo.builder()
                    .setId(uriComponents.getHost())
                    .recipientIndex(uriComponents.getPathSegments().get(0))
                    .cost(cost)
                    .analogCost(0)
                    .build());

        } catch (Exception e) {
            log.error("cannot parse f24url={}", f24url, e);
            return Mono.error(e);
        }
    }


    @Builder
    @Getter
    @Setter
    private static class F24AttachmentInfo{
        private Integer cost;
        private Integer analogCost;
        private String setId;
        private String recipientIndex;
    }


    private <T> Mono<T> catchThrowableAndThrowPnF24FlowException(Throwable ex, String iun, String requestId, String correlationId) {
        F24Error error = new F24Error();
        error.setIun(iun);
        error.setMessage(ex.getMessage());
        error.setRequestId(requestId);
        error.setRelatedRequestId(correlationId);
        error.setAttempt(0);

        PnF24FlowException pnF24FlowException = new PnF24FlowException(ExceptionTypeEnum.F24_ERROR, error, ex);
        return Mono.error(pnF24FlowException);
    }

    private void logAuditSuccess(String message, PnDeliveryRequest deliveryRequest){
        pnLogAudit.addsSuccessResolveLogic(
                deliveryRequest.getIun(),
                String.format(message,
                        deliveryRequest.getRequestId(),
                        deliveryRequest.getRelatedRequestId())
        );
    }

    private void logAuditBefore(String message, PnDeliveryRequest deliveryRequest){
        pnLogAudit.addsBeforeResolveLogic(
                deliveryRequest.getIun(),
                String.format(message,
                        deliveryRequest.getRequestId(),
                        deliveryRequest.getRelatedRequestId())
        );
    }


    private void f24ResponseLogAuditSuccess(PnDeliveryRequest entity, List<String> generatedUrls) {

            String docs = String.join(",", generatedUrls);

            pnLogAudit.addsSuccessResolveLogic(
                    entity.getIun(),
                    String.format("prepare requestId = %s, relatedRequestId = %s, traceId = %s generated f24 docs = %s Response OK from F24 service",
                            entity.getRequestId(),
                            entity.getRelatedRequestId(),
                            entity.getCorrelationId(),
                            docs));
    }

    private void f24ResponseLogAuditFailure(String requestId, List<String> generatedUrls) {

            String docs = String.join(",", generatedUrls==null?List.of("null"):generatedUrls);

            pnLogAudit.addsFailLog(
                    PnAuditLogEventType.AUD_FD_RESOLVE_LOGIC,
                    requestId,
                    String.format("prepare requestId = %s generated f24 docs = %s error from F24 service",
                            requestId, docs));

    }


}
