package it.pagopa.pn.paperchannel.middleware.queue.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.service.PaperAsyncService;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.mockito.InjectMocks;
import org.mockito.Mock;


@Slf4j
class QueueListenerTest extends BaseTest {

    @InjectMocks
    private QueueListener queueListener;
    @Mock
    private PaperAsyncService paperAsyncService;
    @Mock
    private SqsSender sender;
    @Mock
    private PaperResultAsyncService paperResultAsyncService;
    @Mock
    private ObjectMapper objectMapper;





}
