package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.api.PaperMessagesApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.api.PaperRequestMetadataPatchApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperEngageRequestAttachmentsInnerDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperEngageRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.RequestMetadataPatchRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pagopa.pn.paperchannel.utils.Const.PN_AAR;


@CustomLog
@Component
public class ExternalChannelClientImpl extends BaseClient implements ExternalChannelClient {

    private static final String PN_EXTERNAL_CHANNEL_DESCRIPTION = "External Channel sendEngageRequest";
    private static final String PN_EXTERNAL_CHANNEL_REWORK_DESCRIPTION = "External Channel patchRequestMetadata";


    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final PaperMessagesApi paperMessagesApi;
    private final PaperRequestMetadataPatchApi paperRequestMetadataPatchApi;



    public ExternalChannelClientImpl(PnPaperChannelConfig pnPaperChannelConfig, PaperMessagesApi paperMessagesApi, PaperRequestMetadataPatchApi paperRequestMetadataPatchApi) {
        this.pnPaperChannelConfig = pnPaperChannelConfig;
        this.paperMessagesApi = paperMessagesApi;
        this.paperRequestMetadataPatchApi = paperRequestMetadataPatchApi;
    }


    public Mono<Void> sendEngageRequest(SendRequest sendRequest, List<AttachmentInfo> attachments, Boolean applyRasterization) {
        return Mono.defer(() -> {
            log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, PN_EXTERNAL_CHANNEL_DESCRIPTION, sendRequest.getRequestId());
            String requestIdx = sendRequest.getRequestId();
            var dto = buildPaperEngageRequest(sendRequest, attachments, applyRasterization);

            return this.paperMessagesApi.sendPaperEngageRequest(requestIdx, this.pnPaperChannelConfig.getXPagopaExtchCxId(), dto);
        });

    }

    public Mono<Void> initNotificationRework(String requestIdx) {
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_CHANNELS, PN_EXTERNAL_CHANNEL_REWORK_DESCRIPTION, requestIdx);
        var dto = new RequestMetadataPatchRequestDto();
        dto.setIsOpenReworkRequest(Boolean.TRUE);
        return this.paperRequestMetadataPatchApi.patchRequestMetadata(requestIdx, this.pnPaperChannelConfig.getXPagopaExtchCxId(), dto);
    }

    private PaperEngageRequestDto buildPaperEngageRequest(SendRequest sendRequest, List<AttachmentInfo> attachments, Boolean applyRasterization) {
        PaperEngageRequestDto dto = new PaperEngageRequestDto();
        dto.setRequestId(sendRequest.getRequestId());
        dto.setRequestPaId(sendRequest.getRequestPaId());
        if(!(pnPaperChannelConfig.getRequestPaIdOverride().isBlank()) && pnPaperChannelConfig.getRequestPaIdOverride()!=null ){
            dto.setRequestPaId(pnPaperChannelConfig.getRequestPaIdOverride());
        }
        dto.setClientRequestTimeStamp(OffsetDateTime.now());
        if (sendRequest.getClientRequestTimeStamp() != null){
            dto.setClientRequestTimeStamp(DateUtils.getOffsetDateTimeFromDate(sendRequest.getClientRequestTimeStamp()));
        }

        dto.setProductType(sendRequest.getProductType().getValue());
        dto.setReceiverFiscalCode(sendRequest.getReceiverFiscalCode());
        AtomicInteger i = new AtomicInteger();
        List<AttachmentInfo> mutableList = new ArrayList<>(attachments);
        Collections.sort(mutableList);
        mutableList.forEach(a -> {
            var attachmentsDto = new PaperEngageRequestAttachmentsInnerDto();
            attachmentsDto.setDocumentType(StringUtils.equals(a.getDocumentType(), PN_AAR) ? Const.AAR : Const.ATTO);
            attachmentsDto.setSha256(a.getSha256());
            attachmentsDto.setOrder(new BigDecimal(i.getAndIncrement()));
            attachmentsDto.setUri(AttachmentsConfigUtils.cleanFileKey(a.getFileKey(), false));
            dto.getAttachments().add(attachmentsDto);
        });

        dto.setPrintType(sendRequest.getPrintType());
        dto.setReceiverNameRow2(sendRequest.getReceiverAddress().getNameRow2());
        dto.setReceiverName(sendRequest.getReceiverAddress().getFullname());
        dto.setReceiverAddress(sendRequest.getReceiverAddress().getAddress());
        dto.setReceiverAddressRow2(sendRequest.getReceiverAddress().getAddressRow2());
        dto.setReceiverCap(sendRequest.getReceiverAddress().getCap());
        dto.setReceiverCity(sendRequest.getReceiverAddress().getCity());
        dto.setReceiverCity2(sendRequest.getReceiverAddress().getCity2());
        dto.setReceiverPr(sendRequest.getReceiverAddress().getPr());
        dto.setReceiverCountry(sendRequest.getReceiverAddress().getCountry());

        dto.setSenderName(sendRequest.getSenderAddress().getFullname());
        dto.setSenderAddress(sendRequest.getSenderAddress().getAddress());
        dto.setSenderCity(sendRequest.getSenderAddress().getCity());
        dto.setSenderPr(sendRequest.getSenderAddress().getPr());

        if (sendRequest.getArAddress() != null){
            dto.setArName(sendRequest.getArAddress().getFullname());
            dto.setArAddress(sendRequest.getArAddress().getAddress());
            dto.setArCap(sendRequest.getArAddress().getCap());
            dto.setArCity(sendRequest.getArAddress().getCity());
        }
        dto.setApplyRasterization(applyRasterization);

        return dto;
    }

}
