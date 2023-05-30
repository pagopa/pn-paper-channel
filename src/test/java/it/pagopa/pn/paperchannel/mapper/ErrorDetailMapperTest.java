package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.exception.PnExcelValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnErrorDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorDetailMapperTest {

    PnErrorDetails errorDetails;
    List<PnErrorDetails> pnErrorDetailsList;
    PnExcelValidatorException.ErrorCell errorCell;
    @BeforeEach
    void setUp(){
        errorCell = new PnExcelValidatorException.ErrorCell(1,1,"colName","ERROR");
        errorDetails = new PnErrorDetails();
        errorDetails.setRow(2);
        errorDetails.setCol(1);
        errorDetails.setMessage("abcde");
        errorDetails.setColName("cap");
        pnErrorDetailsList = List.of(errorDetails);
    }

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<ErrorDetailMapper> constructor = ErrorDetailMapper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        assertNull(exception.getMessage());
    }

    @Test
    void toEntityTest(){
        PnErrorDetails errorDetails1 = ErrorDetailMapper.toEntity(errorCell);
        Assertions.assertNotNull(errorDetails1);
    }

    @Test
    void toDtoTest(){
        PnExcelValidatorException.ErrorCell exception = ErrorDetailMapper.toDto(errorDetails);
        Assertions.assertNotNull(exception);
    }

    @Test
    void toDtosTest(){
        List<PnExcelValidatorException.ErrorCell> pnExceptionList = ErrorDetailMapper.toDtos(pnErrorDetailsList);
        Assertions.assertNotNull(pnExceptionList);
        Assertions.assertEquals(pnExceptionList.size(), pnErrorDetailsList.size());
        for (int i = 0; i < pnExceptionList.size(); i++){
            Assertions.assertEquals(pnExceptionList.get(i).getRow(), pnErrorDetailsList.get(i).getRow());
            Assertions.assertEquals(pnExceptionList.get(i).getCol(), pnErrorDetailsList.get(i).getCol());
            Assertions.assertEquals(pnExceptionList.get(i).getColName(), pnErrorDetailsList.get(i).getColName());
            Assertions.assertEquals(pnExceptionList.get(i).getMessage(), pnErrorDetailsList.get(i).getMessage());
        }
    }

}