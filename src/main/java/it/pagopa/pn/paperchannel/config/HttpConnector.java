package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.paperchannel.model.FileCreationWithContentRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import reactor.core.publisher.Mono;


public interface HttpConnector {


    Mono<PDDocument> downloadFile(String url);

    Mono<byte[]> downloadFileInByteArray(String url);

    Mono<Void> uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponseDto fileCreationResponse, String sha256);
}
