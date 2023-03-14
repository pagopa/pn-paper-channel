package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableTenderResponseDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderCreateRequestDTO;
import it.pagopa.pn.paperchannel.rest.v1.dto.TenderDTO;
import org.joda.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class TenderMapperTest {

    PnTender tender;
    @BeforeEach
    void setUp(){
        this.initialize();
    }
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
        List<PnTender> list= new ArrayList<>();
        PageableTenderResponseDto response= TenderMapper.toPageableResponse(TenderMapper.toPagination(pageable, list));
        Assertions.assertNotNull(response);
    }

    @Test
    void toTenderRequestTest(){
        TenderCreateRequestDTO requestDTO = new TenderCreateRequestDTO();
        requestDTO.setCode("code");
        requestDTO.setName("name");
        requestDTO.setStartDate(new Date());
        requestDTO.setEndDate(new Date());
        PnTender pnTender = TenderMapper.toTenderRequest(requestDTO);
        Assertions.assertNotNull(pnTender);
    }

    @Test
    void toTenderRequestNullCodeTest(){
        TenderCreateRequestDTO requestDTO = new TenderCreateRequestDTO();
        requestDTO.setCode(null);
        requestDTO.setName("name");
        requestDTO.setStartDate(new Date());
        requestDTO.setEndDate(new Date());
        PnTender pnTender = TenderMapper.toTenderRequest(requestDTO);
        Assertions.assertNotNull(pnTender);
    }

    @Test
    void tenderToDtoTest(){
        TenderDTO tenderDTO = TenderMapper.tenderToDto(tender);
        Assertions.assertNotNull(tenderDTO);
    }

    void initialize(){
        tender = new PnTender();
        tender.setTenderCode("tenderCode");
        tender.setDescription("description");
        tender.setAuthor("author");
        tender.setStatus(TenderDTO.StatusEnum.CREATED.getValue());
        tender.setDate(new Date().toInstant());
        tender.setStartDate(new Date().toInstant());
        tender.setEndDate(new Date().toInstant());
    }
}
