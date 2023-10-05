package it.pagopa.pn.paperchannel.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.*;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class HttpConnector {

    public static final long MAX_MAIN_MEMORY_BYTES = 1024L * 1024L;

    private HttpConnector(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static Mono<PDDocument> downloadFile(String url, BigDecimal contentLength) {
        boolean tmpFile = contentLength != null && contentLength.longValue() > MAX_MAIN_MEMORY_BYTES;
        // DataBuffer and DataBufferUtils to stream our download in chunks so that the whole file doesn't get loaded
        // into memory
        URI uri = createURI(url);
        Flux<DataBuffer> dataBufferFlux = WebClient.create()
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_PDF)
                .retrieve()
                .bodyToFlux(DataBuffer.class);

        return DataBufferUtils.join(dataBufferFlux)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new RandomAccessBuffer(bytes);
                })
                .map(randomAccess -> HttpConnector.buildPDDocument(randomAccess, tmpFile));
    }

    private static URI createURI(String url) {
        try {
            return new URI(url);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    private static PDDocument buildPDDocument(RandomAccessRead randomAccessRead, boolean tmpFile) {
        PDDocument pdDocument = null;
        try {
            ScratchFile scratchFile = buildScratchFile(tmpFile);
            PDFParser parser = new PDFParser(randomAccessRead, scratchFile);
            parser.parse();
            pdDocument = parser.getPDDocument();
            return pdDocument;
        }
        catch (IOException e) {
            throw new RuntimeException("Error creating PDDocument file with ScratchFile", e);
        }
        finally {
            IOUtils.closeQuietly(pdDocument);
        }
    }

    private static ScratchFile buildScratchFile(boolean tmpFile) {
        if(tmpFile) {
            log.debug("File size exceeds the limit in memory allowed, I use a temporary file");
            try {
                return new ScratchFile(MemoryUsageSetting.setupMixed(MAX_MAIN_MEMORY_BYTES));
            }
            catch (IOException e) {
                throw new RuntimeException("Error creating temporary file with ScratchFile", e);
            }
        }
        else {
            return ScratchFile.getMainMemoryOnlyInstance();
        }
    }
}
