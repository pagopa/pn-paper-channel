package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.KOReason;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PrepareEventMapper {

    private PrepareEventMapper() {
        throw new IllegalCallerException();
    }

    private static final BaseMapper <PnAddress, AnalogAddress> baseMapperAddress = new BaseMapperImpl<>(PnAddress.class, AnalogAddress.class);

    public static PrepareEvent fromResult(PnDeliveryRequest request, PnAddress address){
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(request.getRequestId());
        entityEvent.setStatusCode(StatusCodeEnum.fromValue(request.getStatusDetail()));

        if (address != null && address.getTtl() != null){
           entityEvent.setReceiverAddress(baseMapperAddress.toDTO(address));
        }

        entityEvent.setStatusDetail(request.getStatusCode());
        entityEvent.setProductType(request.getProductType());
        entityEvent.setStatusDateTime((DateUtils.parseStringTOInstant(request.getStatusDate())));
        return entityEvent;
    }

    public static PrepareEvent toPrepareEvent(PnDeliveryRequest deliveryRequest, Address address, StatusCodeEnum status){
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(deliveryRequest.getRequestId());
        entityEvent.setStatusCode(status);
        if (address != null){
            entityEvent.setReceiverAddress(AddressMapper.toPojo(address));
        }
        entityEvent.setStatusDetail(deliveryRequest.getStatusCode());
        entityEvent.setProductType(deliveryRequest.getProductType());
        entityEvent.setStatusDateTime(Instant.now());
        if (status == StatusCodeEnum.OK)
        {
            enrichWithReplacedF24AttachmentUrls(deliveryRequest, entityEvent);

            enrichWithCategorizedAttachmentResults(deliveryRequest, entityEvent);
        }
        return entityEvent;
    }

    private static void enrichWithReplacedF24AttachmentUrls(PnDeliveryRequest deliveryRequest, PrepareEvent entityEvent) {
        // vado a popolare eventuali url generati da f24, mi baso sul fatto che abbiano il generatedFrom popolato
        // in questa lista, popolo sia gli f24 che verranno spediti, sia quelli eventualmente scartati
        List<String> f24FileKeys = new ArrayList<>(deliveryRequest.getAttachments().stream().filter(x -> x.getGeneratedFrom() != null).map(PnAttachmentInfo::getFileKey).toList());
        if (!CollectionUtils.isEmpty(deliveryRequest.getRemovedAttachments()))
            f24FileKeys.addAll(deliveryRequest.getRemovedAttachments().stream().filter(x -> x.getGeneratedFrom() != null).map(PnAttachmentInfo::getFileKey).toList());

        if (!f24FileKeys.isEmpty())
        {
            entityEvent.setReplacedF24AttachmentUrls(f24FileKeys);
        }
    }

    private static void enrichWithCategorizedAttachmentResults(PnDeliveryRequest deliveryRequest, PrepareEvent entityEvent) {
        CategorizedAttachmentsResult categorizedAttachmentsResult = new CategorizedAttachmentsResult();
        categorizedAttachmentsResult.setAcceptedAttachments(deliveryRequest.getAttachments().stream().map(x -> mapToResultFilter(ResultFilterEnum.SUCCESS, x)).toList());

        // se sono presenti documenti rimossi, popolo la lista corrispondente
        if (!CollectionUtils.isEmpty(deliveryRequest.getRemovedAttachments())) {
            categorizedAttachmentsResult.setDiscardedAttachments(deliveryRequest.getRemovedAttachments().stream().map(x -> mapToResultFilter(ResultFilterEnum.DISCARD, x)).toList());
        } else {
            categorizedAttachmentsResult.setDiscardedAttachments(new ArrayList<>());
        }

        entityEvent.setCategorizedAttachments(categorizedAttachmentsResult);
    }

    @NotNull
    private static ResultFilter mapToResultFilter(ResultFilterEnum result, PnAttachmentInfo x) {
        ResultFilter resultFilter = new ResultFilter();
        resultFilter.setFileKey(x.getFileKey());
        resultFilter.setResult(result);
        resultFilter.setReasonCode(x.getFilterResultCode());
        resultFilter.setReasonDescription(x.getFilterResultDiagnostic());
        return resultFilter;
    }

    public static PrepareEvent toPrepareEvent(PnDeliveryRequest deliveryRequest, Address address, StatusCodeEnum status, KOReason koReason){
        PrepareEvent prepareEvent = toPrepareEvent(deliveryRequest, address, status);
        if(koReason != null) {
            prepareEvent.setFailureDetailCode(koReason.failureDetailCode());
            if(koReason.addressFailed() != null) {
                prepareEvent.setReceiverAddress(AddressMapper.toPojo(koReason.addressFailed()));
            }
        }
        return prepareEvent;
    }

}
