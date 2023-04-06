package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.service.SqsSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

class Paper_ARIT extends BaseTest {

    @MockBean
    private SqsSender sqsSender;

    @DirtiesContext
    @Test
    void Test_AR_Delivered__RECRN001A_RECRN001B_RECRN001C(){
        // meta, demat, final (send to delivery push)
        // ...
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_AR_NotDelivered__RECRN002A_RECRN002B_RECRN002C(){
        // meta, demat, final (send to delivery push)
        // ...
        // deliveryFailureCause
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_AR_AbsoluteUntraceability__RECRN002D_RECRN002E_RECRN002F(){
        // meta, demat, final (send to delivery push)
        // ...
        // deliveryFailureCause
        // optional discoveredAddress
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_AR_DeliveredToStorage__RECRN003A_RECRN003B_RECRN003C(){
        // meta, demat, final (send to delivery push)
        // ...
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_AR_RefusedToStorage__RECRN004A_RECRN004B_RECRN004C(){
        // meta, demat, final (send to delivery push)
        // ...
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_AR_CompletedStorage__RECRN005A_RECRN005B_RECRN005C(){
        // meta, demat, final (send to delivery push)
        // ...
        //
        // demat PROGRESS -> send to delivery push
    }

    @DirtiesContext
    @Test
    void Test_AR_TheftLossDeterioration__RECRN006__RetryPC(){
        // retry paper channel
        // ...
    }
}
