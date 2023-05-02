package it.pagopa.pn.paperchannel.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PnExcelValidatorExceptionErrorCellTest {
    private Integer row;
    private Integer col;
    private String colName;
    private String message;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void setGetTest() {
        PnExcelValidatorException.ErrorCell errorCell = initErrorCell();
        Assertions.assertNotNull(errorCell);
        Assertions.assertEquals(row, errorCell.getRow());
        Assertions.assertEquals(col, errorCell.getCol());
        Assertions.assertEquals(colName, errorCell.getColName());
        Assertions.assertEquals(message, errorCell.getMessage());

        Integer row = 1;
        Integer col = 2;
        String colName = "TAX_ID";
        String message = "Formato TAX_ID errato";

        errorCell.setRow(row);
        errorCell.setCol(col);
        errorCell.setColName(colName);
        errorCell.setMessage(message);

        Assertions.assertEquals(row, errorCell.getRow());
        Assertions.assertEquals(col, errorCell.getCol());
        Assertions.assertEquals(colName, errorCell.getColName());
        Assertions.assertEquals(message, errorCell.getMessage());
    }

    private PnExcelValidatorException.ErrorCell initErrorCell() {
        return new PnExcelValidatorException.ErrorCell(row, col, colName, message);
    }

    private void initialize() {
        row = 0;
        col = 0;
        colName = "CAP";
        message = "Formato CAP non valido";
    }
}
