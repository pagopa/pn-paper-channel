package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
            errors.add(new PnExcelValidatorException.ErrorCell(businessName.getRow(), businessName.getCol(), "Campo Business name deve essere valorizzato"));
        }

        //taxId check
        ExcelEngine.ExcelCell taxId = data.get("TAX_ID");
        deliveryAndCost.setTaxId(taxId.getValue());
        if (StringUtils.isBlank(taxId.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(taxId.getRow(), taxId.getCol(), "Campo Partita ive deve essere valorizzato"));
        }
        if (!Utility.isValidFromRegex(taxId.getValue(),Const.taxIdRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(taxId.getRow(), taxId.getCol(), "Partita iva non corretto"));
        }

        //fsu check
        ExcelEngine.ExcelCell fsu = data.get("FSU");
        if (StringUtils.isBlank(fsu.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(fsu.getRow(), fsu.getCol(), "Campo FSU deve essere valorizzato"));
        }
        if (!checkBoolean(fsu.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(fsu.getRow(), fsu.getCol(), "Il tipo di dato non è quello desiderato."));
        } else {
            deliveryAndCost.setFsu(Boolean.valueOf(fsu.getValue()));
        }

        //cap check
        ExcelEngine.ExcelCell cap = data.get("CAP");
        if (StringUtils.isNotBlank(cap.getValue())){
            List<String> caps = Utility.isValidCap(cap.getValue());
            if (caps == null) {
                errors.add(new PnExcelValidatorException.ErrorCell(cap.getRow(), cap.getCol(), "Sono presenti duplicati nei cap inseriti."));
            }
        }

        //zone check
        ExcelEngine.ExcelCell zone = data.get("ZONE");
        if (!StringUtils.isBlank(zone.getValue()) && !Utility.isValidFromRegex(zone.getValue(), Const.zoneRegex)){
            errors.add(new PnExcelValidatorException.ErrorCell(zone.getRow(), zone.getCol(), "Problema nelle zone inserite."));
        } else if (StringUtils.isNotEmpty(zone.getValue())) {
            deliveryAndCost.setZone(zone.getValue());
        }
        if (!StringUtils.isBlank(zone.getValue())){
            boolean zone_ok = false;
            if (Utility.isValidFromRegex(zone.getValue(),Const.ZONE_1)) {
                zone_ok = true;
                log.info("Formato zone corretto.");
            }
            else if (Utility.isValidFromRegex(zone.getValue(),Const.ZONE_2)) {
                zone_ok = true;
                log.info("Formato zone corretto.");
            }
            else if (Utility.isValidFromRegex(zone.getValue(),Const.ZONE_3)){
                zone_ok = true;
                log.info("Formato zone corretto.");
            }
            else{
                errors.add(new PnExcelValidatorException.ErrorCell(zone.getRow(), zone.getCol(), "Il tipo di dato non è quello desiderato."));
            }
        }
        if (StringUtils.isBlank(zone.getValue()) && StringUtils.isBlank(cap.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(zone.getRow(), zone.getCol(), "Cap o Zone deve essere valorizzato"));
        }

        //productType check
        ExcelEngine.ExcelCell productType = data.get("PRODUCT_TYPE");
        deliveryAndCost.setProductType(productType.getValue());
        if (StringUtils.isBlank(productType.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(productType.getRow(), productType.getCol(), "Il campo product type deve essere valorizzato"));
        }
        boolean productType_ok = false;
        if (Utility.isValidFromRegex(productType.getValue(), Const.RACCOMANDATA_AR)){
            productType_ok = true;
            log.info("Formato productType corretto.");
        }
        else if (Utility.isValidFromRegex(productType.getValue().replace(".0",""), Const.RACCOMANDATA_890)){
            productType_ok = true;
            log.info("Formato productType corretto.");
        }
        else if (Utility.isValidFromRegex(productType.getValue(), Const.RACCOMANDATA_SEMPLICE)) {
            productType_ok = true;
            log.info("Formato productType corretto.");
        }
        else {
            errors.add(new PnExcelValidatorException.ErrorCell(productType.getRow(), productType.getCol(), "Il tipo di dato non è quello desiderato."));
        }

        //basePrice check
        ExcelEngine.ExcelCell basePrice = data.get("BASE_PRICE");
        if (StringUtils.isBlank(basePrice.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(basePrice.getRow(), basePrice.getCol(), "Il campo base price deve essere valorizzato"));
        } else{
            deliveryAndCost.setBasePrice(getFloat(basePrice.getValue()));
            if (deliveryAndCost.getBasePrice() == null) {
                errors.add(new PnExcelValidatorException.ErrorCell(basePrice.getRow(), basePrice.getCol(), "Formato non valido."));
            }
        }

        //pagePrice check
        ExcelEngine.ExcelCell pagePrice = data.get("PAGE_PRICE");
        deliveryAndCost.setPagePrice(pagePrice.getRow().floatValue());
        if (StringUtils.isBlank(pagePrice.getValue())){
            errors.add(new PnExcelValidatorException.ErrorCell(pagePrice.getRow(), pagePrice.getCol(), "Il campo page price deve essere valorizzato"));
        } else{
            deliveryAndCost.setPagePrice(getFloat(pagePrice.getValue()));
            if (deliveryAndCost.getPagePrice() == null) {
                errors.add(new PnExcelValidatorException.ErrorCell(pagePrice.getRow(), pagePrice.getCol(), "Formato non valido."));
            }
        }
        return deliveryAndCost;
    }

    private static boolean checkBoolean(String val){
        val = val.toLowerCase();
        return (val.equals("true") || val.equals("false"));
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
