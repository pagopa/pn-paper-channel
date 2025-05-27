package it.pagopa.pn.paperchannel.config;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.commons.utils.metrics.SpringAnalyzer;
import it.pagopa.pn.paperchannel.LocalStackTestConfig;
import it.pagopa.pn.paperchannel.middleware.queue.producer.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(LocalStackTestConfig.class)
public abstract class BaseTest {

    @MockitoBean
    private SpringAnalyzer springAnalyzer;

    /**
     * Subclasses need to be annotated with:
     * SpringBootTest
     * EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
     * ActiveProfiles(“test”)
     */
    public static class WithOutLocalStackTest {

        @MockitoBean
        private DeliveryPushMomProducer deliveryMomProducer;

        @MockitoBean
        private InternalQueueMomProducer internalQueueMomProducer;

        @MockitoBean
        private NormalizeAddressQueueMomProducer normalizeAddressQueueMomProducer;

        @MockitoBean
        private PaperchannelToDelayerMomProducer paperchannelToDelayerMomProducer;

        @MockitoBean
        private DelayerToPaperchannelInternalProducer delayerToPaperchannelInternalProducer;

        @MockitoBean
        private SpringAnalyzer springAnalyzer;


    }


    @Slf4j
    @SpringBootTest
    @EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class })
    @ActiveProfiles("test")
    public static class WithMockServer extends WithOutLocalStackTest {
        @Autowired
        private MockServerBean mockServer;


        @BeforeEach
        public void init(){
            log.info(this.getClass().getSimpleName());
            //TODO set name file with name class + ".json";
            setExpection(this.getClass().getSimpleName() + "-webhook.json");
        }

        @AfterEach
        public void kill(){
            log.info("Killed");
            this.mockServer.stop();
        }

        public void setExpection(String file){
            this.mockServer.initializationExpection(file);
        }
    }


}
