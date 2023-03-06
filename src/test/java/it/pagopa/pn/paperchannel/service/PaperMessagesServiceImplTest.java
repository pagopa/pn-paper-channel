package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.encryption.impl.DataVaultEncryptionImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.impl.CostDAOImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.impl.RequestDeliveryDAOImpl;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.middleware.msclient.impl.NationalRegistryClientImpl;
import it.pagopa.pn.paperchannel.service.impl.PaperMessagesServiceImpl;
import it.pagopa.pn.paperchannel.service.impl.SqsQueueSender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Slf4j
public class PaperMessagesServiceImplTest extends BaseTest {
    @Autowired
    private AddressDAO addressDAO;
    @Autowired
    private ExternalChannelClient externalChannelClient;
    @Autowired
    private PnPaperChannelConfig pnPaperChannelConfig;

    @Autowired
    private PaperTenderService paperTenderService;


    private PaperMessagesServiceImpl paperMessagesService;

    //@Test
    void createPaperMessagesServiceImpl(){
        Mockito.when(new PaperMessagesServiceImpl(null,null,
                null,null,null)).thenReturn(null);
         new PaperMessagesServiceImpl(null,null,null,null,null);
    }



}
