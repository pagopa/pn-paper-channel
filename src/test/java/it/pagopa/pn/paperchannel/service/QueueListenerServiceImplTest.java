package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEvent;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEventItem;
import it.pagopa.pn.api.dto.events.PnF24PdfSetReadyEventPayload;
import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessageDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnnationalregistries.v1.dto.AddressSQSMessagePhysicalAddressDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.middleware.queue.model.EventTypeEnum;
import it.pagopa.pn.paperchannel.model.F24Error;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.service.impl.QueueListenerServiceImpl;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.assertj.core.api.Assertions.*;

class QueueListenerServiceImplTest extends BaseTest {

    @Autowired
    QueueListenerServiceImpl queueListenerService;

    @MockBean
    private PaperResultAsyncService paperResultAsyncService;

    @MockBean
    private PaperAsyncService paperAsyncService;

    @MockBean
    private AddressDAO addressDAO;

    @MockBean
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private SqsSender sqsSender;

    @MockBean
    private F24Service f24Service;

    @MockBean
    private ExternalChannelClient externalChannelClient;


    @Test
    void internalListenerTest(){

        // Given
        int attempt = 10;
        PrepareAsyncRequest prepareAsyncRequest = new PrepareAsyncRequest();
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();

        // When
        Mockito.when(this.paperAsyncService.prepareAsync(Mockito.any(PrepareAsyncRequest.class))).thenReturn(Mono.just(pnDeliveryRequest));

        try {
            this.queueListenerService.internalListener(prepareAsyncRequest, attempt);
        } catch (PnGenericException ex) {
            Assertions.fail("Exception thrown not expected");
        }

        // Then
        Mockito.verify(this.paperAsyncService, Mockito.times(1)).prepareAsync(Mockito.any(PrepareAsyncRequest.class));
    }

    @Test
    void internalListenerErrorTest(){
        Mockito.when(this.paperAsyncService.prepareAsync(Mockito.any())).thenReturn(Mono.error(new PnGenericException(ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION,ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage() )));
        try{
            this.queueListenerService.internalListener(new PrepareAsyncRequest(),10);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(PREPARE_ASYNC_LISTENER_EXCEPTION, ex.getExceptionType());
        }
    }

    @Test
    void nationalRegistriesResponseListenerUntraceableAddressBecauseCorrelationIdIsNotFoundTest(){

        try{
            this.queueListenerService.nationalRegistriesResponseListener(new AddressSQSMessageDto());
            Assertions.fail("Il metodo non è andato in eccezione");
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(CORRELATION_ID_NOT_FOUND, ex.getExceptionType());
        }

        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();
        addressSQSMessageDto.setCorrelationId("");
        try{
            this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(CORRELATION_ID_NOT_FOUND, ex.getExceptionType());
        }
    }

