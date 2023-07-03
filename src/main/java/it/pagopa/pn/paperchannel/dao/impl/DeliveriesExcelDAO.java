package it.pagopa.pn.paperchannel.dao.impl;


import it.pagopa.pn.paperchannel.dao.DAOException;
import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.validator.ExcelValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXCEL_BADLY_CONTENT;
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
        log.info("Start to read data");
        List<PnExcelValidatorException.ErrorCell> errors = new ArrayList<>();
        DeliveriesData response = null;
        try {
            ExcelEngine engine = new ExcelEngine(inputStream);
            List<DeliveryAndCost> data = engine.extractDataLikeTable(
                    map -> ExcelValidator.validateExcel(errors, map),
                    DeliveryAndCost.class);
            response =  new DeliveriesData();
            response.setDeliveriesAndCosts(data);

        } catch (DAOException e){
            throw new PnGenericException(EXCEL_BADLY_FORMAT, e.getMessage());
        } catch (Exception e) {
            throw new PnGenericException(EXCEL_BADLY_CONTENT, e.getMessage());
        }

        if (!errors.isEmpty()){
            throw new PnExcelValidatorException(EXCEL_BADLY_FORMAT, errors);
        }
        return response;
    }

}
