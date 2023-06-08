package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.mapper.RequestDeliveryMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDiscoveredAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventMeta;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
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
import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


class PaperPNRN012IT extends BaseTest {
    private static final String REQUEST_ID = "abc-234-SDSS";
    private static final String PRODUCT_TYPE = "AR";
    @Autowired
    private PaperResultAsyncServiceImpl paperResultAsyncService;

    @MockBean
    private SqsSender sqsSender;
    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;
    @MockBean
    private EventMetaDAO eventMetaDAO;
    @MockBean
    private EventDematDAO eventDematDAO;


    @Test
    void Test_AR_StartProcessing__RECRN011(){

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(createSimpleAnalogMail(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), PRODUCT_TYPE));

        PnDeliveryRequest requestHandler = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());

        Mockito.when(requestDeliveryDAO.getByRequestId(REQUEST_ID))
                .thenReturn(Mono.just(requestHandler));

        PnDeliveryRequest updatedIntoResultAsync = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());
        RequestDeliveryMapper.changeState(
                updatedIntoResultAsync,
                extChannelMessage.getAnalogMail().getStatusCode(),
                extChannelMessage.getAnalogMail().getStatusDescription(),
                ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()),
                updatedIntoResultAsync.getProductType(),
                extChannelMessage.getAnalogMail().getStatusDateTime().toInstant()
                );

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any()))
                .thenReturn(Mono.just(updatedIntoResultAsync));

        Mockito.doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        Mockito.when(eventMetaDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(new PnEventMeta()));

        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

    }


    @Test
    void Test_AR_SaveDemat__RECRN004B(){
        String RECRN004B = "RECRN004B";

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(createSimpleAnalogMail(RECRN004B, StatusCodeEnum.PROGRESS.getValue(), PRODUCT_TYPE));

        PnDeliveryRequest requestHandler = getDeliveryRequest(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), RECRN011_STATUS_CODE.concat(StatusCodeEnum.PROGRESS.getValue()));

        Mockito.when(requestDeliveryDAO.getByRequestId(REQUEST_ID))
                .thenReturn(Mono.just(requestHandler));

        PnDeliveryRequest updatedIntoResultAsync = getDeliveryRequest(RECRN011_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), RECRN011_STATUS_CODE.concat(StatusCodeEnum.PROGRESS.getValue()));
        RequestDeliveryMapper.changeState(
                updatedIntoResultAsync,
                extChannelMessage.getAnalogMail().getStatusCode(),
                extChannelMessage.getAnalogMail().getStatusDescription(),
                ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()),
                updatedIntoResultAsync.getProductType(),
                extChannelMessage.getAnalogMail().getStatusDateTime().toInstant()
        );

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any()))
                .thenReturn(Mono.just(updatedIntoResultAsync));


        Mockito.when(eventDematDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(new PnEventDemat()));


        Mockito.doNothing().when(sqsSender).pushSendEvent(Mockito.any());

        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

    }

    @Test
    void Test_AR_Save_MetaData__RECRN003A() {
        String RECRN003A_STATUS_CODE = "RECRN003A";

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(createSimpleAnalogMail(RECRN003A_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), PRODUCT_TYPE));

        PnDeliveryRequest requestHandler = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());

        Mockito.when(requestDeliveryDAO.getByRequestId(REQUEST_ID))
                .thenReturn(Mono.just(requestHandler));

        PnDeliveryRequest updatedIntoResultAsync = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());
        RequestDeliveryMapper.changeState(
                updatedIntoResultAsync,
                extChannelMessage.getAnalogMail().getStatusCode(),
                extChannelMessage.getAnalogMail().getStatusDescription(),
                ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()),
                updatedIntoResultAsync.getProductType(),
                extChannelMessage.getAnalogMail().getStatusDateTime().toInstant()
        );

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any()))
                .thenReturn(Mono.just(updatedIntoResultAsync));

        PnEventMeta pnEventMeta = buildPnEventMeta(extChannelMessage.getAnalogMail());

        Mockito.when(eventMetaDAO.createOrUpdate(pnEventMeta))
                .thenReturn(Mono.just(pnEventMeta));

        Assertions.assertDoesNotThrow(() -> {
            this.paperResultAsyncService.resultAsyncBackground(extChannelMessage, 15).block();
        });

        verify(eventMetaDAO, times(1)).createOrUpdate(pnEventMeta);
    }

    @Test
    void Test_AR_Save_MetaData__RECRN004C() {
        String RECRN004C_STATUS_CODE = "RECRN004C";

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(createSimpleAnalogMail(RECRN004C_STATUS_CODE, StatusCodeEnum.PROGRESS.getValue(), PRODUCT_TYPE));

        PnDeliveryRequest requestHandler = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());

        Mockito.when(requestDeliveryDAO.getByRequestId(REQUEST_ID))
                .thenReturn(Mono.just(requestHandler));

        PnDeliveryRequest updatedIntoResultAsync = getDeliveryRequest(TAKING_CHARGE.getCode(), TAKING_CHARGE.getDetail(), TAKING_CHARGE.getDescription());
        RequestDeliveryMapper.changeState(
                updatedIntoResultAsync,
                extChannelMessage.getAnalogMail().getStatusCode(),
                extChannelMessage.getAnalogMail().getStatusDescription(),
                ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()),
                updatedIntoResultAsync.getProductType(),
                extChannelMessage.getAnalogMail().getStatusDateTime().toInstant()
        );

        Mockito.when(requestDeliveryDAO.updateData(Mockito.any()))
                .thenReturn(Mono.just(updatedIntoResultAsync));

        final String metaRequestId = buildMetaRequestId(requestHandler.getRequestId());

        Mockito.when(eventMetaDAO.getDeliveryEventMeta(metaRequestId, buildMetaStatusCode(RECRN011_STATUS_CODE)))
                .thenReturn(Mono.just(new PnEventMeta()));

    }

    private PnEventMeta buildPnEventMeta(PaperProgressStatusEventDto paperRequest) {
        long ttlDays = 365;

        PnEventMeta pnEventMeta = new PnEventMeta();
        pnEventMeta.setMetaRequestId(buildMetaRequestId(paperRequest.getRequestId()));
        pnEventMeta.setMetaStatusCode(buildMetaStatusCode(paperRequest.getStatusCode()));
        pnEventMeta.setTtl(paperRequest.getStatusDateTime().plusDays(ttlDays).toEpochSecond());

        pnEventMeta.setRequestId(paperRequest.getRequestId());
        pnEventMeta.setStatusCode(paperRequest.getStatusCode());
        pnEventMeta.setDeliveryFailureCause(paperRequest.getDeliveryFailureCause());

        if (paperRequest.getDiscoveredAddress() != null) {
            PnDiscoveredAddress discoveredAddress = new BaseMapperImpl<>(DiscoveredAddressDto.class, PnDiscoveredAddress.class).toDTO(paperRequest.getDiscoveredAddress());
            pnEventMeta.setDiscoveredAddress(discoveredAddress);
        }

        pnEventMeta.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());
        return pnEventMeta;
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
