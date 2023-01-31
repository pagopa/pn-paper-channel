package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperTender;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableTenderResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class TenderMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<TenderMapper> constructor = TenderMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void deliveryDriverToPageableResponseTest() {
        Pageable pageable = Mockito.mock(Pageable.class, Mockito.CALLS_REAL_METHODS);
        List<PnPaperTender> list= new ArrayList<>();
        PageableTenderResponseDto response= TenderMapper.toPageableResponse(TenderMapper.toPagination(pageable, list));
        Assertions.assertNotNull(response);
    }

    @Test
    void deliveryDriverTenderToDtoTest() {
        PnPaperTender pnPaperTender = new PnPaperTender();
        TenderDTO response= TenderMapper.tenderToDto(pnPaperTender);
        Assertions.assertNotNull(response);
    }
}
