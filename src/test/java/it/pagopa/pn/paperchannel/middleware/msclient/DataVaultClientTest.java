package it.pagopa.pn.paperchannel.middleware.msclient;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.api.PaperAddressesApi;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.dto.PaperAddressRequestDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pndatavault.v1.dto.PaperAddressResponseDto;
import it.pagopa.pn.paperchannel.middleware.msclient.impl.DataVaultClientImpl;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.PaperAddressRequestInternalDto;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataVaultClientTest {

    private DataVaultClient dataVaultClient;
    private PaperAddressesApi paperAddressesApi;

    @BeforeEach
    void setUp() {
        paperAddressesApi = mock(PaperAddressesApi.class);
        dataVaultClient = new DataVaultClientImpl(paperAddressesApi);
    }


    private PaperAddressRequestInternalDto createRequest(String iun, Integer attempt, Integer recIndex, Address address) {
        PaperAddressRequestInternalDto paperAddressRequestInternalDto = new PaperAddressRequestInternalDto();
        paperAddressRequestInternalDto.setAddress(address);
        paperAddressRequestInternalDto.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_" + 1 + ".ATTEMPT_" + 1);
        paperAddressRequestInternalDto.setAttempt(attempt);
        paperAddressRequestInternalDto.setRecIndex(recIndex);
        paperAddressRequestInternalDto.setPcRetry(0);
        paperAddressRequestInternalDto.setNormalized(true);
        return paperAddressRequestInternalDto;
    }

    private Address createAddress() {
        Address address = new Address();
        address.setAddress("Via Roma 1");
        address.setAddressRow2("Apt 10");
        address.setCap("00100");
        address.setCity("Roma");
        address.setCity2("Roma");
        address.setPr("RM");
        address.setCountry("IT");
        address.setFullName("Mario Rossi");
        address.setNameRow2("Italy");
        return address;
    }

    @Test
    void testCreatePaperAddressWithAllFields() {
        String addressId = UUID.randomUUID().toString();
        Address address = createAddress();
        PaperAddressRequestInternalDto paperAddressRequestInternalDto = createRequest("IUN-001", 0, 0, address);
        PaperAddressResponseDto paperAddressResponseDto = new PaperAddressResponseDto();
        paperAddressResponseDto.setPaperAddressId(addressId);
        ArgumentCaptor<PaperAddressRequestDto> argumentCaptor = ArgumentCaptor.forClass(PaperAddressRequestDto.class);
        when(paperAddressesApi.createPaperAddress(eq("IUN-001"), argumentCaptor.capture()))
                .thenReturn(Mono.just(paperAddressResponseDto));
        StepVerifier.create(dataVaultClient.createPaperAddress("IUN-001", paperAddressRequestInternalDto, AddressTypeEnum.DISCOVERED_ADDRESS))
                        .expectNext(paperAddressResponseDto);
        Assertions.assertEquals(0, argumentCaptor.getValue().getAttempt());
        Assertions.assertEquals(0, argumentCaptor.getValue().getRecIndex());
    }

    @Test
    void testCreatePaperAddressWithoutAttemptAndRecIndex() {
        String addressId = UUID.randomUUID().toString();
        Address address = createAddress();
        PaperAddressRequestInternalDto paperAddressRequestInternalDto = createRequest("IUN-002", null, null, address);
        PaperAddressResponseDto paperAddressResponseDto = new PaperAddressResponseDto();
        paperAddressResponseDto.setPaperAddressId(addressId);
        ArgumentCaptor<PaperAddressRequestDto> argumentCaptor = ArgumentCaptor.forClass(PaperAddressRequestDto.class);
        when(paperAddressesApi.createPaperAddress(eq("IUN-002"), argumentCaptor.capture()))
                .thenReturn(Mono.just(paperAddressResponseDto));
        StepVerifier.create(dataVaultClient.createPaperAddress("IUN-002", paperAddressRequestInternalDto, AddressTypeEnum.DISCOVERED_ADDRESS))
                .expectNext(paperAddressResponseDto);
        Assertions.assertEquals(1, argumentCaptor.getValue().getAttempt());
        Assertions.assertEquals(1, argumentCaptor.getValue().getRecIndex());
    }
}

