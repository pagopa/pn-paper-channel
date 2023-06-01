package it.pagopa.pn.paperchannel.encryption.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.ApiClient;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.api.RecipientsApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.dto.RecipientTypeDto;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@CustomLog
@Component("dataVaultEncryption")
public class DataVaultEncryptionImpl extends BaseClient implements DataEncryption {

    private RecipientsApi recipientsApi;

    private final PnPaperChannelConfig pnPaperChannelConfig;


    public DataVaultEncryptionImpl(PnPaperChannelConfig pnPaperChannelConfig) {
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient apiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(pnPaperChannelConfig.getClientDataVaultBasepath());
        this.recipientsApi = new RecipientsApi(apiClient);
    }

    @Override
    public String encode(String fiscalCode, String type) {
        String PN_DATA_VAULT_DESCRIPTION = "Data Vault encode";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, PN_DATA_VAULT_DESCRIPTION);
        return this.recipientsApi.ensureRecipientByExternalId(
                (StringUtils.equalsIgnoreCase(type, RecipientTypeDto.PF.getValue()) ? RecipientTypeDto.PF: RecipientTypeDto.PG), fiscalCode)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(25))
                                .filter(throwable ->throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).map(item -> item)
                .onErrorResume(ex -> Mono.error(new PnGenericException(ExceptionTypeEnum.DATA_VAULT_ENCRYPTION_ERROR, ExceptionTypeEnum.DATA_VAULT_ENCRYPTION_ERROR.getMessage())))
                .block();
    }

    @Override
    public String decode(String data) {
        List<String> toDecode = new ArrayList<>();
        toDecode.add(data);
        String PN_DATA_VAULT_DESCRIPTION = "Data Vault decode";
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, PN_DATA_VAULT_DESCRIPTION);
        return this.recipientsApi.getRecipientDenominationByInternalId(toDecode)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(25))
                                .filter(throwable ->throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                .map(BaseRecipientDtoDto::getTaxId)
                .onErrorResume(ex -> {
                    log.error("Error {}", ex.getMessage());
                    return Mono.error(new PnGenericException(ExceptionTypeEnum.DATA_VAULT_DECRYPTION_ERROR, ExceptionTypeEnum.DATA_VAULT_DECRYPTION_ERROR.getMessage()));
                })
                .blockFirst();
    }
}