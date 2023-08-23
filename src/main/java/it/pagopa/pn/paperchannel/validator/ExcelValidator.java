package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Slf4j
public class ExcelValidator {
    private ExcelValidator() {
        throw new IllegalCallerException();
    }

    public static DeliveryAndCost validateExcel(List<PnExcelValidatorException.ErrorCell> errors, Map<String, ExcelEngine.ExcelCell> data) {
        DeliveryAndCost deliveryAndCost = new DeliveryAndCost();

        //businessName check
        ExcelEngine.ExcelCell businessName = data.get("BUSINESS_NAME");
        deliveryAndCost.setBusinessName(businessName.getValue());
        if (StringUtils.isBlank(businessName.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(businessName.getRow(), businessName.getCol(), "BUSINESS_NAME","Campo Business name deve essere valorizzato"));
        }

        ExcelEngine.ExcelCell denomination = data.get("DENOMINATION");
        deliveryAndCost.setDenomination(denomination.getValue());

        ExcelEngine.ExcelCell registeredOffice = data.get("OFFICE_NAME");
        deliveryAndCost.setRegisteredOffice(registeredOffice.getValue());

        ExcelEngine.ExcelCell pec = data.get("PEC");
        deliveryAndCost.setPec(pec.getValue());


        ExcelEngine.ExcelCell phoneNumber = data.get("PHONE_NUMBER");
        deliveryAndCost.setPhoneNumber(phoneNumber.getValue());

        ExcelEngine.ExcelCell uniqueCode = data.get("UNIQUE_CODE");
        deliveryAndCost.setUniqueCode(uniqueCode.getValue());
        if (StringUtils.isNotBlank(uniqueCode.getValue()) && !Utility.isValidFromRegex(uniqueCode.getValue(), Const.uniqueCodeRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(uniqueCode.getRow(), uniqueCode.getCol(), "UNIQUE_CODE","Il formato del codice univoco è errato"));
        }

        ExcelEngine.ExcelCell fiscalCode = data.get("FISCAL_CODE");
        deliveryAndCost.setFiscalCode(fiscalCode.getValue());
        if (StringUtils.isNotBlank(fiscalCode.getValue()) && !Utility.isValidFromRegex(fiscalCode.getValue(), Const.fiscalCodeRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(fiscalCode.getRow(), fiscalCode.getCol(), "FISCAL_CODE","Il formato del codice fiscale è errato"));
        }


        //taxId check
        ExcelEngine.ExcelCell taxId = data.get("TAX_ID");
        deliveryAndCost.setTaxId(taxId.getValue());
        if (StringUtils.isBlank(taxId.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(taxId.getRow(), taxId.getCol(), "TAX_ID","Campo Partita iva deve essere valorizzato"));
        } else if (!Utility.isValidFromRegex(taxId.getValue(),Const.taxIdRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(taxId.getRow(), taxId.getCol(), "TAX_ID","Partita iva non corretto"));
        }

        //fsu check
        ExcelEngine.ExcelCell fsu = data.get("FSU");
        if (StringUtils.isBlank(fsu.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(fsu.getRow(), fsu.getCol(), "FSU", "Campo FSU deve essere valorizzato"));
        }
        if (!checkBoolean(fsu.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(fsu.getRow(), fsu.getCol(), "FSU", "Il tipo di dato non è quello desiderato."));
        } else {
            deliveryAndCost.setFsu(Boolean.valueOf(fsu.getValue()));
        }

        //cap check
        ExcelEngine.ExcelCell cap = data.get("CAP");
        ExcelEngine.ExcelCell zone = data.get("ZONE");
        if (StringUtils.isBlank(cap.getValue()) && StringUtils.isBlank(zone.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(cap.getRow(), cap.getCol(), "CAP", "Attenzione, cap e zona sono vuoti. Devi inserire uno dei due campi"));
        }

        if (StringUtils.isNotBlank(cap.getValue())){
            List<String> caps = Utility.isValidCap(cap.getValue());
            if (caps == null) {
                errors.add(new PnExcelValidatorException.ErrorCell(cap.getRow(), cap.getCol(), "CAP", "Sono presenti duplicati nei cap inseriti."));
            } else {
                deliveryAndCost.setCaps(caps);
            }
        }

        //zone check
        if (!StringUtils.isBlank(zone.getValue()) && !Utility.isValidFromRegex(zone.getValue(), Const.zoneRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(zone.getRow(), zone.getCol(),"ZONE", "Il valore inserito per la zona non è tra i valori ammissibili. (ZONE_1, ZONE_2, ZONE_3)"));
        } else {
            deliveryAndCost.setZone(zone.getValue());
        }


        //productType check
        ExcelEngine.ExcelCell productType = data.get("PRODUCT_TYPE");
        deliveryAndCost.setProductType(productType.getValue());
        if (StringUtils.isBlank(productType.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(productType.getRow(), productType.getCol(), "PRODUCT_TYPE", "Il campo product type deve essere valorizzato"));
        } else if (!StringUtils.equals(productType.getValue(), Const.RACCOMANDATA_AR) &&
                !StringUtils.equals(productType.getValue(), Const.RACCOMANDATA_890) &&
                !StringUtils.equals(productType.getValue(), Const.RACCOMANDATA_SEMPLICE)){
            errors.add(new PnExcelValidatorException.ErrorCell(productType.getRow(), productType.getCol(), "PRODUCT_TYPE", "Il tipo di prodotto non è corretto. (AR, 890, SEMPLICE)"));
        }

        //basePrice check
        ExcelEngine.ExcelCell basePrice = data.get("BASE_PRICE");
        if (StringUtils.isBlank(basePrice.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(basePrice.getRow(), basePrice.getCol(), "BASE_PRICE", "Il campo base price deve essere valorizzato"));
        } else{
            deliveryAndCost.setBasePrice(getBigDecimal(basePrice.getValue()));
            if (deliveryAndCost.getBasePrice() == null) {
                errors.add(new PnExcelValidatorException.ErrorCell(basePrice.getRow(), basePrice.getCol(),"BASE_PRICE",  "Formato non valido."));
            }
        }

        //pagePrice check
        ExcelEngine.ExcelCell pagePrice = data.get("PAGE_PRICE");
        if (StringUtils.isBlank(pagePrice.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(pagePrice.getRow(), pagePrice.getCol(), "PAGE_PRICE", "Il campo page price deve essere valorizzato"));
        } else{
            deliveryAndCost.setPagePrice(getBigDecimal(pagePrice.getValue()));
            if (deliveryAndCost.getPagePrice() == null) {
                errors.add(new PnExcelValidatorException.ErrorCell(pagePrice.getRow(), pagePrice.getCol(), "PAGE_PRICE", "Formato non valido."));
            }
        }
        return deliveryAndCost;
    }

    private static boolean checkBoolean(String val){
        val = val.toLowerCase();
        return (val.equals("true") || val.equals("false"));
    }

    private static BigDecimal getBigDecimal(String value) {
        BigDecimal number = null;
        try {
            number = Utility.toBigDecimal(value);
        } catch (ParseException ex) {
            log.error("Error ParseException cost");
        }
        return number;
    }

    private static Float getFloat(String value){
        Float f = null;
        try{
            f = Float.valueOf(value);
        }
        catch(NumberFormatException ex){
            log.error("Error NumberFormatException");
        }
        return f;
    }
}
