package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaperRequestErrorDAOTestIT extends BaseTest {

    @Autowired
    private PaperRequestErrorDAO paperRequestErrorDAO;

    @Test
    void createRequestErrorTest(){

        // Given
        PnRequestError pnRequestError = PnRequestError.builder()
                .requestId("id-1234")
                .error("errore test")
                .flowThrow("java")
                .build();

        // When
        PnRequestError requestError = this.paperRequestErrorDAO.created(pnRequestError).block();

        // Then
        assertNotNull(requestError);
    }

    @Test
    void findAllRequestErrorTestOK(){
        List<PnRequestError> requestErrorList= this.paperRequestErrorDAO.findAll().block();
        assertNotNull(requestErrorList);

    }

}
