package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.ExternalChannelClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.DiscoveredAddressDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class PaperResultAsyncServiceTest extends BaseTest {
    @Mock
    private ExternalChannelClient externalChannelClient;

    private PnPaperChannelConfig pnPaperChannelConfig;
    private AddressDAO addressDAO;
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @Autowired
    PaperResultAsyncService paperResultAsyncService;
    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    protected PnLogAudit pnLogAudit;


    //SingleStatusUpdateDto singleStatusUpdateDto, Integer attempt

    @Test
    void resultAsyncBackgroundReturnNullError(){
        StepVerifier.create(paperResultAsyncService.resultAsyncBackground(null,0))
                .expectError(PnGenericException.class).verify();
    }

    @Test
    void resultAsyncBackgroundReturnNullAnalogMailError(){
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        singleStatusUpdateDto.setAnalogMail(null);
        StepVerifier.create(paperResultAsyncService.resultAsyncBackground(null,0))
                .expectError(PnGenericException.class).verify();
    }

    @Test
    void resultAsyncBackgroundReturnNotNullAnalogMailIsBlankRequestIdError(){
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setRequestId(null);
        singleStatusUpdateDto.setAnalogMail(paperProgressStatusEventDto);
        StepVerifier.create(paperResultAsyncService.resultAsyncBackground(null,0))
                .expectError(PnGenericException.class).verify();
    }

    @Test
    void resultAsyncBackgroundReturnOkInFirstIf(){
        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setRequestId(Const.RETRY.concat("001"));
        DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();
        discoveredAddressDto.setAddress("address");
        discoveredAddressDto.setCap("99999");
        discoveredAddressDto.setCity("Milano");
        discoveredAddressDto.setPr("AR");
        paperProgressStatusEventDto.setDiscoveredAddress(discoveredAddressDto);
        singleStatusUpdateDto.setAnalogMail(paperProgressStatusEventDto);

        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest));

        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest));
        //Mockito.when(pnLogAudit.addsBeforeReceive(Mockito.any(), Mockito.anyString(), Mockito.anyString()).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsSuccessReceive(Mockito.any(), Mockito.any(),  Mockito.any(), Mockito.any(), Mockito.any()).thenReturn(Mono.empty());

        PnDeliveryRequest pnDeliveryRequest1 = new PnDeliveryRequest();
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest1));

        //mock per riga 78
        //Mockito.when(updateEntityResult(Mockito.any(), (Mockito.any()).publishOn(Schedulers.boundedElastic()).thenReturn(Mono.just(pnDeliveryRequest1));

        //publish in sendEngageRequest
        Mockito.when( Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())).thenReturn(Mono.empty());

        //Mockito.when(pnLogAudit.addsBeforeSend(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());

        //mock sendEngageRequest
        Mockito.when(externalChannelClient.sendEngageRequest(Mockito.any(), Mockito.any() )).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsSuccessSend(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsFailSend(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any()  )).thenReturn(Mono.empty());

        Mockito.when(paperRequestErrorDAO.created(Mockito.any(), Mockito.any(),  Mockito.any()) ).thenReturn(Mono.empty());

        //dopo la sendEngageRequest
        Mono.delay(Mockito.any()).publishOn(Mockito.any()).thenReturn(Mono.empty());

        //Mockito.when(pnLogAudit.addsBeforeDiscard(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsSuccessDiscard(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(Mono.empty());

        PnDeliveryRequest response = paperResultAsyncService.resultAsyncBackground( singleStatusUpdateDto,5).block();
        assertNotNull(response);
    }

    @Test
    void resultAsyncBackgroundReturnIsTecnicalErrorStatusCode() {

        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setRequestId(Const.RETRY.concat("002"));
        DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();
        discoveredAddressDto.setAddress("address");
        discoveredAddressDto.setCap("1111");
        discoveredAddressDto.setCity("Roma");
        discoveredAddressDto.setPr("899");
        paperProgressStatusEventDto.setDiscoveredAddress(discoveredAddressDto);
        singleStatusUpdateDto.setAnalogMail(paperProgressStatusEventDto);

        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest));

        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest));
        //Mockito.when(pnLogAudit.addsBeforeReceive(Mockito.any(), Mockito.anyString(), Mockito.anyString()).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsSuccessReceive(Mockito.any(), Mockito.any(),  Mockito.any(), Mockito.any(), Mockito.any()).thenReturn(Mono.empty());

        PnDeliveryRequest pnDeliveryRequest1 = new PnDeliveryRequest();
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest1));

        //mock per riga 78
        //Mockito.when(updateEntityResult(Mockito.any(), (Mockito.any()).publishOn(Schedulers.boundedElastic()).thenReturn(Mono.just(pnDeliveryRequest1));

        //publish in sendEngageRequest
        Mockito.when( Mono.delay(Duration.ofMillis(10)).publishOn(Schedulers.boundedElastic())).thenReturn(Mono.empty());

        //Mockito.when(pnLogAudit.addsBeforeSend(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());

        //Mockito.when(pnLogAudit.addsSuccessSend(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsFailSend(Mockito.any(),Mockito.any(),Mockito.any(),Mockito.any()  )).thenReturn(Mono.empty());

        Mockito.when(paperRequestErrorDAO.created(Mockito.any(), Mockito.any(),  Mockito.any()) ).thenReturn(Mono.empty());

        //Mockito.when(pnLogAudit.addsBeforeDiscard(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsSuccessDiscard(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(Mono.empty());

        PnDeliveryRequest response = paperResultAsyncService.resultAsyncBackground( singleStatusUpdateDto,5).block();
        assertNotNull(response);
    }



    @Test
    void resultAsyncBackgroundReturnNotIsTechnicalErrorStatusCode() {

        SingleStatusUpdateDto singleStatusUpdateDto = new SingleStatusUpdateDto();
        PaperProgressStatusEventDto paperProgressStatusEventDto = new PaperProgressStatusEventDto();
        paperProgressStatusEventDto.setRequestId(Const.RETRY.concat("003"));
        DiscoveredAddressDto discoveredAddressDto = new DiscoveredAddressDto();
        discoveredAddressDto.setAddress("address03");
        discoveredAddressDto.setCap("222");
        discoveredAddressDto.setCity("Roma");
        discoveredAddressDto.setPr("AR");
        paperProgressStatusEventDto.setDiscoveredAddress(discoveredAddressDto);
        singleStatusUpdateDto.setAnalogMail(paperProgressStatusEventDto);

        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest));

        Mockito.when(requestDeliveryDAO.getByRequestId(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest));
        //Mockito.when(pnLogAudit.addsBeforeReceive(Mockito.any(), Mockito.anyString(), Mockito.anyString()).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsSuccessReceive(Mockito.any(), Mockito.any(),  Mockito.any(), Mockito.any(), Mockito.any()).thenReturn(Mono.empty());

        PnDeliveryRequest pnDeliveryRequest1 = new PnDeliveryRequest();
        Mockito.when(requestDeliveryDAO.updateData(Mockito.any())).thenReturn(Mono.just(pnDeliveryRequest1));

        //mock per riga 78
        //Mockito.when(updateEntityResult(Mockito.any(), (Mockito.any()).publishOn(Schedulers.boundedElastic()).thenReturn(Mono.just(pnDeliveryRequest1));


        Mockito.when(paperRequestErrorDAO.created(Mockito.any(), Mockito.any(),  Mockito.any()) ).thenReturn(Mono.empty());

        //Mockito.when(pnLogAudit.addsBeforeDiscard(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(Mono.empty());
        //Mockito.when(pnLogAudit.addsSuccessDiscard(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(Mono.empty());

        PnDeliveryRequest response = paperResultAsyncService.resultAsyncBackground( singleStatusUpdateDto,5).block();
        assertNotNull(response);
    }




}