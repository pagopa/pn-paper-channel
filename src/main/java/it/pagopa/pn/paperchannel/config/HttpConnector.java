package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.paperchannel.exception.PnDownloadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
@Slf4j
public class HttpConnector {

    private final WebClient webClient;

    public HttpConnector(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Mono<PDDocument> downloadFile(String url) {
        return downloadFileInByteArray(url)
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

    public Mono<byte[]> downloadFileInByteArray(String url) {
        log.info("Url to download: {}", url);
        try {
            Flux<DataBuffer> dataBufferFlux = webClient
                    .get()
                    .uri(new URI(url))
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
        } catch (URISyntaxException ex) {
            log.error("error in URI ", ex);
            return Mono.error(ex);
        }
    }
}
