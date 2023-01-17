package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.AttachmentDetails;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.impl.PaperResultAsyncServiceImpl;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.field.OffsetDateTimeField;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DATA_NULL_OR_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
class PaperResultAsyncServiceTest extends BaseTest {

    @InjectMocks
    PaperResultAsyncServiceImpl paperResultAsyncService;
    @Mock
    RequestDeliveryDAO requestDeliveryDAO;
    @Mock
    SqsSender sqsSender;

    @Test
    void resultAsyncBackgroundOK() {
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setAnalogMail(new PaperProgressStatusEventDto());
        singleStatusUpdateDto.getAnalogMail().setRequestId("requestId");
        singleStatusUpdateDto.getAnalogMail().setStatusCode("statusCode");
        singleStatusUpdateDto.getAnalogMail().setStatusDescription("statusDescription");
        singleStatusUpdateDto.getAnalogMail().setStatusDateTime(Instant.now().atOffset(ZoneOffset.UTC));
        singleStatusUpdateDto.getAnalogMail().setRegisteredLetterCode("registeredLetterCode");
        singleStatusUpdateDto.getAnalogMail().setClientRequestTimeStamp(Instant.now().atOffset(ZoneOffset.UTC));
        singleStatusUpdateDto.getAnalogMail().setDeliveryFailureCause("deliveryFailureCause");
        singleStatusUpdateDto.getAnalogMail().setDiscoveredAddress(new DiscoveredAddressDto());
        singleStatusUpdateDto.getAnalogMail().setAttachments(new ArrayList<>());
        singleStatusUpdateDto.getAnalogMail().getAttachments().add(new AttachmentDetailsDto());
        singleStatusUpdateDto.getAnalogMail().getAttachments().get(0).setId("Id");
        singleStatusUpdateDto.getAnalogMail().getAttachments().get(0).documentType("documentType");
        singleStatusUpdateDto.getAnalogMail().getAttachments().get(0).setDate(Instant.now().atOffset(ZoneOffset.UTC));
        singleStatusUpdateDto.getAnalogMail().getAttachments().get(0).setUrl("Url");

        Mono<PnDeliveryRequest> pnDeliveryRequestMono = Mono.just(new PnDeliveryRequest());
        Mockito.when(requestDeliveryDAO.getByRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId())).thenReturn(pnDeliveryRequestMono);
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(pnDeliveryRequestMono);

        SendEvent sendEvent = new SendEvent();
        Mockito.doNothing().when(sqsSender).pushSendEvent(sendEvent);
        paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto)
                .map(entity -> {
                    assertEquals(entity.getStatusDetail(), singleStatusUpdateDto.getAnalogMail().getStatusDescription());
                    assertEquals(entity.getStatusCode(), singleStatusUpdateDto.getAnalogMail().getStatusCode());
                    assertEquals(entity.getStatusDate(), DateUtils.formatDate(DateUtils.getDatefromOffsetDateTime(singleStatusUpdateDto.getAnalogMail().getStatusDateTime())));
                    return Mono.empty();
                }).block();

        singleStatusUpdateDto.getAnalogMail().setAttachments(new ArrayList<>());
        paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto)
                .map(entity -> {
                    assertEquals(entity.getStatusDetail(), singleStatusUpdateDto.getAnalogMail().getStatusDescription());
                    assertEquals(entity.getStatusCode(), singleStatusUpdateDto.getAnalogMail().getStatusCode());
                    assertEquals(entity.getStatusDate(), DateUtils.formatDate(DateUtils.getDatefromOffsetDateTime(singleStatusUpdateDto.getAnalogMail().getStatusDateTime())));
                    return Mono.empty();
                }).block();

        singleStatusUpdateDto.getAnalogMail().setAttachments(null);
        paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto)
                .map(entity -> {
                    assertEquals(entity.getStatusDetail(), singleStatusUpdateDto.getAnalogMail().getStatusDescription());
                    assertEquals(entity.getStatusCode(), singleStatusUpdateDto.getAnalogMail().getStatusCode());
                    assertEquals(entity.getStatusDate(), DateUtils.formatDate(DateUtils.getDatefromOffsetDateTime(singleStatusUpdateDto.getAnalogMail().getStatusDateTime())));
                    return Mono.empty();
                }).block();
    }

    @Test
    void resultAsyncBackgroundThrowError() {
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setAnalogMail(new PaperProgressStatusEventDto());
        singleStatusUpdateDto.getAnalogMail().setRequestId("requestId");
        Mockito.when(requestDeliveryDAO.getByRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId())).thenReturn(Mono.error(new PnGenericException(ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST, ExceptionTypeEnum.DELIVERY_REQUEST_NOT_EXIST.getMessage())));
        StepVerifier.create(paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto))
                .expectError(PnGenericException.class).verify();
    }

    @Test
    void resultAsyncBackgroundThrowDataException() {
        //CASE 1
        SingleStatusUpdateDto singleStatusUpdateDto = null;
        paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto)
                .onErrorResume(PnGenericException.class, exception ->{
                    assertEquals(DATA_NULL_OR_INVALID.getMessage(), exception.getMessage() );
                    return Mono.empty();}
                ).block();

        //CASE 2
        singleStatusUpdateDto = new SingleStatusUpdateDto();
        paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto)
                .onErrorResume(PnGenericException.class, exception ->{
                    assertEquals(DATA_NULL_OR_INVALID.getMessage(), exception.getMessage() );
                    return Mono.empty();}
                ).block();

        //CASE 3
        singleStatusUpdateDto.setAnalogMail(new PaperProgressStatusEventDto());
        paperResultAsyncService.resultAsyncBackground(singleStatusUpdateDto)
                .onErrorResume(PnGenericException.class, exception ->{
                    assertEquals(DATA_NULL_OR_INVALID.getMessage(), exception.getMessage() );
                    return Mono.empty();}
                ).block();
    }
}
