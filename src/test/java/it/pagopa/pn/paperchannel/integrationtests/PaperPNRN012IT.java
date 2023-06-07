package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.HandlersFactory;
import it.pagopa.pn.paperchannel.middleware.queue.consumer.handler.RECRN011MessageHandler;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.service.impl.PaperResultAsyncServiceImpl;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.TAKING_CHARGE;
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.RECRN011_STATUS_CODE;
import static org.mockito.Mockito.mock;


public class PaperPNRN012IT extends BaseTest {
    private static final String REQUEST_ID = "abc-234-SDSS";
    private static final String PRODUCT_TYPE = "AR";
    @Autowired
    private PaperResultAsyncServiceImpl paperResultAsyncService;

    @MockBean
    private SqsSender sqsSender;
    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;
    @MockBean
    private HandlersFactory handlersFactory;


    @Test
    void Test_AR_StartProcessing__RECRN011(){

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(createSimpleAnalogMail(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), PRODUCT_TYPE));

        Mockito.when(requestDeliveryDAO.getByRequestId(REQUEST_ID))
                .thenReturn(Mono.just(getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription())));

        PnDeliveryRequest updatedIntoResultAsync = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());
        RequestDeliveryMapper.changeState(
                updatedIntoResultAsync,
                extChannelMessage.getAnalogMail().getStatusCode(),
                extChannelMessage.getAnalogMail().getStatusDescription(),
                ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()),
                PRODUCT_TYPE,
                extChannelMessage.getAnalogMail().getStatusDateTime().toInstant()
                );

        RECRN011MessageHandler handler = mock(RECRN011MessageHandler.class);
        Mockito.when(handlersFactory.getHandler(Mockito.any()))
                .thenReturn(handler);

        Mockito.when(handler.handleMessage(Mockito.any(), Mockito.any()))
                        .thenReturn(Mono.empty());

        Mockito.when(requestDeliveryDAO.updateData(updatedIntoResultAsync)).thenReturn(Mono.just(updatedIntoResultAsync));

        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

    }



    private PnDeliveryRequest getDeliveryRequest(String code, String detail, String description){
        var request = new PnDeliveryRequest();
        request.setRequestId(REQUEST_ID);
        request.setStatusCode(code);
        request.setStatusDetail(detail);
        request.setStatusDescription(description);
        return request;
    }

    private PaperProgressStatusEventDto createSimpleAnalogMail(String statusCode, String statusDetail, String productType) {
        var analogMail = new PaperProgressStatusEventDto();
        analogMail.requestId(REQUEST_ID);
        analogMail.setClientRequestTimeStamp(OffsetDateTime.now());
        analogMail.setStatusDateTime(OffsetDateTime.now());
        analogMail.setStatusCode(statusCode);
        analogMail.setProductType(productType);
        analogMail.setStatusDescription(statusDetail);

        return analogMail;
    }

}
