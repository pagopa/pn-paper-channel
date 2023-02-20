package it.pagopa.pn.paperchannel.dao.common;

import it.pagopa.pn.paperchannel.dao.DAOException;
import it.pagopa.pn.paperchannel.dao.model.ColumnExcel;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Getter
public class ExcelEngine {
    private final XSSFWorkbook workbook;
    private final List<XSSFSheet> sheets;
    private XSSFSheet currentSheet;
    private String filename;

    public ExcelEngine(String filename){
        this.filename = filename;
        this.workbook = new XSSFWorkbook();
        this.currentSheet = workbook.createSheet("Sheet 1");
        this.sheets = new ArrayList<>();
        this.sheets.add(this.currentSheet);
    }

    public ExcelEngine(InputStream stream) throws IOException {
        this.workbook = new XSSFWorkbook(stream);
        int numberOfSheets = this.workbook.getNumberOfSheets();
        this.sheets = new ArrayList<>();
        if (numberOfSheets == 0){
            this.currentSheet = workbook.createSheet("Sheet 1");
            this.sheets.add(this.currentSheet);
        } else {
            for(int i=0; i < numberOfSheets; i++){
                this.sheets.add(workbook.getSheetAt(i));
            }
            this.currentSheet = this.sheets.get(0);
        }
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

    public File saveOnDisk() {
        File file = null;
        try {
            file = new File(filename.concat(".xlsx"));
            if (file.createNewFile()) {
                try (FileOutputStream os = new FileOutputStream(file)) {
                    workbook.write(os);
                }
            }

        } catch (Exception e) {
            log.error("Error in file {}", e.getMessage());
            throw new DAOException("Error with save on disk file");
        } finally {
            try {
                workbook.close();
            } catch (IOException ioException) {
                log.error("Error in file {}", ioException.getMessage());
            }
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
        createHeader(clazz, 0);

        if (data != null && !data.isEmpty()){
            int other = 1;
            for (T element:data) {
                createRow(element, clazz, other);
                other ++;
            }
        }
    }

    public <T> List<T> extractDataLikeTable(Function<Map<String, ExcelCell>, T> map, Class<T> tClass) throws DAOException {
        List<T> rows = new ArrayList<>();
        int lastRow = this.currentSheet.getLastRowNum();
        int firstRow = this.currentSheet.getFirstRowNum(); //-1 se non ci sono dati
        if (firstRow == -1 || lastRow == -1) return rows;
        if (firstRow > lastRow) throw new DAOException("Excel malformed");
        if ((lastRow-firstRow) <= 0) return rows;
        int i = firstRow+1;
        Map<String, Integer> headerCell = getHeaderCellPosition(this.currentSheet.getRow(firstRow), tClass);
        if (headerCell.isEmpty()) throw new DAOException("Header mismatch with annotation value");

        while (i <= lastRow){
            Map<String, ExcelCell> mapRow = new HashMap<>();
            Row row = this.currentSheet.getRow(i);
            headerCell.keySet().forEach(key -> {
                if (row.getCell(headerCell.get(key)).getCellType() == CellType.NUMERIC) {
                    mapRow.put(key, new ExcelCell(row.getRowNum()+1, headerCell.get(key)+1, String.valueOf(row.getCell(headerCell.get(key)).getNumericCellValue())));
                } else {
                    mapRow.put(key, new ExcelCell(row.getRowNum()+1, headerCell.get(key)+1, row.getCell(headerCell.get(key)).getStringCellValue()));
                }
            }
            );
            rows.add(map.apply(mapRow));
            i++;
        }
        return rows;
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

                if (value instanceof List<?>) {
                    String cellValue = StringUtils.join(value, ',');
                    log.info(cellValue);
                    cellValue = cellValue.replace("[", "");
                    cellValue = cellValue.replace("]", "");
                    log.info(cellValue);
                    cell.setCellValue(cellValue);
                } else {
                    cell.setCellValue(value != null ? value.toString() : "");
                }
                
                cell.setCellStyle(getBodyCellStyle());
            } catch (IllegalAccessException e) {
                throw new PnGenericException(ExceptionTypeEnum.DATA_NULL_OR_INVALID,"Invalid access to field in mapping excel column name");
            } finally {
                field.setAccessible(false);
            }
        }
    }

    private <T> Map<String, Integer> getHeaderCellPosition(Row header, Class<T> tClass){
        List<Field> annotatedSortedFields = Arrays.stream(tClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ColumnExcel.class))
                .toList();
        int firstCell = header.getFirstCellNum();
        int lastCell = header.getLastCellNum();
        if (firstCell == -1 || lastCell == -1) return Collections.emptyMap();
        if (firstCell > lastCell) throw new DAOException("Excel malformed");
        if ((lastCell-firstCell) <= 1) return Collections.emptyMap();
        Map<String, Integer> headerPosition = new HashMap<>();
        int i = firstCell;
        while (i <= lastCell-1){
            Cell cell = header.getCell(i);

            List<Field> filters = annotatedSortedFields.stream()
                    .filter(field -> field.getAnnotation(ColumnExcel.class).value().equals(cell.getStringCellValue()))
                    .toList();
            if (!filters.isEmpty()){
                headerPosition.put(filters.get(0).getAnnotation(ColumnExcel.class).value(), i);
            }
            i++;
        }
        if (headerPosition.size() != annotatedSortedFields.size()) throw new DAOException("The header have badly format");
        return headerPosition;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    public static class ExcelCell {
        private Integer row;
        private Integer col;
        private String value;
    }

}
