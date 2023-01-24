package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;

import java.util.ArrayList;
import java.util.List;

public class ExcelModelMapper {

    private ExcelModelMapper(){
        throw new IllegalCallerException();
    }


    public static DeliveriesData fromDeliveriesDrivers(List<PnPaperDeliveryDriver> drivers, List<PnPaperCost> costs){
        DeliveriesData data = new DeliveriesData();
        if (drivers != null && !drivers.isEmpty())  {
            data.setDeliveriesAndCosts(new ArrayList<>());
            drivers.forEach(driver -> {
                List<PnPaperCost> driverCosts = costs.stream().filter(cost -> cost.getIdDeliveryDriver().equals(driver.getUniqueCode()))
                        .toList();
                if (!driverCosts.isEmpty()){
                    data.getDeliveriesAndCosts().addAll(driverCosts.stream().map(cost -> {
                        DeliveryAndCost model = new DeliveryAndCost();
                        model.setUniqueCode(driver.getUniqueCode());
                        model.setDenomination(driver.getDenomination());
                        model.setFsu(driver.getFsu());
                        model.setPec(driver.getPec());
                        model.setBusinessName(driver.getBusinessName());
                        model.setFiscalCode(driver.getFiscalCode());
                        model.setRegisteredOffice(driver.getRegisteredOffice());
                        model.setPhoneNumber(driver.getPhoneNumber());
                        model.setProductType(cost.getProductType());
                        model.setCap(cost.getCap());
                        model.setZone(cost.getZone());
                        model.setBasePrice(cost.getBasePrice());
                        model.setPagePrice(cost.getPagePrice());
                        return model;
                    }).toList());
                    return ;
                }
                DeliveryAndCost model = new DeliveryAndCost();
                model.setUniqueCode(driver.getUniqueCode());
                model.setDenomination(driver.getDenomination());
                model.setFsu(driver.getFsu());
                model.setPec(driver.getPec());
                model.setBusinessName(driver.getBusinessName());
                model.setFiscalCode(driver.getFiscalCode());
                model.setRegisteredOffice(driver.getRegisteredOffice());
                model.setPhoneNumber(driver.getPhoneNumber());
                data.getDeliveriesAndCosts().add(model);
            });
        }
        return data;
    }


}
