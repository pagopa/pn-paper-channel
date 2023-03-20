package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
        CostDTO response=CostMapper.toCostDTO(getPnPaperCost(ProductTypeEnumDto.RS.getValue()));
        Assertions.assertNotNull(response);
    }
    @Test
    void costMapperToNationalContractTest() {
        CostDTO response=CostMapper.toCostDTO(getPnPaperCost(ProductTypeEnumDto.AR.getValue()));
        Assertions.assertNotNull(response);
    }
    @Test
    void costMapperFromContractDTOTest() {
        PnCost response=CostMapper.fromCostDTO("ABX_xxx", "driverId", getContractDto());
        Assertions.assertNotNull(response);
    }
    @Test
    void toPageableResponseTest(){
        Pageable pageable = Mockito.mock(Pageable.class, Mockito.CALLS_REAL_METHODS);
        List<PnCost> list= new ArrayList<>();
        PageableCostResponseDto pageableCostResponseDto = CostMapper.toPageableResponse(CostMapper.toPagination(pageable,list));
        Assertions.assertNotNull(pageableCostResponseDto);
    }

    private CostDTO getContractDto(){
        CostDTO contractDto = new CostDTO();
        //contractDto.setCap("00061");
        contractDto.setZone(InternationalZoneEnum._1);
        contractDto.setPrice(0.1F);
        contractDto.setPriceAdditional(0.2F);
        contractDto.setProductType(ProductTypeEnumDto.AR);
        return contractDto;
    }
    private PnCost getPnPaperCost(String str){
        PnCost pnCost = new PnCost();
        //pnCost.setCap("00061");
        pnCost.setPagePrice(0.1F);
        pnCost.setBasePrice(0.5F);
        pnCost.setTenderCode("GARA-2022");
        pnCost.setZone("ZONE_1");
        pnCost.setProductType(str);
        pnCost.setDeliveryDriverCode("12345");
        pnCost.setUuid("12345");
        return pnCost;
    }
}
