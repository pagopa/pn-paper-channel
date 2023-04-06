package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventMetaDAO;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

class Paper_RSIT extends BaseTest {

    @MockBean
    private SqsSender sqsSender;

    @DirtiesContext
    @Test
    void Test_RS_Delivered__RECRS001C(){
        // final only -> send to delivery push
        // ...
    }

    @DirtiesContext
    @Test
    void Test_RS_NotDelivered__RECRS002A_RECRS002B_RECRS002C(){
        // meta, demat, final (send to delivery push)
        // ...
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_RS_AbsoluteUntraceability__RECRS002D_RECRS002E_RECRS002F(){
        // meta, demat, final (send to delivery push)
        // ...
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_RS_DeliveredToStorage__RECRS003C(){
        // final only -> send to delivery push
        // ...
    }

    @DirtiesContext
    @Test
    void Test_RS_RefusedToStorage__RECRS004A_RECRS004B_RECRS004C(){
        // meta, demat, final (send to delivery push)
        // ...
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_RS_CompletedStorage__RECRS005A_RECRS005B_RECRS005C(){
        // meta, demat, final (send to delivery push)
        // ...
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_RS_TheftLossDeterioration__RECRS006__RetryPC(){
        // retry paper channel
        // ...
    }
}
