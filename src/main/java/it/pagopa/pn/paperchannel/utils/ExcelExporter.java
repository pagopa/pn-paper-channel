package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
public class ExcelExporter {

    public static <T> void exportToExcel(List<T> data, String fileName) throws PnGenericException {
        // Create a Workbook
        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            // Create a Sheet
            XSSFSheet sheet = workbook.createSheet("Data");


            // Configuring the style for the excel file's headers (first row)
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont font = workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) 10);
            font.setBold(true);
            headerStyle.setFont(font);


//            // Auto size all the columns
//            for (int x = 0; x < sheet.getRow(0).getPhysicalNumberOfCells(); x++) {
//                sheet.autoSizeColumn(x);
//            }

            // Configuring the style for all the remaining the excel file's cells
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setWrapText(true);

            populateExcel(data, sheet, headerStyle, cellStyle);

            // set autofilter on headers
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, sheet.getRow(0).getPhysicalNumberOfCells() - 1));

            // Write the Workbook to a file
            FileOutputStream fileOut = new FileOutputStream(fileName);
            workbook.write(fileOut);

            fileOut.close();
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static <T> void populateExcel(List<T> data, XSSFSheet sheet, CellStyle headerStyle, CellStyle cellStyle) throws PnGenericException {
        Row headerRow = sheet.createRow(0);
        int rowNum = 0;
        // Create the first row with field names
        Row firstRow = sheet.createRow(rowNum++);
        if (Objects.nonNull(data.get(0))) {
            // Getting the fields annotated and sorted by the order value of @ExcelPropertyName(value = "Insert timestamp", order = 1)
            Class<?> clazz = data.get(0).getClass();
            List<Field> annotatedSortedFields = Arrays.stream(clazz.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(ExcelPropertyName.class))
                    .sorted(Comparator.comparingInt(f -> f.getAnnotation(ExcelPropertyName.class).order()))
                    .collect(Collectors.toList());

            // Creating the fist row to fill with the fields name in the order chosen by @ExcelPropertyName
            int cellNum = 0;
            for (Field field : annotatedSortedFields) {
                Cell cell = firstRow.createCell(cellNum++);
                cell.setCellValue(field.getAnnotation(ExcelPropertyName.class).value());
                cell.setCellStyle(headerStyle);
            }

            // populating the data of the excel
            for (T obj : data) {
                // Create a new row for each object
                Row row = sheet.createRow(rowNum++);
                cellNum = 0;
                for (Field field : annotatedSortedFields) {
                    field.setAccessible(true);
                    try {
                        // Add the value of the field to the corresponding cell
                        Cell cell = row.createCell(cellNum++);
                        Object value = field.get(obj);
                        cell.setCellValue(value != null ? value.toString() : "");
                        cell.setCellStyle(cellStyle);
                    } catch (IllegalAccessException e) {
                        throw new PnGenericException(ExceptionTypeEnum.DATA_NULL_OR_INVALID,"Invalid access to field in mapping excel column name");
                    } finally {
                        field.setAccessible(false);
                    }
                }
            }
        } else {
            throw new PnGenericException(ExceptionTypeEnum.DATA_NULL_OR_INVALID,"Records not found to map the excel file");
        }

    }
}