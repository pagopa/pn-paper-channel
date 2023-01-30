package it.pagopa.pn.paperchannel.dao.impl;


import it.pagopa.pn.paperchannel.dao.DAOException;
import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXCEL_BADLY_FORMAT;

@Slf4j
@Component
public class DeliveriesExcelDAO implements ExcelDAO<DeliveriesData> {

    private static final String TEMPLATE_FILENAME = "DeliveryTemplate";
    private static final String DATA_FILENAME = "Delivery";

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
    public DeliveriesData readData(InputStream inputStream) {
        try {
            ExcelEngine engine = new ExcelEngine(inputStream);
            engine.extractDataLikeTable(map -> {
                log.info(map.toString());
                //call validator
                    //throws error validation or return DeliveryAndCost
                return new DeliveryAndCost();
            }, DeliveryAndCost.class);
            return new DeliveriesData();
        } catch (IOException e) {
            log.error("ERROR WITH EXCEL : {}", e.getMessage());
            return null;
        } catch (DAOException e){
            throw new PnGenericException(EXCEL_BADLY_FORMAT, e.getMessage());
        }
    }

}
