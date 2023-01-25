package it.pagopa.pn.paperchannel.dao.impl;


import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;

@Component
public class DeliveriesExcelDAO implements ExcelDAO<DeliveriesData> {


    @Override
    public void createAndSave(DeliveriesData data) {
        ExcelEngine excelEngine = new ExcelEngine();
        excelEngine.fillLikeTable(data.getDeliveriesAndCosts(), DeliveryAndCost.class);
        excelEngine.saveOnDisk("Pippo.xlsx");
    }

    @Override
    public DeliveriesData readData(FileInputStream inputStream) {
        return null;
    }

}