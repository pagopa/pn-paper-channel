package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.DeliveryDriverDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PageableDeliveryDriverResponseDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.paperchannel.utils.Const.*;
import static org.junit.jupiter.api.Assertions.*;

class DeliveryDriverMapperTest {

    private DeliveriesData fsuWithoutAllZone;
    private DeliveriesData fsuWithoutDefaultNationalCosts;
    private DeliveriesData withInternationalCostsDuplicated;
    private DeliveriesData withNationalCostsDuplicated;
    private DeliveriesData withDataOK;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<DeliveryDriverMapper> constructor = DeliveryDriverMapper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        assertNull(exception.getMessage());
    }

    @Test
    void toEntity(){
        DeliveryDriverDTO driverDTO = new DeliveryDriverDTO();
        driverDTO.setFiscalCode("fiscalCode");
        driverDTO.setTaxId("taxId");
        driverDTO.setFsu(true);
        driverDTO.setPec("pec");
        driverDTO.setBusinessName("businessName");
        driverDTO.setDenomination("denomination");
        driverDTO.setPhoneNumber("phoneNumber");
        driverDTO.setUniqueCode("uniqueCode");
        PnDeliveryDriver deliveryDriver = DeliveryDriverMapper.toEntity(driverDTO);
        Assertions.assertNotNull(deliveryDriver);
    }

    @Test
    void deliveryDriverMapperTest() {
        DeliveryDriverDTO response= DeliveryDriverMapper.deliveryDriverToDto(getPnPaperDeliveryDriver() );
        Assertions.assertNotNull(response);
    }

    @Test
    void deliveryDriverToPageableResponseTest() {
        Pageable pageable =Mockito.mock(Pageable.class, Mockito.CALLS_REAL_METHODS);
        List<PnDeliveryDriver> list= new ArrayList<>();
        PageableDeliveryDriverResponseDto response= DeliveryDriverMapper.toPageableResponse(DeliveryDriverMapper.toPagination(pageable, list));
        Assertions.assertNotNull(response);
    }

    @Test
    void deliveryDriverToPaginationTest() {
        Pageable pageable =Mockito.mock(Pageable.class, Mockito.CALLS_REAL_METHODS);
        List<PnDeliveryDriver> list= new ArrayList<>();
        PageModel<PnDeliveryDriver> response= DeliveryDriverMapper.toPagination(pageable, list);

        Assertions.assertNotNull(response);
    }



    @Test
    @DisplayName("whenFSUHasNotAllInternationCostThrowErrorTest")
    void toEntityFromExcelFSUInternationalTest(){
        try{
            DeliveryDriverMapper.toEntityFromExcel(this.fsuWithoutAllZone, "tenderCode");
        } catch (PnExcelValidatorException e){
            Assertions.assertNotNull(e);
            assertEquals(INVALID_ZONE_FSU, e.getErrorType());
        }
    }

    @Test
    @DisplayName("whenInternationalCostsIsDuplicatedThrowErrorTest")
    void toEntityFromExcelInternationalTest(){
        try{
            DeliveryDriverMapper.toEntityFromExcel(this.withInternationalCostsDuplicated, "tenderCode");
        } catch (PnExcelValidatorException e){
            Assertions.assertNotNull(e);
            assertEquals(INVALID_ZONE_PRODUCT_TYPE, e.getErrorType());
        }
    }

    @Test
    @DisplayName("whenFSUNotHaveAllNationalDefaultCostsThrowErrorTest")
    void toEntityFromExcelFSUNationalDefaultTest(){
        try{
            DeliveryDriverMapper.toEntityFromExcel(this.fsuWithoutDefaultNationalCosts, "tenderCode");
        } catch (PnExcelValidatorException e){
            Assertions.assertNotNull(e);
            assertEquals(INVALID_CAP_FSU, e.getErrorType());
        }
    }

    @Test
    @DisplayName("whenNationalCostsIsDuplicatedThrowErrorTest")
    void toEntityFromExcelNationalCostDuplicatedTest(){
        try{
            DeliveryDriverMapper.toEntityFromExcel(this.withNationalCostsDuplicated, "tenderCode");
        } catch (PnExcelValidatorException e){
            Assertions.assertNotNull(e);
            assertEquals(INVALID_CAP_PRODUCT_TYPE, e.getErrorType());
        }
    }

    @Test
    @DisplayName("whenPassedCorrectDataThenReturnMapDataTest")
    void toEntityFromExcelOkTest(){
        Map<PnDeliveryDriver, List<PnCost>> maps = DeliveryDriverMapper.toEntityFromExcel(this.withDataOK, "tenderCode");

        Assertions.assertNotNull(maps);
        Assertions.assertEquals(1, maps.keySet().size());
        Assertions.assertNotNull(maps.keySet().toArray()[0]);
        Assertions.assertNotNull(maps.get(maps.keySet().toArray()[0]));
        Assertions.assertEquals(withDataOK.getDeliveriesAndCosts().size(), maps.get(maps.keySet().toArray()[0]).size());

    }


    private PnDeliveryDriver getPnPaperDeliveryDriver() {
        PnDeliveryDriver pnDeliveryDriver = new PnDeliveryDriver();
        pnDeliveryDriver.setFiscalCode("FRDYVB568501A");
        pnDeliveryDriver.setUniqueCode("123456");
        pnDeliveryDriver.setTenderCode("GARA-2022");
        pnDeliveryDriver.setDenomination("denomination");
        pnDeliveryDriver.setTaxId("12345");
        pnDeliveryDriver.setPhoneNumber("3397755223");
        pnDeliveryDriver.setFsu(true);
        pnDeliveryDriver.setBusinessName("develop");
        pnDeliveryDriver.setRegisteredOffice("roma");
        pnDeliveryDriver.setPec("mario@pec.it");
        pnDeliveryDriver.setAuthor("author");
        pnDeliveryDriver.setStartDate(new Date().toInstant());
        return pnDeliveryDriver;
    }

    private void initialize(){
        this.fsuWithoutAllZone = new DeliveriesData();
        this.withInternationalCostsDuplicated = new DeliveriesData();
        this.fsuWithoutDefaultNationalCosts = new DeliveriesData();
        this.withNationalCostsDuplicated = new DeliveriesData();
        this.withDataOK = new DeliveriesData();

        List<ProductTypeEnum> pt1 = List.of(ProductTypeEnum.AR);
        List<ProductTypeEnum> pt2 = List.of(ProductTypeEnum.AR, ProductTypeEnum.RS);
        List<ProductTypeEnum> pt3 = List.of(ProductTypeEnum.AR, ProductTypeEnum._890, ProductTypeEnum.RS);
        List<String> capDefault = List.of("99999");

        this.fsuWithoutAllZone.setDeliveriesAndCosts(costsFSU(pt2, ZONE_1, null));


        List<DeliveryAndCost> duplicated = costsFSU(pt2, ZONE_1, null);
        duplicated.addAll(costsFSU(pt1, ZONE_1, null));
        this.withInternationalCostsDuplicated.setDeliveriesAndCosts(duplicated);

        List<DeliveryAndCost> withoutCostDefaultCaps = costsFSU(pt2, ZONE_1, null);
        withoutCostDefaultCaps.addAll(costsFSU(pt2, ZONE_2, null));
        withoutCostDefaultCaps.addAll(costsFSU(pt2, ZONE_3, null));
        withoutCostDefaultCaps.addAll(costsFSU(pt2, null, capDefault));
        this.fsuWithoutDefaultNationalCosts.setDeliveriesAndCosts(withoutCostDefaultCaps);


        List<DeliveryAndCost> duplicatedNationalCosts = costsFSU(pt2, ZONE_1, null);
        duplicatedNationalCosts.addAll(costsFSU(pt2, ZONE_2, null));
        duplicatedNationalCosts.addAll(costsFSU(pt2, ZONE_3, null));
        duplicatedNationalCosts.addAll(costsFSU(pt3, null, capDefault));
        duplicatedNationalCosts.addAll(costsFSU(pt2, null, capDefault));
        this.withNationalCostsDuplicated.setDeliveriesAndCosts(duplicatedNationalCosts);

        List<DeliveryAndCost> withDataOK = costsFSU(pt2, ZONE_1, null);
        withDataOK.addAll(costsFSU(pt2, ZONE_2, null));
        withDataOK.addAll(costsFSU(pt2, ZONE_3, null));
        withDataOK.addAll(costsFSU(pt3, null, capDefault));
        this.withDataOK.setDeliveriesAndCosts(withDataOK);

    }

    private List<DeliveryAndCost> costsFSU(List<ProductTypeEnum> enums, String zone, List<String> caps) {
        return enums.stream().map(productType -> {
            DeliveryAndCost cost= new DeliveryAndCost();
            cost.setDenomination("BRT");
            cost.setBusinessName("BRT");
            cost.setTaxId("12345678901");
            cost.setProductType(productType.getValue());
            if (caps != null){
                cost.setCaps(caps);
            } else {
                cost.setZone(zone);
            }

            cost.setFsu(true);
            return cost;
        }).collect(Collectors.toList());
    }
}
