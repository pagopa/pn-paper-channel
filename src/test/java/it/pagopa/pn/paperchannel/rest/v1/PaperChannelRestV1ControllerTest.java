package it.pagopa.pn.paperchannel.rest.v1;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.model.Contract;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
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

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(controllers = {PaperChannelRestV1Controller.class})
class PaperChannelRestV1ControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaperChannelService paperChannelService;

    @Test
    void addContract() {
        BaseResponse response = new BaseResponse();
        String path = "/paper-channel/backoffice/v1/delivery-tender/contract";
        Mockito.when(paperChannelService.createContract(Mockito.any()))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .bodyValue(getContractInsertRequestDto())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void takeDeliveryDriver() {
        PageableDeliveryDriverResponseDto response = new PageableDeliveryDriverResponseDto();
        String path = "/paper-channel/backoffice/v1/deliveries-drivers";
        Mockito.when(paperChannelService.takeDeliveryDriver(Mockito.any()))
                .thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .exchange()
                .expectStatus().isOk();
    }

    private ContractInsertRequestDto getContractInsertRequestDto(){
        ContractInsertRequestDto contractInsertRequestDto = new ContractInsertRequestDto();
        List<ContractDto> contractDtoList = new ArrayList<>();
        ContractDto contractDto = new ContractDto();
        contractDto.setCap("cap");
        contractDto.setStartDate(new Date());
        contractDto.setEndDate(new Date());
        contractDto.setPrice(contractDto.getPrice());
        contractDto.setPriceAdditional(contractDto.getPriceAdditional());
        contractDto.setZone(InternationalZoneEnum._1);
        contractDto.setRegisteredLetter(TypeRegisteredLetterInterEnum.AR_INTER);
        contractDtoList.add(contractDto);

        contractInsertRequestDto.setBusinessName("businessName");
        contractInsertRequestDto.setCodeFiscal("fiscalCode");
        contractInsertRequestDto.setDenomination("denomination");
        contractInsertRequestDto.setFsu(true);
        contractInsertRequestDto.setPhoneNumber("phoneNumber");
        contractInsertRequestDto.setPec("pec");
        contractInsertRequestDto.setRegisteredOffice("registeredOffice");
        contractInsertRequestDto.setTaxId("taxId");
        contractInsertRequestDto.setUniqueCode("uniqueCode");

        return contractInsertRequestDto;

    }
}