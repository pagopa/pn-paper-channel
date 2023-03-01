package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class PaperRequestErrorDAOTest extends BaseTest {

    @Autowired
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @Test
    void createRequestErrorTest(){
        String requestId = "id-1234";
        String error = "errore test";
        String classType = "java";
        PnRequestError requestError = this.paperRequestErrorDAO.created(requestId, error, classType).block();
        assertNotNull(requestError);

    }
}
