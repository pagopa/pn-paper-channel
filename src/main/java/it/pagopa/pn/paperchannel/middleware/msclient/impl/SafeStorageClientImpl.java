package it.pagopa.pn.paperchannel.middleware.msclient.impl;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.paperchannel.middleware.msclient.common.BaseClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.ApiClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;


@Slf4j
@Component
public class SafeStorageClientImpl extends BaseClient implements SafeStorageClient {
    private final PnPaperChannelConfig pnPaperChannelConfig;
    private FileDownloadApi fileDownloadApi;

    public SafeStorageClientImpl(PnPaperChannelConfig pnPaperChannelConfig) {
        this.pnPaperChannelConfig = pnPaperChannelConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(this.pnPaperChannelConfig.getClientSafeStorageBasepath());
        this.fileDownloadApi = new FileDownloadApi(newApiClient);
    }


    @Override
    public Mono<FileDownloadResponseDto> getFile(String fileKey) {
        String reqFileKey = fileKey;
        log.info("Getting file with {} key", fileKey);
        String BASE_URL = "safestorage://";
        if (fileKey.contains(BASE_URL)){
            fileKey = fileKey.replace(BASE_URL, "");
        }
        log.debug("Req params : {}", fileKey);

        return fileDownloadApi.getFile(fileKey, this.pnPaperChannelConfig.getSafeStorageCxId(), false)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                .map(response -> {
                    if(response.getDownload() != null && response.getDownload().getRetryAfter() != null) {
                        throw new PnRetryStorageException(response.getDownload().getRetryAfter());
                    }
                    response.setKey(reqFileKey);
                    return response;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(ex);
                /*
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND){
                    return Mono.error(new RaddGenericException(RETRY_AFTER, new BigDecimal(670)));
                }
                return Mono.error(new PnSafeStorageException(ex));
                */
                });
    }
}
