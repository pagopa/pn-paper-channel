package it.pagopa.pn.paperchannel.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
@Slf4j
public class HttpConnector {

    public Mono<PDDocument> downloadFile(String url) {
        log.info("Url to download: {}", url);
        try {
            return WebClient
                    .builder()
                    .codecs(codecs ->
                            codecs.defaultCodecs()
                                    .maxInMemorySize(-1)
                    )
                    .build()
                    .get()
                    .uri(new URI(url))
                    .accept(MediaType.APPLICATION_PDF)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .flatMap(bytes -> {
                        try {
                            return Mono.just(PDDocument.load(bytes));
                        } catch (IOException e) {
                            log.error("Error load PDF for url {}: {}", url, e.getMessage());
                            return Mono.error(e);
                        }
                    });
        } catch (URISyntaxException e) {
            log.error("Error syntax URI for url {}: {}", url, e.getMessage());
            return Mono.error(e);
        }
    }

    public Mono<byte[]> downloadFileInByteArray(String url) {
        log.info("Url to download: {}", url);
        try {
            return WebClient
                    .builder()
                    .codecs(codecs ->
                            codecs.defaultCodecs()
                                    .maxInMemorySize(-1)
                    )
                    .build()
                    .get()
                    .uri(new URI(url))
                    .accept(MediaType.APPLICATION_PDF)
                    .retrieve()
                    .bodyToMono(byte[].class);
        } catch (URISyntaxException e) {
            log.error("Error syntax URI for url {}: {}", url, e.getMessage());
            return Mono.error(e);
        }
    }
}
