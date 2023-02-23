package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.model.PageModel;
import it.pagopa.pn.paperchannel.rest.v1.dto.DeliveryDriverDTO;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.internal.util.Assert;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Pageable;
import reactor.test.StepVerifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.Const.ZONE_2;
import static it.pagopa.pn.paperchannel.utils.Const.ZONE_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryDriverMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<DeliveryDriverMapper> constructor = DeliveryDriverMapper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        assertEquals(null, exception.getMessage());
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




    //@Test
    void errorToEntityFromExcelTest(){
       { /*StepVerifier.create(DeliveryDriverMapper.toEntityFromExcel(getDeliveries(), "ABC"))
                    .expectErrorMatches((ex) -> {
                        assertTrue(ex instanceof PnExcelValidatorException);
                        assertEquals(ExceptionTypeEnum.DATA_NULL_OR_INVALID, ((PnExcelValidatorException) ex).getErrorType());
                        return true;
                    }).verify();*/
        }


    }


    @Test
    void validateCorrectExcel(){
        DeliveriesData deliveriesData = getDeliveriesDataFromCorrectExcel();
        Assert.notNull(DeliveryDriverMapper.toEntityFromExcel(deliveriesData, "tenderCode01"));
    }

    @Test
    void validateInorrectExcelCapMissing(){
        DeliveriesData deliveriesData = getDeliveriesDataFromCorrectExcel();
        deliveriesData.getDeliveriesAndCosts().get(0).setCaps(null);
        try{
            DeliveryDriverMapper.toEntityFromExcel(deliveriesData, "tenderCode01");
        }
        catch (PnExcelValidatorException e){
            Assertions.assertEquals(e.getErrorType().getMessage(), ExceptionTypeEnum.INVALID_CAP_PRODUCT_TYPE.getMessage());
        }
    }

    @Test
    void validateIncorrectExcelZoneMissing(){
        DeliveriesData deliveriesData = getDeliveriesDataFromCorrectExcel();
        deliveriesData.getDeliveriesAndCosts().get(1).setZone(null);
        try{
            DeliveryDriverMapper.toEntityFromExcel(deliveriesData, "tenderCode01");
        }
        catch (PnExcelValidatorException e){
            Assertions.assertEquals(e.getErrorType().getMessage(), ExceptionTypeEnum.INVALID_ZONE_PRODUCT_TYPE.getMessage());
        }
    }


    @Test
    void validateIncorrectExcelDuplicateZone(){
        DeliveriesData deliveriesData = getDeliveriesDataFromCorrectExcel();
        DeliveryDriverMapper.toEntityFromExcel(deliveriesData, "tenderCode01");
    }

    private DeliveriesData getDeliveries() {
        DeliveriesData deliveriesData = new DeliveriesData();
        DeliveryAndCost d1 = new DeliveryAndCost();
        d1.setUniqueCode("AAAAA");
        d1.setTaxId("ID-1");
        d1.setProductType("AR");
        d1.setCaps(List.of("81029, 23945"));
        d1.setFsu(false);
        DeliveryAndCost d2 = new DeliveryAndCost();
        d2.setUniqueCode("AA3AA");
        d2.setTaxId("ID-3");
        d2.setProductType("AR");
        d2.setCaps(List.of("81029, 43945"));
        d2.setFsu(false);
        DeliveryAndCost d3 = new DeliveryAndCost();
        d3.setUniqueCode("AAAAA");
        d3.setTaxId("ID-1");
        d3.setProductType("890");
        d3.setZone("ZONE_1");
        d3.setFsu(false);
        DeliveryAndCost d4 = new DeliveryAndCost();
        d4.setUniqueCode("AAAAA");
        d4.setTaxId("ID-1");
        d4.setProductType("SEMPLICE");
        d4.setCaps(List.of("81029, 23945"));
        d4.setFsu(false);
        DeliveryAndCost d5 = new DeliveryAndCost();
        d5.setUniqueCode("AAAAA");
        d5.setTaxId("ID-1");
        d5.setProductType("890");
        d5.setZone("ZONE_1");
        d5.setFsu(false);
        deliveriesData.setDeliveriesAndCosts(List.of(d1, d2,d3,d4,d5));
        return deliveriesData;
    }

    private DeliveriesData getDeliveriesDataFromCorrectExcel() {
        DeliveriesData deliveriesData = new DeliveriesData();

        //r2
        DeliveryAndCost d1 = new DeliveryAndCost();
        d1.setDenomination("GLS");
        d1.setBusinessName("GLS");
        d1.setTaxId("12344454332");
        d1.setProductType("AR");
        d1.setCaps(List.of("21048, 11111,"));
        float basePrice= (float) 12.45;
        d1.basePrice= basePrice ;
        d1.setFsu(true);

        //r1
        DeliveryAndCost d2 = new DeliveryAndCost();
        d2.setDenomination("GLS");
        d2.setBusinessName("GLS");
        d2.setTaxId("12344454332");
        d2.setProductType("AR");
        d2.setZone(ZONE_2);
        d2.setFsu(true);

        //r3
        DeliveryAndCost d3 = new DeliveryAndCost();
        d3.setDenomination("BRT");
        d3.setBusinessName("BRT");
        d3.setTaxId("12345678901");
        d3.setProductType("RS");
        d3.setZone(ZONE_3);
        d3.setFsu(false);

        //r4
        DeliveryAndCost d4 = new DeliveryAndCost();
        d4.setDenomination("BRT");
        d4.setBusinessName("BRT");
        d4.setTaxId("12345678901");
        d4.setProductType("890");
        d4.setCaps(List.of("22233, 21048,"));
        d4.setFsu(false);

        //r5
        DeliveryAndCost d5 = new DeliveryAndCost();
        d5.setDenomination("sdadasd");
        d5.setBusinessName("IIIII");
        d5.setTaxId("12345643221");
        d5.setProductType("RS");
        d5.setCaps((List.of("22233")));
        d5.setFsu(false);


        //r6
        DeliveryAndCost d6 = new DeliveryAndCost();
        d6.setDenomination("BRT");
        d6.setBusinessName("BRT");
        d6.setTaxId("12345678901");
        d6.setProductType("RS");
        d6.setZone(ZONE_3);
        d6.setFsu(true);

        deliveriesData.setDeliveriesAndCosts(List.of(d1, d2,d3,d4,d5,d6));
        return deliveriesData;
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
}
