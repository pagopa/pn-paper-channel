package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InternationalZoneEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PageableCostResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnumDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
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
        PnCost pnPaperCost = getPnPaperCost(ProductTypeEnumDto.RS.getValue());
        CostDTO response=CostMapper.toCostDTO(pnPaperCost);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(pnPaperCost.getBasePrice(), response.getPrice());
        Assertions.assertEquals(pnPaperCost.getBasePrice50(), response.getPrice50());
        Assertions.assertEquals(pnPaperCost.getBasePrice100(), response.getPrice100());
        Assertions.assertEquals(pnPaperCost.getBasePrice250(), response.getPrice250());
        Assertions.assertEquals(pnPaperCost.getBasePrice350(), response.getPrice350());
        Assertions.assertEquals(pnPaperCost.getBasePrice1000(), response.getPrice1000());
        Assertions.assertEquals(pnPaperCost.getBasePrice2000(), response.getPrice2000());
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
        contractDto.setPrice(BigDecimal.valueOf(0.1F));
        contractDto.setPriceAdditional(BigDecimal.valueOf(0.2F));
        contractDto.setProductType(ProductTypeEnumDto.AR);
        return contractDto;
    }
    private PnCost getPnPaperCost(String str){
        PnCost pnCost = new PnCost();
        //pnCost.setCap("00061");
        pnCost.setPagePrice(BigDecimal.valueOf(0.1F));
        pnCost.setBasePrice(BigDecimal.valueOf(0.5F));
        pnCost.setBasePrice50(BigDecimal.valueOf(0.5F));
        pnCost.setBasePrice100(BigDecimal.valueOf(0.6F));
        pnCost.setBasePrice250(BigDecimal.valueOf(0.7F));
        pnCost.setBasePrice350(BigDecimal.valueOf(0.8F));
        pnCost.setBasePrice1000(BigDecimal.valueOf(0.9F));
        pnCost.setBasePrice2000(BigDecimal.valueOf(1.0F));
        pnCost.setTenderCode("GARA-2022");
        pnCost.setZone("ZONE_1");
        pnCost.setProductType(str);
        pnCost.setDeliveryDriverCode("12345");
        pnCost.setUuid("12345");
        return pnCost;
    }
}
