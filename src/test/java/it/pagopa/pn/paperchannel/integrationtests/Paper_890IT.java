package it.pagopa.pn.paperchannel.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.QueueListener;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.QueueListenerService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import it.pagopa.pn.paperchannel.utils.Utility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.MAPPER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class Paper_890IT extends BaseTest {

    @Autowired
    private PaperResultAsyncService paperResultAsyncService;

    @MockBean
    private SqsSender sqsSender;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;



    @Test
    void test_890_DossierClose_RECAG001C(){

    }


    @Test
    void test_890_DeliverDossierClose_RECAG002C(){

    }

    @Test
    void test_890_NotDeliverDossierClose_RECAG003A_RECAG003C(){

    }


    @Test
    void test_890_untraceableDossierClose_RECAG003D_RECAG003F(){

    }


    @Test
    void test_890_deliverStockDossierClose_RECAG005C(){

    }


    @Test
    void test_890_deliverStockDossierClose_RECAG005C(){

    }

    /*
    *****************************************************************************************************
     */

    private static String generateObject(String statusCode,String productType){
        String json = """
                {
                     "digitalCourtesy": null,
                     "digitalLegal": null,
                     "analogMail": 
                     {
                        "requestId": "AKUZ-AWPL-LTPX-20230415",
                        "registeredLetterCode": null, 
                        "productType": "AR",
                        "iun": "AKUZ-AWPL-LTPX-20230415",
                        "statusCode":"""+"\""+statusCode+"\""+",\n"+"""
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


    @Test
    void test(){

        PnDeliveryRequest pnDeliveryRequest = createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = new PaperProgressStatusEventDto();
        analogMail.requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        analogMail.setClientRequestTimeStamp(OffsetDateTime.now());
        analogMail.setStatusDateTime(OffsetDateTime.now());
        analogMail.setStatusCode("RECRS002B");
        analogMail.setProductType("RS");
        analogMail.setStatusDescription("In progress");
        analogMail.setAttachments(List.of(
                new AttachmentDetailsDto()
                        .documentType("CAD")
                        .date(OffsetDateTime.now())
                        .url("https://safestorage.it"),
                new AttachmentDetailsDto()
                        .documentType("23L")
                        .date(OffsetDateTime.now())
                        .url("https://safestorage.it"))
        );

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);

        PnDeliveryRequest afterSetForUpdate = createPnDeliveryRequest();
        afterSetForUpdate.setStatusCode(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        afterSetForUpdate.setStatusDetail(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(Date.from(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant())));

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));

        // verifico che il flusso è stato completato con successo
        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());


        ArgumentCaptor<SendEvent> caturedSendEvent = ArgumentCaptor.forClass(SendEvent.class);

        verify(sqsSender, timeout(2000).times(1)).pushSendEvent(caturedSendEvent.capture());

        System.out.println(caturedSendEvent.getAllValues());

    }


    private PnDeliveryRequest createPnDeliveryRequest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_KREP-VHAD-TAQV-202302-P-1.RECINDEX_0.SENTATTEMPTMADE_1");
        pnDeliveryRequest.setCorrelationId("Self=1-63fe1166-09f74e174d4e13d26f7d08c0;Root=1-63fe1166-cdf14290b52666124be856be;Parent=a3bb560233ceb4ec;Sampled=1");
        pnDeliveryRequest.setFiscalCode("PF-a6c1350d-1d69-4209-8bf8-31de58c79d6e");
        pnDeliveryRequest.setHashedFiscalCode("81af12154dfaf8094715acadc8065fdde56c31fb52a9d1766f8f83470262c13a");
        pnDeliveryRequest.setHashOldAddress("60cba8d6dda57ac74ec15e5a4b78402672883ecdffdb01d1f19501cba176f7254b803f38a0359c42d8fe8459d0a6ecac8ca9e7539a64df346290c966dc9845444dee871c93f2d2d33a691daa7a5c75b10f504efc91a03dcb3882744f9");
        pnDeliveryRequest.setIun("KREP-VHAD-TAQV-202302-P-1");
        pnDeliveryRequest.setPrintType("BN_FRONTE_RETRO");
        pnDeliveryRequest.setProposalProductType("890");
        pnDeliveryRequest.setReceiverType("PF");
        pnDeliveryRequest.setRelatedRequestId("PREPARE_ANALOG_DOMICILE.IUN_KREP-VHAD-TAQV-202302-P-1.RECINDEX_0.SENTATTEMPTMADE_0");
        pnDeliveryRequest.setStartDate("2023-02-28T15:36:22.225");
        pnDeliveryRequest.setStatusCode("PROGRESS");
        pnDeliveryRequest.setStatusDate("2023-02-28T15:36:22.29");
        pnDeliveryRequest.setStatusDetail("In attesa di indirizzo da National Registry");

        return pnDeliveryRequest;
    }

    private <T> T convertToObject(String body, Class<T> tClass){
        T entity = Utility.jsonToObject(this.objectMapper, body, tClass);
        if (entity == null) throw new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        return entity;
    }
}
