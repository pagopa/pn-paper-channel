package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PresignedUrlResponseDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;

class PresignedUrlResponseMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<PresignedUrlResponseMapper> constructor = PresignedUrlResponseMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void presignedUrlResponseMapperFromResultTest () throws MalformedURLException {
        URL url = new URL("http://example.com/");
        PresignedUrlResponseDto response= PresignedUrlResponseMapper.fromResult(url,"12345");
        Assertions.assertNotNull(response);
    }

    @Test
    void presignedUrlResponseMapperToEntityTest () {
        PnDeliveryFile response= PresignedUrlResponseMapper.toEntity(getPresignedUrlResponseDto());
        Assertions.assertNotNull(response);
    }

    public PresignedUrlResponseDto getPresignedUrlResponseDto(){
        PresignedUrlResponseDto dto= new PresignedUrlResponseDto();
        dto.setPresignedUrl("www.ciao.it");
        dto.setUuid("12345");
        return dto;
    }
}
