package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.safestorage.model.FileCreationResponse;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import reactor.core.publisher.Mono;

/**
 * HTTP reactive client used to invoke external services that do not have OpenAPI documentation, such as the S3 service
 */
public interface HttpConnector {


    Mono<PDDocument> downloadFile(String url);

    Mono<byte[]> downloadFileAsByteArray(String url);

    Mono<Void> uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256);
}
