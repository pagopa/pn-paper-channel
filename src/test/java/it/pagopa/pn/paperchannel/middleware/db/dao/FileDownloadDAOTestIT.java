package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class FileDownloadDAOTestIT extends BaseTest {

    @Autowired
    private FileDownloadDAO fileDownloadDAO;
    private final PnDeliveryFile pnDeliveryFile = new PnDeliveryFile();

    @BeforeEach
    public void setUp(){
        initialize();
    }
    @Test
    void getUuidTest (){
        PnDeliveryFile deliveryFile = this.fileDownloadDAO.getUuid(pnDeliveryFile.getUuid()).block();
        assertNotNull(deliveryFile);
        assertEquals(deliveryFile.getUuid(), pnDeliveryFile.getUuid());
        assertEquals(deliveryFile.getStatus(), pnDeliveryFile.getStatus());
        assertEquals(deliveryFile.getUrl(), pnDeliveryFile.getUrl());

    }

    private void initialize(){

        pnDeliveryFile.setUuid("12345");
        pnDeliveryFile.setStatus("UPLOADING");
        pnDeliveryFile.setUrl("www.abcd.it");
        pnDeliveryFile.setFilename("document");
        this.fileDownloadDAO.create(pnDeliveryFile);

    }
}
