package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.InfoDownloadDTO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

class FileMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<FileMapper> constructor = FileMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void fileMapperTest () {
        byte[] data = new byte[250];
        InfoDownloadDTO response= FileMapper.toDownloadFile(getPnDeliveryFile(), data);
        Assertions.assertNotNull(response);
    }

    public PnDeliveryFile getPnDeliveryFile(){
        PnDeliveryFile pnDeliveryFile = new PnDeliveryFile();
        pnDeliveryFile.setFilename("Gara-2022");
        pnDeliveryFile.setStatus(InfoDownloadDTO.StatusEnum.UPLOADING.getValue());
        pnDeliveryFile.setUuid("12345");
        pnDeliveryFile.setUrl("www.ciao.it");
        return pnDeliveryFile;
    }
}
