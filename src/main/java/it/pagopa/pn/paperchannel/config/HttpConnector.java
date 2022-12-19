package it.pagopa.pn.paperchannel.config;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class HttpConnector {

    private HttpConnector(){
        throw new IllegalCallerException("the constructor must not called");
    }

    private static Mono<PDDocument> downloadFile(String url) {
        return WebClient.create(url)
                .get()
                .accept(MediaType.APPLICATION_PDF)
                .retrieve()
                .bodyToMono(byte[].class)
                .flatMap(bytes -> {
                    try {
                        return Mono.just(PDDocument.load(bytes));
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                });
    }
}