    @Test
    void nationalRegistriesResponseListenerDeliveryNotExistsTest (){
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.empty());
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();
        addressSQSMessageDto.setCorrelationId("1234");
        try{
            this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(DELIVERY_REQUEST_NOT_EXIST, ex.getExceptionType());
        }
    }

    @Test
    void nationalRegistriesResponseListenerReceivedDeliveryRequestErrormessageisnotemptyforcorrelationId(){

        // Given
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();

        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");

        deliveryRequest.setStatusCode(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode());
        deliveryRequest.setStatusDescription(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode() + " - " + StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDescription());
        deliveryRequest.setStatusDetail(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDetail());

        addressSQSMessageDto.setCorrelationId("1234");
        addressSQSMessageDto.setError("ok");
        AddressSQSMessagePhysicalAddressDto addressDto = new AddressSQSMessagePhysicalAddressDto();
        addressSQSMessageDto.setPhysicalAddress(addressDto);

        // When
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.just(deliveryRequest));

        // Then
        try{
            this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);
        } catch(PnGenericException ex){
            Assertions.assertEquals(NATIONAL_REGISTRY_LISTENER_EXCEPTION, ex.getExceptionType());
        }
    }

    @Test
    void nationalRegistriesResponseOk(){

        // Given
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();

        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");

        deliveryRequest.setStatusCode(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode());
        deliveryRequest.setStatusDescription(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode() + " - " + StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDescription());
        deliveryRequest.setStatusDetail(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDetail());

        addressSQSMessageDto.setCorrelationId("1234");
        AddressSQSMessagePhysicalAddressDto addressDto = new AddressSQSMessagePhysicalAddressDto();
        addressSQSMessageDto.setPhysicalAddress(addressDto);

        // When
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(this.addressDAO.findByRequestId(Mockito.anyString())).thenReturn(Mono.just(getRelatedAddress()));
        Mockito.doNothing().when(this.sqsSender).pushToInternalQueue(Mockito.any(PrepareAsyncRequest.class));

        this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);

        // Then
        Mockito.verify(this.paperRequestErrorDAO, Mockito.never()).created(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.sqsSender, Mockito.times(1)).pushToInternalQueue(Mockito.any(PrepareAsyncRequest.class));
    }

    @Test
    void nationalRegistriesResponseListenerReceivedDeliveryRequestKoLogTest(){

        // Given
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();

        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");

        deliveryRequest.setStatusCode(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode());
        deliveryRequest.setStatusDescription(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode() + " - " + StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDescription());
        deliveryRequest.setStatusDetail(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getDetail());

        addressSQSMessageDto.setCorrelationId("1234");
        addressSQSMessageDto.setError("");

        // When
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.just(deliveryRequest));

        this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);

        // Then
        Mockito.verify(this.paperRequestErrorDAO, Mockito.never()).created(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.sqsSender, Mockito.times(1)).pushToInternalQueue(Mockito.any(PrepareAsyncRequest.class));
    }

    @Test
    void nationalRegistriesResponseListenerWithDuplicatedEventOKTest() {

        // Given
        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        AddressSQSMessageDto addressSQSMessageDto = new AddressSQSMessageDto();

        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");

        deliveryRequest.setStatusCode(StatusDeliveryEnum.READY_TO_SEND.getCode());
        deliveryRequest.setStatusDescription(StatusDeliveryEnum.READY_TO_SEND.getCode() + " - " + StatusDeliveryEnum.READY_TO_SEND.getDescription());
        deliveryRequest.setStatusDetail(StatusDeliveryEnum.READY_TO_SEND.getDetail());

        addressSQSMessageDto.setCorrelationId("1234");
        addressSQSMessageDto.setError("");

        // When
        Mockito.when(this.requestDeliveryDAO.getByCorrelationId(Mockito.anyString())).thenReturn(Mono.just(deliveryRequest));

        this.queueListenerService.nationalRegistriesResponseListener(addressSQSMessageDto);

        // Then
        Mockito.verify(this.paperRequestErrorDAO, Mockito.never()).created(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(this.sqsSender, Mockito.never()).pushToInternalQueue(Mockito.any(PrepareAsyncRequest.class));
    }

    @Test
    void f24ResponseListenerOKTest(){
        PnF24PdfSetReadyEvent.Detail response = PnF24PdfSetReadyEvent.Detail.builder()
                .clientId("cxid")
                .pdfSetReady(PnF24PdfSetReadyEventPayload.builder()
                        .requestId("requestid123")
                        .generatedPdfsUrls(List.of(PnF24PdfSetReadyEventItem.builder()
                                        .uri("safestorage://2345678")
                                        .build(),
                                PnF24PdfSetReadyEventItem.builder()
                                        .uri("safestorage://876543")
                                        .build()))
                        .build())
                .build();

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");

        Mockito.when(this.f24Service.arrangeF24AttachmentsAndReschedulePrepare(Mockito.eq("requestid123"), Mockito.anyList())).thenReturn(Mono.just(deliveryRequest));

        Assertions.assertDoesNotThrow(() -> this.queueListenerService.f24ResponseListener(response));

    }

    @Test
    void f24ResponseListenerKOTest(){
        PnF24PdfSetReadyEvent.Detail response = PnF24PdfSetReadyEvent.Detail.builder()
                .clientId("cxid")
                .pdfSetReady(PnF24PdfSetReadyEventPayload.builder()
                        .generatedPdfsUrls(List.of(PnF24PdfSetReadyEventItem.builder()
                                        .uri("safestorage://2345678")
                                        .build(),
                                PnF24PdfSetReadyEventItem.builder()
                                        .uri("safestorage://876543")
                                        .build()))
                        .build())
                .build();

        Assertions.assertThrows(PnGenericException.class, () -> this.queueListenerService.f24ResponseListener(response));

    }

    @Test
    void f24ErrorListenerOKTest(){
        F24Error error = new F24Error();
        error.setIun("1223");
        error.setRequestId("1234dc");
        error.setRelatedRequestId("1234abc");
        error.setCorrelationId("9999");
        error.setAttempt(1);

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId(), true)).thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.just(deliveryRequest));

        Assertions.assertDoesNotThrow(() -> this.queueListenerService.f24ErrorListener(error, 1));

    }

    @Test
    void f24ErrorListenerKOTest(){
        F24Error error = new F24Error();
        error.setIun("1223");
        error.setRequestId("1234dc");
        error.setRelatedRequestId("1234abc");
        error.setCorrelationId("9999");
        error.setAttempt(1);

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId(), true)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId())).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.error(new PnInternalException("missing URL f24set on f24serviceImpl", PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR)));
        //Mockito.doNothing().when(sqsSender).pushInternalError(Mockito.any(F24Error.class), Mockito.anyInt(), Mockito.eq(F24Error.class));

        Assertions.assertThrows(PnF24FlowException.class, () -> this.queueListenerService.f24ErrorListener(error, 1));
        Mockito.verify(requestDeliveryDAO, Mockito.timeout(1000)).updateData(Mockito.any());

    }

    @Test
    void f24ErrorListenerKOTest2(){
        F24Error error = new F24Error();
        error.setIun("1223");
        error.setRequestId("1234dc");
        error.setRelatedRequestId("1234abc");
        error.setCorrelationId("9999");
        error.setAttempt(1);

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setIun("1223");
        deliveryRequest.setRequestId("1234dc");
        deliveryRequest.setRelatedRequestId("1234abc");
        deliveryRequest.setCorrelationId("9999");
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId(), true)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.getByRequestId(error.getRequestId())).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(deliveryRequest));

        Mockito.when(this.f24Service.preparePDF(deliveryRequest)).thenReturn(Mono.error(new PnF24FlowException(ExceptionTypeEnum.F24_ERROR, error, new NullPointerException())));
        //Mockito.doNothing().when(sqsSender).pushInternalError(Mockito.any(F24Error.class), Mockito.any(Integer.class), Mockito.eq(F24Error.class));

        Assertions.assertThrows(PnF24FlowException.class, () -> this.queueListenerService.f24ErrorListener(error, 1));
        Mockito.verify(requestDeliveryDAO, Mockito.timeout(1000)).updateData(Mockito.any());

    }

    @Test
    void externalChannelListenerOkTest(){

        // Given
        int attempt = 10;

        // When
        Mockito.when(this.paperResultAsyncService.resultAsyncBackground(Mockito.any(SingleStatusUpdateDto.class), Mockito.anyInt())).thenReturn(Mono.empty());

        try {
            this.queueListenerService.externalChannelListener(new SingleStatusUpdateDto(),attempt);
        } catch (PnGenericException ex) {
            Assertions.fail("Exception thrown not expected");
        }

        // Then
        Mockito.verify(this.paperResultAsyncService, Mockito.times(1)).resultAsyncBackground(Mockito.any(), Mockito.any());
    }

    @Test
    void externalChannelListenerErrorTest(){
        Mockito.when(this.paperResultAsyncService.resultAsyncBackground(Mockito.any(), Mockito.any())).thenReturn(Mono.error(new PnGenericException(ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION,ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION.getMessage() )));
        try{
            this.queueListenerService.externalChannelListener(new SingleStatusUpdateDto(),10);
        }
        catch(PnGenericException ex){
            Assertions.assertEquals(EXTERNAL_CHANNEL_LISTENER_EXCEPTION, ex.getExceptionType());
        }
    }

    @Test
    void manualRetryExternalChannelOk() {
        String requestid = "PREPARE_ANALOG_DOMICILE.IUN_ZAMG-URDZ-VZRU-202311-D-1.RECINDEX_0.ATTEMPT_1";
        String pcRetry = "1";

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestid);
        deliveryRequest.setProductType("AR");
        deliveryRequest.setAttachments(List.of());

        final PnAddress addOne = new PnAddress();
        addOne.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        final PnAddress addTwo = new PnAddress();
        addTwo.setTypology(AddressTypeEnum.SENDER_ADDRES.name());


        Mockito.when(requestDeliveryDAO.getByRequestId(requestid)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(addressDAO.findAllByRequestId(requestid)).thenReturn(Mono.just(List.of(addOne, addTwo)));
        Mockito.when(externalChannelClient.sendEngageRequest(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any()))
                .thenReturn(Mono.just(deliveryRequest));

        assertThatNoException().isThrownBy(() -> queueListenerService.manualRetryExternalChannel(requestid, pcRetry));
        Mockito.verify(requestDeliveryDAO, Mockito.times(1)).updateData(Mockito.any());
        Mockito.verify(paperRequestErrorDAO, Mockito.never()).created(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void manualRetryExternalChannelKo() {
        String requestid = "PREPARE_ANALOG_DOMICILE.IUN_ZAMG-URDZ-VZRU-202311-D-1.RECINDEX_0.ATTEMPT_1";
        String pcRetry = "1";

        PnDeliveryRequest deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId(requestid);
        deliveryRequest.setProductType("AR");
        deliveryRequest.setAttachments(List.of());

        final PnAddress add = new PnAddress();
        add.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());


        Mockito.when(requestDeliveryDAO.getByRequestId(requestid)).thenReturn(Mono.just(deliveryRequest));
        Mockito.when(paperRequestErrorDAO.created(requestid, "Error message", EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name()))
                .thenReturn(Mono.just(new PnRequestError()));

        Mockito.when(externalChannelClient.sendEngageRequest(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new NullPointerException("Error message")));

        Mockito.when(addressDAO.findAllByRequestId(requestid)).thenReturn(Mono.just(List.of(add)));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queueListenerService.manualRetryExternalChannel(requestid, pcRetry));

        Mockito.verify(paperRequestErrorDAO, Mockito.times(1))
                .created(requestid, "Error message", EventTypeEnum.EXTERNAL_CHANNEL_ERROR.name());
    }

    private PnAddress getRelatedAddress(){
        PnAddress address = new PnAddress();
        address.setFullName("Mario Rossi");
        return address;
    }
}