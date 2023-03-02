package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorDetails;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorMessageMapperTest {

    PnExcelValidatorException pnExcelValidatorException;


    @BeforeEach
    void setUp (){
        List<PnExcelValidatorException.ErrorCell> errorCells = new ArrayList<>();
        PnExcelValidatorException.ErrorCell cell = new PnExcelValidatorException.ErrorCell(1,1,"colName","ERROR");
        errorCells.add(cell);
        pnExcelValidatorException = new PnExcelValidatorException(ExceptionTypeEnum.EXCEL_BADLY_CONTENT,errorCells);
    }

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<ErrorMessageMapper> constructor = ErrorMessageMapper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        assertNull(exception.getMessage());
    }

    @Test
    void toEntityErrorsNotNullTest(){
        PnErrorMessage errorMessage = ErrorMessageMapper.toEntity(pnExcelValidatorException);
        Assertions.assertNotNull(errorMessage);
        List<PnErrorDetails> pnErrorDetailsList = errorMessage.getErrorDetails();
        for (int i = 0; i < pnErrorDetailsList.size(); i++){
            Assertions.assertEquals(pnErrorDetailsList.get(i).getRow(), pnExcelValidatorException.getErrors().get(i).getRow());
            Assertions.assertEquals(pnErrorDetailsList.get(i).getCol(), pnExcelValidatorException.getErrors().get(i).getCol());
            Assertions.assertEquals(pnErrorDetailsList.get(i).getColName(), pnExcelValidatorException.getErrors().get(i).getColName());
            Assertions.assertEquals(pnErrorDetailsList.get(i).getMessage(), pnExcelValidatorException.getErrors().get(i).getMessage());
        }
    }

}