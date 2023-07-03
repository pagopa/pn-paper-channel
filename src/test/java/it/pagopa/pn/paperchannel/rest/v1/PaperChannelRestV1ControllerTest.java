package it.pagopa.pn.paperchannel.rest.v1;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@WebFluxTest(controllers = {PaperChannelRestV1Controller.class})
class PaperChannelRestV1ControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private PaperChannelService paperChannelService;

    @Test
    void takeTenderTest(){
        PageableTenderResponseDto response = new PageableTenderResponseDto();
        String path = "/paper-channel-bo/v1/tenders";
        Mockito.when(paperChannelService.getAllTender(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("value", "GARA2023")
                        .build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void takeDeliveriesDriversTest(){
        PageableDeliveryDriverResponseDto response = new PageableDeliveryDriverResponseDto();
        String path = "/paper-channel-bo/v1/deliveries-drivers/GARA2023";
        Mockito.when(paperChannelService.getAllDeliveriesDrivers(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void addTenderFromFileTest(){
        PresignedUrlResponseDto response = new PresignedUrlResponseDto();
        String path = "/paper-channel-bo/v1/delivery-tender/file-upload";
        Mockito.when(paperChannelService.getPresignedUrl())
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }
    
    @Test
    void downloadTenderFileTest(){
        InfoDownloadDTO response = new InfoDownloadDTO();
        String path = "/paper-channel-bo/v1/delivery-tender/file-download";
        Mockito.when(paperChannelService.downloadTenderFile(Mockito.anyString(),Mockito.anyString()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("tenderCode", "GARA2023")
                        .queryParam("uuid", "1234")
                        .build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void notifyUploadTest(){
        NotifyResponseDto response = new NotifyResponseDto();
        String path = "/paper-channel-bo/v1/delivery-tender/GARA2023/notify-upload";
        Mockito.when(paperChannelService.notifyUpload(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getNotifyRequest())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void createUpdateTenderTest(){
        TenderCreateResponseDTO response = new TenderCreateResponseDTO();
        String path = "/paper-channel-bo/v1/tender";
        Mockito.when(paperChannelService.createOrUpdateTender(Mockito.any()))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getTenderRequest())
                .exchange()
                .expectStatus().isOk();
    }
    @Test
    void createUpdateDriverTest(){
        String path = "/paper-channel-bo/v1/delivery-driver/GARA2023";
        Mockito.when(paperChannelService.createOrUpdateDriver(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getDeliveryDriverRequest())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void createUpdateCostTest(){
        String path = "/paper-channel-bo/v1/GARA2023/delivery-driver/12345/cost";
        Mockito.when(paperChannelService.createOrUpdateCost(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getCostRequest())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getTenderDetailsTest(){
        TenderDetailResponseDTO response = new TenderDetailResponseDTO();
        String path = "/paper-channel-bo/v1/tenders/GARA2023";
        Mockito.when(paperChannelService.getTenderDetails(Mockito.anyString()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getDriverDetailsTest(){
        DeliveryDriverResponseDTO response = new DeliveryDriverResponseDTO();
        String path = "/paper-channel-bo/v1/deliveries-drivers/GARA2023/detail/12345";
        Mockito.when(paperChannelService.getDriverDetails(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getDetailFSUTest(){
        FSUResponseDTO response = new FSUResponseDTO();
        String path = "/paper-channel-bo/v1/deliveries-drivers/GARA2023/fsu";
        Mockito.when(paperChannelService.getDetailsFSU(Mockito.anyString()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getAllCostOfDriverAndTenderTest(){
        PageableCostResponseDto response = new PageableCostResponseDto();
        String path = "/paper-channel-bo/v1/GARA2023/delivery-driver/123456/get-cost";
        Mockito.when(paperChannelService.getAllCostFromTenderAndDriver(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteTenderTest(){
        String path = "/paper-channel-bo/v1/tender/GARA2023";
        Mockito.when(paperChannelService.deleteTender(Mockito.anyString()))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteDriverTest(){
        String path = "/paper-channel-bo/v1/GARA2023/delivery-driver/123455";
        Mockito.when(paperChannelService.deleteDriver(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteCostTest(){
        String path = "/paper-channel-bo/v1/GARA2023/delivery-driver/123567/cost/1222";
        Mockito.when(paperChannelService.deleteCost(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateStatusTenderTest(){
        TenderCreateResponseDTO response = new TenderCreateResponseDTO();
        String path = "/paper-channel-bo/v1/tender/GARA2023";
        Mockito.when(paperChannelService.updateStatusTender(Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.just(response));

        webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getStatusRequest())
                .exchange()
                .expectStatus().isOk();
    }




    private Status getStatusRequest(){
        Status request = new Status();
        request.setStatusCode(Status.StatusCodeEnum.CREATED);
        return request;
    }

    private CostDTO getCostRequest(){
        CostDTO request = new CostDTO();
        List<String> caps = new ArrayList<>();
        caps.add("00166");
        caps.add("00167");
        request.setUid("UUID");
        request.setTenderCode("tenderCode");
        request.setDriverCode("driverCode");
        request.setPrice(10.3f);
        request.setPriceAdditional(11.2f);
        request.setProductType(ProductTypeEnumDto.AR);
        request.setCap(caps);
        request.setZone(InternationalZoneEnum._1);
        return request;
    }

    private DeliveryDriverDTO getDeliveryDriverRequest(){
        DeliveryDriverDTO request = new DeliveryDriverDTO();
        request.setDenomination("denomination");
        request.setBusinessName("businessName");
        request.setRegisteredOffice("registeredOffice");
        request.setPec("pec");
        request.setFiscalCode("fiscalCode");
        request.setTaxId("taxId");
        request.setPhoneNumber("phoneNumber");
        request.setUniqueCode("uniqueCode");
        request.setFsu(true);
        return request;
    }

    private TenderCreateRequestDTO getTenderRequest(){
        TenderCreateRequestDTO request = new TenderCreateRequestDTO();
        request.setCode("1223");
        request.setName("GARA2023");
        request.setStartDate(new Date());
        request.setEndDate(new Date());
        return request;
    }


    private NotifyUploadRequestDto getNotifyRequest(){
        NotifyUploadRequestDto request = new NotifyUploadRequestDto();
        request.setUuid("1234567");
        return request;
    }




}