package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.paperchannel.exception.PnDownloadException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import lombok.CustomLog;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;

@Component
@CustomLog
public class HttpConnectorWebClient implements HttpConnector {

    private final WebClient webClient;

    public HttpConnectorWebClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Mono<PDDocument> downloadFile(String url) {
        return downloadFileAsByteArray(url)
                .map(bytes -> buildPDDocument(url, bytes));
    }

    private PDDocument buildPDDocument(String url, byte[] bytes) {
        try {
            return PDDocument.load(bytes);
        }
        catch (IOException e) {
            throw new PnDownloadException("Error load PDF for url " + url, e);
        }

    }

    public Mono<byte[]> downloadFileAsByteArray(String url) {
        log.info("Url to download: {}", url);

        Flux<DataBuffer> dataBufferFlux = webClient
                .get()
                .uri(URI.create(url))
                .accept(MediaType.APPLICATION_PDF)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .doOnError(ex -> log.error("Error in WebClient", ex));

        return DataBufferUtils.join(dataBufferFlux)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                });

    }

    public Mono<Void> uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponseDto fileCreationResponse, String sha256) {
        final String UPLOAD_FILE_CONTENT = "Safe Storage uploadContent";
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, UPLOAD_FILE_CONTENT, fileCreationResponse.getKey());

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Content-type", fileCreationRequest.getContentType());
        headers.add("x-amz-checksum-sha256", sha256);
        headers.add("x-amz-meta-secret", fileCreationResponse.getSecret());

        URI url = URI.create(fileCreationResponse.getUploadUrl());
        HttpMethod method = fileCreationResponse.getUploadMethod() == FileCreationResponseDto.UploadMethodEnum.POST ? HttpMethod.POST : HttpMethod.PUT;

        return webClient.method(method)
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(BodyInserters.fromResource(new ByteArrayResource(fileCreationRequest.getContent())))
                .retrieve()
                .toEntity(String.class)
                .flatMap(stringResponseEntity -> {
                    if (stringResponseEntity.getStatusCodeValue() != org.springframework.http.HttpStatus.OK.value()) {
                        return Mono.error(new RuntimeException());
                    }
                    return Mono.empty();
                });
    }
}
