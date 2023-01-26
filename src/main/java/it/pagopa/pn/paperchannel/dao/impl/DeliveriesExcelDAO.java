package it.pagopa.pn.paperchannel.dao.impl;


import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.UUID;

@Component
public class DeliveriesExcelDAO implements ExcelDAO<DeliveriesData> {

    private String TEMPLATE_FILENAME = "DeliveryTemplate";
    private String DATA_FILENAME = "Delivery";

    @Override
    public ExcelEngine create(DeliveriesData data) {
        String filename = TEMPLATE_FILENAME;
        if (CollectionUtils.isNotEmpty(data.getDeliveriesAndCosts())) {
            filename = DATA_FILENAME.concat(UUID.randomUUID().toString());
        }
        ExcelEngine excelEngine = new ExcelEngine(filename);
        excelEngine.fillLikeTable(data.getDeliveriesAndCosts(), DeliveryAndCost.class);
        return excelEngine;
    }

    @Override
    public DeliveriesData readData(FileInputStream inputStream) {
        return null;
    }

}
