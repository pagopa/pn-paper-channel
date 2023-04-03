package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class Paper_ARIT extends BaseTest {
    @Autowired
    private EventMetaDAO eventMetaDAO;

    @Autowired
    private EventDematDAO eventDematDAO;

    @MockBean
    private SqsSender sqsSender;

    //@BeforeAll

}
