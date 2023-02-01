package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.rest.v1.dto.AllPricesContractorResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.InternationalContractDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.InternationalZoneEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.NationalContractDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.TypeRegisteredLetterInterEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

class CostMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<CostMapper> constructor = CostMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void costMappertoInternationalContractTest() {
        InternationalContractDto response=CostMapper.toInternationalContract(getPnPaperCost("AR_INTER"));
        Assertions.assertNotNull(response);
    }
    @Test
    void costMapperToNationalContractTest() {
        NationalContractDto response=CostMapper.toNationalContract(getPnPaperCost("AR"));
        Assertions.assertNotNull(response);
    }
    @Test
    void costMapperToResponseTest() {
        AllPricesContractorResponseDto response=CostMapper.toResponse(new ArrayList<>());
        Assertions.assertNotNull(response);
    }
    @Test
    void costMapperFromContractDTOTest() {
        PnCost response=CostMapper.fromContractDTO(getContractDto());
        Assertions.assertNotNull(response);
    }
    private ContractDto getContractDto(){
        ContractDto contractDto = new ContractDto();
        contractDto.setCap("00061");
        contractDto.setZone(InternationalZoneEnum.fromValue("ZONE_1"));
        contractDto.setPrice(0.1F);
        contractDto.setPriceAdditional(0.2F);
        contractDto.setRegisteredLetter(TypeRegisteredLetterInterEnum.fromValue("AR_INTER"));
        return contractDto;
    }
    private PnCost getPnPaperCost(String str){
        PnCost pnCost = new PnCost();
        pnCost.setCap("00061");
        pnCost.setPagePrice(0.1F);
        pnCost.setBasePrice(0.5F);
        pnCost.setTenderCode("GARA-2022");
        pnCost.setZone("ZONE_1");
        pnCost.setProductType(str);
        pnCost.setIdDeliveryDriver("12345");
        pnCost.setUuid("12345");
        return pnCost;
    }
}
