package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;

import java.util.List;

public class ExcelModelMapper {

    private ExcelModelMapper(){
        throw new IllegalCallerException();
    }


    public static DeliveriesData fromDeliveriesDrivers(List<PnPaperDeliveryDriver> drivers){
        DeliveriesData data = new DeliveriesData();

        if (drivers != null && !drivers.isEmpty()){
            data.setDeliveriesAndCosts(drivers.stream().map(deliveryDriver -> {
                DeliveryAndCost model = new DeliveryAndCost();
                model.setUniqueCode(deliveryDriver.getUniqueCode());
                model.setDenomination(deliveryDriver.getDenomination());
                model.setFsu(deliveryDriver.getFsu());
                model.setPec(deliveryDriver.getPec());
                model.setBusinessName(deliveryDriver.getBusinessName());
                model.setFiscalCode(deliveryDriver.getFiscalCode());
                model.setRegisteredOffice(deliveryDriver.getRegisteredOffice());
                model.setPhoneNumber(deliveryDriver.getPhoneNumber());
                return model;
            }).toList());
        }
        return data;

    }


}
