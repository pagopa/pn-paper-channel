package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
class ExcelValidatorTest {

    private Map<String, ExcelEngine.ExcelCell> dataExcelWithoutRequiredField;

    private Map<String, ExcelEngine.ExcelCell> dataExcelWithErrorType;
    private Map<String, ExcelEngine.ExcelCell> dataExcelOk;

    @BeforeEach
    void setUp(){
        settingMapModelExcel();
    }


    @Test
    void whenValidateRowWithoutRequiredField(){
        List<PnExcelValidatorException.ErrorCell> errors = new ArrayList<>();
        ExcelValidator.validateExcel(errors, dataExcelWithoutRequiredField);
        Assertions.assertFalse(errors.isEmpty());
        List<String> errorsExpected = List.of("BUSINESS_NAME", "TAX_ID", "CAP", "BASE_PRICE", "PAGE_PRICE");
        errors.forEach(error-> {
            Assertions.assertTrue(errorsExpected.contains(error.getColName()));
        });
    }

    @Test
    void whenValidateRowWithMismatchedTypeField(){
        List<PnExcelValidatorException.ErrorCell> errors = new ArrayList<>();
        ExcelValidator.validateExcel(errors, dataExcelWithErrorType);
        Assertions.assertFalse(errors.isEmpty());
        List<String> errorsExpected = List.of("UNIQUE_CODE", "FISCAL_CODE", "TAX_ID", "FSU", "CAP", "PRODUCT_TYPE", "BASE_PRICE", "PAGE_PRICE");
        errors.forEach(error-> {
            Assertions.assertTrue(errorsExpected.contains(error.getColName()));
        });
    }

    @Test
    void whenDataExcelOKThenReturnEmptyErrorsAndDeliveryData(){
        List<PnExcelValidatorException.ErrorCell> errors = new ArrayList<>();
        DeliveryAndCost deliveryAndCost = ExcelValidator.validateExcel(errors, dataExcelOk);
        Assertions.assertTrue(errors.isEmpty());

        Assertions.assertTrue(StringUtils.isNotEmpty(deliveryAndCost.getDenomination()));
        Assertions.assertTrue(StringUtils.isNotEmpty(deliveryAndCost.getBusinessName()));
        Assertions.assertTrue(StringUtils.isNotEmpty(deliveryAndCost.getRegisteredOffice()));
        Assertions.assertTrue(StringUtils.isNotEmpty(deliveryAndCost.getPec()));
        Assertions.assertTrue(StringUtils.isNotEmpty(deliveryAndCost.getTaxId()));
        Assertions.assertTrue(StringUtils.isNotEmpty(deliveryAndCost.getFiscalCode()));
        Assertions.assertTrue(deliveryAndCost.getFsu());
        Assertions.assertNull(deliveryAndCost.getCaps());
        Assertions.assertEquals(dataExcelOk.get("ZONE").getValue(), deliveryAndCost.getZone());
        Assertions.assertEquals(Float.valueOf(dataExcelOk.get("BASE_PRICE").getValue()),deliveryAndCost.getBasePrice());
        Assertions.assertEquals(Float.valueOf(dataExcelOk.get("PAGE_PRICE").getValue()),deliveryAndCost.getPagePrice());
    }

    @Test
    void whenDateExcelCapsBadlyFormattedThenReturnErrors(){
        ExcelEngine.ExcelCell dataZone = dataExcelOk.get("ZONE");
        dataZone.setValue("");
        ExcelEngine.ExcelCell dataCap = dataExcelOk.get("CAP");
        dataCap.setValue("10000, 34322, 10000");

        List<PnExcelValidatorException.ErrorCell> errors = new ArrayList<>();
        DeliveryAndCost deliveryAndCost = ExcelValidator.validateExcel(errors, dataExcelOk);
        Assertions.assertFalse(errors.isEmpty());
        Assertions.assertEquals(1, errors.size());
        Assertions.assertEquals("CAP", errors.get(0).getColName());

        dataCap.setValue("10000, 00100-00600");
        errors = new ArrayList<>();
        deliveryAndCost = ExcelValidator.validateExcel(errors, dataExcelOk);
        Assertions.assertTrue(errors.isEmpty());
        Assertions.assertNotNull(deliveryAndCost.getCaps());
        Assertions.assertFalse(deliveryAndCost.getCaps().isEmpty());
        Assertions.assertEquals(502, deliveryAndCost.getCaps().size());
        Assertions.assertEquals("00100", deliveryAndCost.getCaps().get(1));

        dataCap.setValue("00120, 00100-00600");
        errors = new ArrayList<>();
        deliveryAndCost = ExcelValidator.validateExcel(errors, dataExcelOk);
        Assertions.assertFalse(errors.isEmpty());
        Assertions.assertEquals(1, errors.size());
        Assertions.assertEquals("CAP", errors.get(0).getColName());

        dataCap.setValue("00120, 00900-01100");
        errors = new ArrayList<>();
        deliveryAndCost = ExcelValidator.validateExcel(errors, dataExcelOk);
        Assertions.assertTrue(errors.isEmpty());
        Assertions.assertNotNull(deliveryAndCost.getCaps());
        Assertions.assertFalse(deliveryAndCost.getCaps().isEmpty());
        Assertions.assertEquals(202, deliveryAndCost.getCaps().size());
        Assertions.assertEquals("01100", deliveryAndCost.getCaps().get(deliveryAndCost.getCaps().size()-1));

    }


