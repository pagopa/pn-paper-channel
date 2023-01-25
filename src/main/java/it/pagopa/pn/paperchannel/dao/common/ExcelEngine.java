package it.pagopa.pn.paperchannel.dao.common;

import it.pagopa.pn.paperchannel.dao.model.ColumnExcel;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.TempFile;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.*;

@Getter
@Slf4j
public class ExcelEngine {
    private final XSSFWorkbook workbook;
    private final List<XSSFSheet> sheets;
    private XSSFSheet currentSheet;


    public ExcelEngine(){
        this.workbook = new XSSFWorkbook();
        this.currentSheet = workbook.createSheet("Sheet 1");
        this.sheets = new ArrayList<>();
        this.sheets.add(this.currentSheet);
    }

    public void createNewSheet(String nameSheet){
        this.currentSheet = workbook.createSheet(nameSheet);
        this.sheets.add(this.currentSheet);
    }

    public void setCurrentSheet(String nameSheet){
        if (sheets.isEmpty()) return;
        if (currentSheet.getSheetName().equals(nameSheet)) return;
        for (XSSFSheet sheet: sheets) {
            if (sheet.getSheetName().equals(nameSheet)){
                this.currentSheet = sheet;
                return;
            }
        }
    }

    public File saveOnDisk(String fileName){
        File file = null;
        try {
//            FileOutputStream fileOut = new FileOutputStream(fileName);
//            workbook.write(fileOut);
//            fileOut.close();

            file = TempFile.createTempFile(fileName, ".xlsx");
            try (FileOutputStream os = new FileOutputStream(file)) {
                workbook.write(os);
            }
            workbook.close();
        } catch (Exception e) {
            log.error("Error in file", e.getMessage());
        }

        return file;
    }

    private CellStyle getHeaderCellStyle() {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }

    private CellStyle getBodyCellStyle(){
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setWrapText(true);
        return cellStyle;
    }

    public <T> void fillLikeTable(List<T> data, Class<T> clazz){
        createHeader(clazz, 1);

        if (data != null && !data.isEmpty()){
            int other = 2;
            for (T element:data) {
                createRow(element, clazz, other);
                other ++;
            }
        }

    }

    private <T> void createHeader(Class<T> tClass, int rowNum){
        Row firstRow = this.currentSheet.createRow(rowNum);
        List<Field> annotatedSortedFields = Arrays.stream(tClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ColumnExcel.class))
                .toList();

        // Creating the fist row to fill with the fields name in the order chosen by @ExcelPropertyName
        int cellNum = 0;
        for (Field field : annotatedSortedFields) {
            Cell cell = firstRow.createCell(cellNum++);
            cell.setCellValue(field.getAnnotation(ColumnExcel.class).value());
            cell.setCellStyle(getHeaderCellStyle());
        }
    }

    private <T> void createRow(T element, Class<T> tClass, int rowNum){
        Row row = this.currentSheet.createRow(rowNum);
        List<Field> annotatedSortedFields = Arrays.stream(tClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ColumnExcel.class))
                .toList();
        int cellNum = 0;
        for (Field field : annotatedSortedFields) {
            try {
                field.setAccessible(true);
                Cell cell = row.createCell(cellNum++);
                Object value = field.get(element);
                cell.setCellValue(value != null ? value.toString() : "");
                cell.setCellStyle(getBodyCellStyle());
            } catch (IllegalAccessException e) {
                throw new PnGenericException(ExceptionTypeEnum.DATA_NULL_OR_INVALID,"Invalid access to field in mapping excel column name");
            } finally {
                field.setAccessible(false);
            }
        }
    }

}
