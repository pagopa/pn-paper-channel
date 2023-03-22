package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.ApiClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.api.PaperMessagesApi;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperEngageRequestAttachmentsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperEngageRequestDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;


import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pagopa.pn.paperchannel.utils.Const.PN_AAR;

@Slf4j
@Component
public class ExternalChannelClientImpl extends BaseClient implements ExternalChannelClient {
    private final PnPaperChannelConfig pnPaperChannelConfig;

    private PaperMessagesApi paperMessagesApi;

    public ExternalChannelClientImpl(PnPaperChannelConfig pnPaperChannelConfig) {
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(this.pnPaperChannelConfig.getClientExternalChannelBasepath());
        this.paperMessagesApi = new PaperMessagesApi(newApiClient);
    }

    public Mono<Void> sendEngageRequest(SendRequest sendRequest, List<AttachmentInfo> attachments){
        String requestIdx = sendRequest.getRequestId();
        PaperEngageRequestDto dto = new PaperEngageRequestDto();
        dto.setRequestId(sendRequest.getRequestId());
        dto.setRequestPaId(sendRequest.getRequestPaId());
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
            PaperEngageRequestAttachmentsDto attachmentsDto = new PaperEngageRequestAttachmentsDto();
            attachmentsDto.setDocumentType(StringUtils.equals(a.getDocumentType(), PN_AAR) ? Const.AAR : Const.ATTO);
            attachmentsDto.setSha256(a.getSha256());
            attachmentsDto.setOrder(new BigDecimal(i.getAndIncrement()));
            attachmentsDto.setUri(a.getFileKey());
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

        return this.paperMessagesApi.sendPaperEngageRequest(requestIdx, this.pnPaperChannelConfig.getXPagopaExtchCxId(), dto)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                );
    }

}
