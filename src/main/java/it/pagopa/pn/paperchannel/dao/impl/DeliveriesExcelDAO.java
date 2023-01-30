package it.pagopa.pn.paperchannel.dao.impl;


import it.pagopa.pn.paperchannel.dao.DAOException;
import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
            List<PnExcelValidatorException.ErrorCell> errors = new ArrayList<>();
            ExcelEngine engine = new ExcelEngine(inputStream);
            List<DeliveryAndCost> data = engine.extractDataLikeTable(map -> {
                log.info(map.toString());
                DeliveryAndCost cost = new DeliveryAndCost();
                //Denomination
                ExcelEngine.ExcelCell cella = map.get("FSU");
                if (StringUtils.isBlank(cella.getValue())){
                    errors.add(new PnExcelValidatorException.ErrorCell(cella.getRow(), cella.getCol(), "La cella non può essere vuota"));
                } else if (!Boolean.valueOf(cella.getValue()).toString().equalsIgnoreCase(cella.getValue())){
                    errors.add(new PnExcelValidatorException.ErrorCell(cella.getRow(), cella.getCol(), "Il tipo "));
                }

                //return  validator.validate(errors, map)
                return new DeliveryAndCost();
            }, DeliveryAndCost.class);

            if (!errors.isEmpty()){
                throw new PnExcelValidatorException(EXCEL_BADLY_FORMAT, errors);
            }

            DeliveriesData response = new DeliveriesData();
            response.setDeliveriesAndCosts(data);
            return response;
        } catch (IOException e) {
            log.error("ERROR WITH EXCEL : {}", e.getMessage());
            return null;
        } catch (DAOException e){
            throw new PnGenericException(EXCEL_BADLY_FORMAT, e.getMessage());
        }
    }

}
