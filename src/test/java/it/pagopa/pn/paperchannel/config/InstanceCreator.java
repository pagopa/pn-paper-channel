package it.pagopa.pn.paperchannel.config;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.utils.Const;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InstanceCreator {


    public static PnDeliveryFile getPnDeliveryFile(String status){
        PnDeliveryFile pnDeliveryFile = new PnDeliveryFile();
        pnDeliveryFile.setUrl("URL_PN_FILE");
        pnDeliveryFile.setStatus(status);
        pnDeliveryFile.setFilename("FILE_123");
        pnDeliveryFile.setUuid(UUID.randomUUID().toString());
        return pnDeliveryFile;
    }

    public static List<PnTender> getListTender(int number) {
        List<PnTender> tenders = new ArrayList<>();
        for (int i=0; i < number; i++){
            PnTender tender = new PnTender();
            tender.setTenderCode("Tender_"+i);
            tender.setStatus("CREATED");
            tenders.add(tender);
        }
        return tenders;
    }

    public static List<PnDeliveryDriver> getListDriver(int number){
        List<PnDeliveryDriver> drivers = new ArrayList<>();
        for (int i=0; i < number; i++){
            PnDeliveryDriver driver = getDriver(false);
            driver.setTaxId(driver.getTaxId()+i);
            drivers.add(driver);
        }
        return drivers;
    }

    public static PnDeliveryDriver getDriver(boolean fsu){
        PnDeliveryDriver driver = new PnDeliveryDriver();
        driver.setTenderCode("TENDER_1");
        driver.setFsu(fsu);
        driver.setTaxId("123456781");

        return driver;
    }

    public static List<PnCost> getAllNationalCost(String tenderCode, String driverCode, boolean fsu) {
        List<PnCost> costs = new ArrayList<>();
        List<ProductTypeEnum> products = List.of(ProductTypeEnum.AR, ProductTypeEnum._890, ProductTypeEnum.RS);
        List<String> caps = new ArrayList<>(List.of("21222", "11111"));
        if (fsu) caps.add(Const.CAP_DEFAULT);
        for (ProductTypeEnum national: products){
            PnCost cost = getCost(tenderCode,null, caps, national.getValue());
            cost.setDeliveryDriverCode(driverCode);
            cost.setFsu(fsu);
            costs.add(cost);
        }
        return costs;
    }

    public static List<PnCost> getAllInternationalCost(String tenderCode, String driverCode, boolean fsu) {
        List<PnCost> costs = new ArrayList<>();
        List<ProductTypeEnum> products = List.of(ProductTypeEnum.AR, ProductTypeEnum.RS);
        List<String> zones = List.of("ZONE_1", "ZONE_2", "ZONE_3");
        for (ProductTypeEnum national: products){
            for (String zone: zones){
                PnCost cost = getCost(tenderCode, zone, null, national.getValue());
                cost.setDeliveryDriverCode(driverCode);
                cost.setFsu(fsu);
                costs.add(cost);
            }

        }
        return costs;
    }

    public static PnCost getCost(String tenderCode, String zone, List<String> cap, String productType){
        PnCost cost = new PnCost();
        cost.setTenderCode(tenderCode);
        cost.setFsu(true);
        cost.setZone(zone);
        cost.setCap(cap);
        cost.setUuid(UUID.randomUUID().toString());
        cost.setBasePrice(1.23F);
        cost.setPagePrice(1.23F);
        cost.setProductType(productType);
        return cost;
    }


}