    private void settingMapModelExcel(){
        dataExcelWithoutRequiredField = new HashMap<>();
        dataExcelWithoutRequiredField.put("DENOMINATION", new ExcelEngine.ExcelCell(1,1, "DENOMINATION"));
        dataExcelWithoutRequiredField.put("BUSINESS_NAME", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("OFFICE_NAME", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("PEC", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("FISCAL_CODE", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("TAX_ID", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("PHONE_NUMBER", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("UNIQUE_CODE", new ExcelEngine.ExcelCell(1,1, "abc-23111"));
        dataExcelWithoutRequiredField.put("FSU", new ExcelEngine.ExcelCell(1,1, "false"));
        dataExcelWithoutRequiredField.put("CAP", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("ZONE", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("PRODUCT_TYPE", new ExcelEngine.ExcelCell(1,1, "AR"));
        dataExcelWithoutRequiredField.put("BASE_PRICE", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithoutRequiredField.put("PAGE_PRICE", new ExcelEngine.ExcelCell(1,1, ""));

        dataExcelWithErrorType = new HashMap<>();
        dataExcelWithErrorType.put("DENOMINATION", new ExcelEngine.ExcelCell(1,1, "DENOMINATION"));
        dataExcelWithErrorType.put("BUSINESS_NAME", new ExcelEngine.ExcelCell(1,1, "BUsiness name"));
        dataExcelWithErrorType.put("OFFICE_NAME", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithErrorType.put("PEC", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithErrorType.put("FISCAL_CODE", new ExcelEngine.ExcelCell(1,1, "LLLooddk2043473847"));
        dataExcelWithErrorType.put("TAX_ID", new ExcelEngine.ExcelCell(1,1, "LLLooddk2043473847"));
        dataExcelWithErrorType.put("PHONE_NUMBER", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithErrorType.put("UNIQUE_CODE", new ExcelEngine.ExcelCell(1,1, "abc-23111"));
        dataExcelWithErrorType.put("FSU", new ExcelEngine.ExcelCell(1,1, "NONBOOLEANTYPE"));
        dataExcelWithErrorType.put("CAP", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithErrorType.put("ZONE", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelWithErrorType.put("PRODUCT_TYPE", new ExcelEngine.ExcelCell(1,1, "AR-3443"));
        dataExcelWithErrorType.put("BASE_PRICE", new ExcelEngine.ExcelCell(1,1, "abc"));
        dataExcelWithErrorType.put("PAGE_PRICE", new ExcelEngine.ExcelCell(1,1, "abc"));

        dataExcelOk = new HashMap<>();
        dataExcelOk.put("DENOMINATION", new ExcelEngine.ExcelCell(1,1, "DENOMINATION"));
        dataExcelOk.put("BUSINESS_NAME", new ExcelEngine.ExcelCell(1,1, "BUSINESS_TYPE"));
        dataExcelOk.put("OFFICE_NAME", new ExcelEngine.ExcelCell(1,1, "OFFICE_NAME"));
        dataExcelOk.put("PEC", new ExcelEngine.ExcelCell(1,1, "pec.test@gmail.com"));
        dataExcelOk.put("FISCAL_CODE", new ExcelEngine.ExcelCell(1,1, "FRIMSC80A01F205O"));
        dataExcelOk.put("TAX_ID", new ExcelEngine.ExcelCell(1,1, "12345678901"));
        dataExcelOk.put("PHONE_NUMBER", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelOk.put("UNIQUE_CODE", new ExcelEngine.ExcelCell(1,1, "CJRFTES"));
        dataExcelOk.put("FSU", new ExcelEngine.ExcelCell(1,1, "true"));
        dataExcelOk.put("CAP", new ExcelEngine.ExcelCell(1,1, ""));
        dataExcelOk.put("ZONE", new ExcelEngine.ExcelCell(1,1, "ZONE_1"));
        dataExcelOk.put("PRODUCT_TYPE", new ExcelEngine.ExcelCell(1,1, "890"));
        dataExcelOk.put("BASE_PRICE", new ExcelEngine.ExcelCell(1,1, "1.23"));
        dataExcelOk.put("PAGE_PRICE", new ExcelEngine.ExcelCell(1,1, "2.23"));

    }



}
