package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.Utility;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;



public class ExcelValidator {
    private ExcelValidator() {
        throw new IllegalCallerException();
    }

    public static DeliveryAndCost validateExcel(List<PnExcelValidatorException.ErrorCell> errors, Map<String, ExcelEngine.ExcelCell> data) {
        DeliveryAndCost deliveryAndCost = new DeliveryAndCost();

        //denomination check
        ExcelEngine.ExcelCell denomination = data.get("DENOMINATION");
        deliveryAndCost.setDenomination(denomination.getValue());
        if (StringUtils.isBlank(denomination.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(denomination.getRow(), denomination.getCol(), "La cella non può essere vuota"));
        }
        //businessName check
        ExcelEngine.ExcelCell businessName = data.get("BUSINESS_NAME");
        deliveryAndCost.setBusinessName(businessName.getValue());
        if (StringUtils.isBlank(businessName.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(businessName.getRow(), businessName.getCol(), "La cella non può essere vuota"));
        }
        //registeredOffice check
        ExcelEngine.ExcelCell registeredOffice = data.get("OFFICE_NAME");
        deliveryAndCost.setRegisteredOffice(registeredOffice.getValue());
        if (StringUtils.isBlank(registeredOffice.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(registeredOffice.getRow(), registeredOffice.getCol(), "La cella non può essere vuota"));
        }
        //pec check
        ExcelEngine.ExcelCell pec = data.get("PEC");
        deliveryAndCost.setPec(pec.getValue());
        if (StringUtils.isBlank(pec.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(pec.getRow(), pec.getCol(), "La cella non può essere vuota"));
        }
        //fiscalCode check
        ExcelEngine.ExcelCell fiscalCode = data.get("FISCAL_CODE");
        deliveryAndCost.setFiscalCode(fiscalCode.getValue());
        if (StringUtils.isBlank(fiscalCode.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(fiscalCode.getRow(), fiscalCode.getCol(), "La cella non può essere vuota"));
        }
        if (!Utility.isValidFromRegex(fiscalCode.getValue(),Const.fiscalCodeRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(fiscalCode.getRow(), fiscalCode.getCol(), "Problema nel fiscalCode inserito."));
        }
        //taxId check
        ExcelEngine.ExcelCell taxId = data.get("TAX_ID");
        deliveryAndCost.setTaxId(taxId.getValue());
        if (StringUtils.isBlank(taxId.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(taxId.getRow(), taxId.getCol(), "La cella non può essere vuota"));
        }
        if (!Utility.isValidFromRegex(taxId.getValue(),Const.taxIdRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(taxId.getRow(), taxId.getCol(), "Problema nel taxId inserito."));
        }
        //phoneNumber check
        ExcelEngine.ExcelCell phoneNumber = data.get("PHONE_NUMBER");
        deliveryAndCost.setPhoneNumber(phoneNumber.getValue());
        if (StringUtils.isBlank(phoneNumber.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(phoneNumber.getRow(), phoneNumber.getCol(), "La cella non può essere vuota"));
        }
        if (!Utility.isValidFromRegex(phoneNumber.getValue(), Const.phoneNumberRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(phoneNumber.getRow(), phoneNumber.getCol(), "Problema nel phoneNumber inserito."));
        }
        //uniqueCode check
        ExcelEngine.ExcelCell uniqueCode = data.get("UNIQUE_CODE");
        deliveryAndCost.setUniqueCode(uniqueCode.getValue());
        if (StringUtils.isBlank(uniqueCode.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(uniqueCode.getRow(), uniqueCode.getCol(), "La cella non può essere vuota"));
        }
        if (!Utility.isValidFromRegex(uniqueCode.getValue(),Const.uniqueCodeRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(uniqueCode.getRow(), uniqueCode.getCol(), "Problema nello uniqueCode inserito."));

        }
        //fsu check
        ExcelEngine.ExcelCell fsu = data.get("FSU");
        if (StringUtils.isBlank(fsu.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(fsu.getRow(), fsu.getCol(), "La cella non può essere vuota"));
        }
        if (!checkBoolean(fsu.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(fsu.getRow(), fsu.getCol(), "Il tipo di dato non è quello desiderato."));
        } else {
            deliveryAndCost.setFsu(Boolean.valueOf(fsu.getValue()));
        }
        //cap check
        ExcelEngine.ExcelCell cap = data.get("CAP");
        deliveryAndCost.setCap(cap.getValue());
        if (StringUtils.isBlank(cap.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(cap.getRow(), cap.getCol(), "La cella non può essere vuota"));
        }
        if (!Utility.isValidCap(cap.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(cap.getRow(), cap.getCol(), "Problema nei cap inseriti."));
        }
        //zone check
        ExcelEngine.ExcelCell zone = data.get("ZONE");
        deliveryAndCost.setZone(zone.getValue());
        if (StringUtils.isBlank(zone.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(zone.getRow(), zone.getCol(), "La cella non può essere vuota"));
        }
        if (!Utility.isValidFromRegex(zone.getValue(), Const.zoneRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(zone.getRow(), zone.getCol(), "Problema nelle zone inserite."));
        }
        if (!zone.getValue().equals(Const.ZONA_1) && !zone.getValue().equals(Const.ZONA_2) && !zone.getValue().equals(Const.ZONA_3)){
            errors.add(new PnExcelValidatorException.ErrorCell(zone.getRow(), zone.getCol(), "Il tipo di dato non è quello desiderato."));
        }
        //productType check
        ExcelEngine.ExcelCell productType = data.get("PRODUCT_TYPE");
        deliveryAndCost.setProductType(productType.getValue());
        if (StringUtils.isBlank(productType.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(productType.getRow(), productType.getCol(), "La cella non può essere vuota"));
        }
        if (!productType.getValue().equals(Const.RACCOMANDATA_AR) && !productType.getValue().equals(Const.RACCOMANDATA_890) && !productType.getValue().equals(Const.RACCOMANDATA_SEMPLICE)){
            errors.add(new PnExcelValidatorException.ErrorCell(productType.getRow(), productType.getCol(), "Il tipo di dato non è quello desiderato."));
        }
        //basePrice check
        ExcelEngine.ExcelCell basePrice = data.get("BASE_PRICE");
        if (StringUtils.isBlank(basePrice.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(basePrice.getRow(), basePrice.getCol(), "La cella non può essere vuota"));
        }
        else{
            try{
                deliveryAndCost.setBasePrice(Float.valueOf(basePrice.getValue()));
            }
            catch(NumberFormatException ex){
                errors.add(new PnExcelValidatorException.ErrorCell(basePrice.getRow(), basePrice.getCol(), "Formato non valido."));
            }
        }
        //pagePrice check
        ExcelEngine.ExcelCell pagePrice = data.get("PAGE_PRICE");
        deliveryAndCost.setPagePrice(pagePrice.getRow().floatValue());
        if (StringUtils.isBlank(pagePrice.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(pagePrice.getRow(), pagePrice.getCol(), "La cella non può essere vuota"));
        }
        else{
            try{
                deliveryAndCost.setPagePrice(Float.valueOf(pagePrice.getValue()));
            }
            catch(NumberFormatException ex){
                errors.add(new PnExcelValidatorException.ErrorCell(pagePrice.getRow(), pagePrice.getCol(), "Formato non valido."));
            }
        }
        return deliveryAndCost;
    }

    private static boolean checkBoolean(String val){
        val = val.toLowerCase();
        return (val.equals("true") || val.equals("false"));
    }

}
