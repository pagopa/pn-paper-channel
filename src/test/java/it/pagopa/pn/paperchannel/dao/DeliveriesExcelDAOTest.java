package it.pagopa.pn.paperchannel.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;
import it.pagopa.pn.paperchannel.dao.impl.DeliveriesExcelDAO;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.dao.model.DeliveryAndCost;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.EXCEL_BADLY_CONTENT;
import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class DeliveriesExcelDAOTest extends BaseTest {

    @Autowired
    private DeliveriesExcelDAO deliveriesExcelDAO;
    private final DeliveriesData deliveriesData = new DeliveriesData();
    private final DeliveryAndCost deliveryAndCost1 = new DeliveryAndCost();
    private final DeliveryAndCost deliveryAndCost2 = new DeliveryAndCost();
    private final List<DeliveryAndCost> deliveryAndCostList = new ArrayList<>();

    @BeforeEach
    public void setUp(){
        initialize();
    }

    @Test
    void createTest(){
        ExcelEngine excelEngine = this.deliveriesExcelDAO.create(deliveriesData);
        assertNotNull(excelEngine);
    }
    @Test
    void readDataOKTest(){
        InputStream inputStream = this.getClass().getResourceAsStream("/exceldata/ExcelDownloadTestOK.xlsx");
        DeliveriesData data = this.deliveriesExcelDAO.readData(inputStream);
        assertNotNull(data);

    }
    @Test
    void readDataErrorTest(){
        PnExcelValidatorException exception = assertThrows(PnExcelValidatorException.class, ()-> {
            InputStream inputStream = this.getClass().getResourceAsStream("/exceldata/ExcelDownloadTestFormatKO.xlsx");
            this.deliveriesExcelDAO.readData(inputStream);
        });
        assertEquals("Il file è formattato male", exception.getMessage());
    }
    @Test
    void readDataNullTest(){
        PnGenericException exception = assertThrows(PnGenericException.class, ()-> {
            InputStream inputStream = this.getClass().getResourceAsStream("/ExcelDownloadTestFormatKO.xlsx");
            this.deliveriesExcelDAO.readData(inputStream);
        });
        assertEquals(EXCEL_BADLY_CONTENT, exception.getExceptionType());
    }

    private void initialize(){

        List<String> capList = new ArrayList<>();
        String cap1 = "";
        String cap2 = "";
        capList.add(cap1);
        capList.add(cap2);

        deliveryAndCost1.setFsu(true);
        deliveryAndCost1.setDenomination("Gara2023");
        deliveryAndCost1.setCaps(capList);
        deliveryAndCost1.setTaxId("idTax");
        deliveryAndCost1.setBusinessName("GLS");
        deliveryAndCost1.setFiscalCode("FRMTTR76M06B715E");
        deliveryAndCost1.setPec("gls@gls.pec.it");
        deliveryAndCost1.setZone("ZONA_1");

        deliveryAndCost2.setFsu(false);
        deliveryAndCost2.setDenomination("Gara2023");
        deliveryAndCost2.setCaps(capList);
        deliveryAndCost2.setTaxId("idTax2");
        deliveryAndCost2.setBusinessName("GLS");
        deliveryAndCost2.setFiscalCode("FRMTTR76M06B715E");
        deliveryAndCost2.setPec("gls@gls.pec.it");
        deliveryAndCost2.setZone("ZONA_2");

        deliveryAndCostList.add(deliveryAndCost1);
        deliveryAndCostList.add(deliveryAndCost2);

        deliveriesData.setDeliveriesAndCosts(deliveryAndCostList);
    }
}
