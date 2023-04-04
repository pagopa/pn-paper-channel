package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.QueueListener;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

class Paper_890IT extends BaseTest {
    @Autowired
    private EventMetaDAO eventMetaDAO;

    @Autowired
    private EventDematDAO eventDematDAO;

    @Autowired
    private QueueListener queueListener;

    @MockBean
    private SqsSender sqsSender;


    private static String generateObject(String statusCode,String productType){
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail": 
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null, 
                        "productType": """+productType+","+"""
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode":"""+statusCode+","+"""
                        "statusDescription": "Mock status",
                        "statusDateTime": "2023-01-12T14:35:35.135725152Z",
                        "deliveryFailureCause": null,
                        "attachments": null,
                        "discoveredAddress": null,
                        "clientRequestTimeStamp": "2023-01-12T14:35:35.13572075Z"
                     }
                }""";
        return json;
    }

    @DirtiesContext
    @Test
    void test(){
     System.out.println(generateObject("003","AR"));
    }
}
