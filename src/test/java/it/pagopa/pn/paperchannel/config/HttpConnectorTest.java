package it.pagopa.pn.paperchannel.config;

import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.paperchannel.middleware.queue.producer.DeliveryPushMomProducer;
import it.pagopa.pn.paperchannel.middleware.queue.producer.InternalQueueMomProducer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
@ActiveProfiles("test")
class HttpConnectorTest {

    @MockBean
    private DeliveryPushMomProducer deliveryMomProducer;

    @MockBean
    private InternalQueueMomProducer internalQueueMomProducer;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {

        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }


    @Test
    void getFileWithInMemoryProcess() throws IOException {
        long sizeFile = 100 * 1024; //mock file size for in memory usage
        testGetFile(sizeFile);
    }

    @Test
    void getFileWithInTmpFileProcess() throws IOException {
        long sizeFile = 2000 * 1024; //mock file size for tmp file usage
        testGetFile(sizeFile);
    }

    void testGetFile(long sizeFile) throws IOException {
        //Given
        String url = "/safe-storage/v1/files/a-file";
        File pdfFile = new ClassPathResource("mock-pdf/pdf-500KB.pdf").getFile();
        Path path = pdfFile.toPath();
        byte[] bytes = Files.readAllBytes(path);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(url)
                )
                .respond(response()
                        .withBody(bytes)
                        .withContentType(MediaType.PDF)
                        .withStatusCode(200)
                );

        PDDocument actual = HttpConnector.downloadFile("http://localhost:9998" + url, BigDecimal.valueOf(sizeFile)).block();

        assertThat(actual).isNotNull();
        assertThat(actual.getNumberOfPages()).isNotZero();

        PDDocument expected = PDDocument.load(bytes);

        assertThat(actual.getNumberOfPages()).isEqualTo(expected.getNumberOfPages());
        assertThat(actual.getDocumentId()).isEqualTo(expected.getDocumentId());
        assertThat(actual.getVersion()).isEqualTo(expected.getDocument().getVersion());
    }


}


