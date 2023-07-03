package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CapDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CapResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CapMapperTest {
    List<PnCap> caps;
    @BeforeEach
    void setUp(){
        PnCap cap = new PnCap();
        cap.setCap("00166");
        PnCap cap1 = new PnCap();
        cap1.setCap("00167");
        caps = List.of(cap,cap1);
    }

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<CapMapper> constructor = CapMapper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        assertNull(exception.getMessage());
    }

    @Test
    void fromEntityTest(){
        PnCap cap = new PnCap();
        CapDto capDto  = CapMapper.fromEntity(cap);
        Assertions.assertNotNull(capDto);
    }

    @Test
    void toResponseTest(){
        CapResponseDto capResponseDtoDto = CapMapper.toResponse(caps);
        Assertions.assertNotNull(capResponseDtoDto);
        Assertions.assertEquals(capResponseDtoDto.getContent().size(), caps.size());
        for (CapDto cap:capResponseDtoDto.getContent()) {
            long i = caps.stream().filter(item -> cap.getCap().equals(item.getCap())).count();
            if (i != 1) Assertions.fail("Il cap" + cap.getCap() + "non è stato mappato.");
        }
    }

}