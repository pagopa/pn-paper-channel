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
    void createNewSheetTest() {
        String defaultSheet = "Sheet 1";
        String newSheet = "Sheet 2";

        excelEngine.createNewSheet(newSheet);
        Assertions.assertNotNull(excelEngine.getCurrentSheet());

        actualSheets.add(workbook.createSheet(defaultSheet));
        actualSheets.add(workbook.createSheet(newSheet));
        Assertions.assertIterableEquals(actualSheets, excelEngine.getSheets());
    }

    @Test
    void setCurrentSheetTest() {
        String defaultSheet = "Sheet 1";
        excelEngine.getWorkbook().removeSheetAt(0);
        excelEngine.getSheets().remove(excelEngine.getCurrentSheet());
        excelEngine.setCurrentSheet(defaultSheet);
        Assertions.assertEquals(0, excelEngine.getSheets().size());

        excelEngine.createNewSheet(defaultSheet);
        excelEngine.setCurrentSheet(defaultSheet);
        Assertions.assertEquals(1, excelEngine.getSheets().size());

        String sheet = "Sheet 2";
        excelEngine.createNewSheet(sheet);
        excelEngine.setCurrentSheet(sheet);
        Assertions.assertEquals(sheet, excelEngine.getCurrentSheet().getSheetName());

        String newSheet = "Sheet 3";
        excelEngine.createNewSheet(newSheet);
        excelEngine.setCurrentSheet(sheet);
        Assertions.assertEquals(sheet, excelEngine.getCurrentSheet().getSheetName());
    }

    @Test
    void saveOnDiskTest() throws IOException {
        FileOutputStream os = Mockito.mock(FileOutputStream.class);
        XSSFWorkbook workbook = Mockito.mock(XSSFWorkbook.class);
        Mockito.doNothing().when(workbook).write(os);
        Assertions.assertNotNull(excelEngine.saveOnDisk());
//        Mockito.verify(workbook, Mockito.times(1));
//        Mockito.verify(workbook).write(os);
    }

    private void initialize() {
        excelEngine = new ExcelEngine("");
        actualSheets = new ArrayList<>();
        workbook = new XSSFWorkbook();
    }
}
