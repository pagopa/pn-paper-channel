package it.pagopa.pn.paperchannel.dao.common;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ExcelEngineTest {
    @Spy
    private ExcelEngine excelEngine;
    private XSSFWorkbook workbook;
    private List<XSSFSheet> actualSheets;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void saveOnDiskTest() throws IOException {
        FileOutputStream os = Mockito.mock(FileOutputStream.class);
        XSSFWorkbook workbook = Mockito.mock(XSSFWorkbook.class);
        Mockito.doNothing().when(workbook).write(os);
        Assertions.assertNotNull(excelEngine.saveOnDisk());
    }

    private void initialize() {
        excelEngine = new ExcelEngine("");
        actualSheets = new ArrayList<>();
        workbook = new XSSFWorkbook();
    }
}
