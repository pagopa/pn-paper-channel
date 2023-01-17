package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.ApiClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.api.PaperMessagesApi;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperEngageRequestDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;


import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

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

    public Mono<Void> sendEngageRequest(SendRequest sendRequest){
        String requestIdx = sendRequest.getRequestId();
        PaperEngageRequestDto dto = new PaperEngageRequestDto();
        dto.setRequestId(sendRequest.getRequestId());
        dto.setRequestPaId(sendRequest.getRequestPaId());
        dto.setClientRequestTimeStamp(DateUtils.getOffsetDateTimeFromDate(sendRequest.getClientRequestTimeStamp()));
        dto.setProductType(setProductType(sendRequest.getProductType().getValue()));
        dto.setReceiverFiscalCode(sendRequest.getReceiverFiscalCode());
        if (!sendRequest.getAttachmentUrls().isEmpty())
            dto.setAttachmentUri(sendRequest.getAttachmentUrls().get(0));

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

    private String setProductType(String productType){
        String type = null;
        if (StringUtils.equals(productType, ProductTypeEnum.RN_AR.getValue())
                || StringUtils.equals(productType, ProductTypeEnum.RI_AR.getValue())) {
            type = Const.RACCOMANDATA_AR;

        } else if (StringUtils.equals(productType, ProductTypeEnum.RN_RS.getValue())
                || StringUtils.equals(productType, ProductTypeEnum.RI_RS.getValue())) {
            type = Const.RACCOMANDATA_SEMPLICE;

        } else if (StringUtils.equals(productType, ProductTypeEnum.RN_890.getValue())) {
            type = Const.RACCOMANDATA_890;
        }

        return type;
    }

}
