package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.*;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DOCUMENT_NOT_DOWNLOADED;

@Component
@Slf4j
@RequiredArgsConstructor
public class HttpConnector {

    public static final long ONE_MAGABYTE = 1024L * 1024L;

    private final PnPaperChannelConfig config;

    private Long maxMegaByteInMemory;

    @PostConstruct
    public void init() {
        maxMegaByteInMemory = ONE_MAGABYTE * config.getMaxMegabyteInMemory();
        log.info("Max megabyte in memory: {}", maxMegaByteInMemory);
    }


    public Mono<PDDocument> downloadFile(String url, BigDecimal contentLength) {
        boolean tmpFile = contentLength != null && contentLength.longValue() > maxMegaByteInMemory;
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
                .map(randomAccess -> buildPDDocument(randomAccess, tmpFile));
    }

    private URI createURI(String url) {
        try {
            return new URI(url);
        }
        catch (URISyntaxException e) {
            throw new PnInternalException(e.getMessage(), DOCUMENT_NOT_DOWNLOADED.getTitle(), e);
        }
    }


    private PDDocument buildPDDocument(RandomAccessRead randomAccessRead, boolean tmpFile) {
        PDDocument pdDocument = null;
        try {
            ScratchFile scratchFile = buildScratchFile(tmpFile);
            PDFParser parser = new PDFParser(randomAccessRead, scratchFile);
            parser.parse();
            pdDocument = parser.getPDDocument();
            return pdDocument;
        }
        catch (IOException e) {
            throw new PnInternalException("Error creating PDDocument file with ScratchFile", DOCUMENT_NOT_DOWNLOADED.getTitle(), e);
        }
        finally {
            IOUtils.closeQuietly(pdDocument);
        }
    }

    private ScratchFile buildScratchFile(boolean tmpFile) {
        if(tmpFile) {
            log.debug("File size exceeds the limit in memory allowed, I use a temporary file");
            try {
                return new ScratchFile(MemoryUsageSetting.setupMixed(maxMegaByteInMemory));
            }
            catch (IOException e) {
                throw new PnInternalException("Error creating temporary file with ScratchFile", DOCUMENT_NOT_DOWNLOADED.getTitle(), e);
            }
        }
        else {
            return ScratchFile.getMainMemoryOnlyInstance();
        }
    }
}
