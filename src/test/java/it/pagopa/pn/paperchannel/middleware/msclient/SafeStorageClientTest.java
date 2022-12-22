package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnRetryStorageException;
import it.pagopa.pn.paperchannel.msclient.generated.pnsafestorage.v1.dto.FileDownloadResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class SafeStorageClientTest extends BaseTest.WithMockServer {


    @Autowired
    private SafeStorageClient safeStorageClient;


    @Test
    void testOK(){
        FileDownloadResponseDto responseDto = safeStorageClient.getFile("PDFURL").block();
        Assertions.assertNotNull(responseDto);
        Assertions.assertNotNull(responseDto.getDownload());
        Assertions.assertNotNull(responseDto.getDownload().getUrl());
    }

    @Test
    void testOnErrorResume(){
        FileDownloadResponseDto responseDto = safeStorageClient.getFile("ERROR404")
                .onErrorResume(WebClientResponseException.class, ex -> {
                    Assertions.assertEquals(ex.getStatusCode(), HttpStatus.NOT_FOUND);
                    return Mono.empty();
                }).block();

    }

    @Test
    void testUrl2(){
        Assertions.assertThrows(PnRetryStorageException.class, () ->
                safeStorageClient.getFile("RETRY").block());
    }

    @Test
    void testRetry(){
        StepVerifier.create(safeStorageClient.getFile("RETRY"))
                .expectError(PnRetryStorageException.class).verify();
    }
}
