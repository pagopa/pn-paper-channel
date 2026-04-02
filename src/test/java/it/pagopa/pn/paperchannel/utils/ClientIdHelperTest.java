package it.pagopa.pn.paperchannel.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientIdHelperTest {

    @Test
    void testGetClientId_ServiceDeskPrefix() {
        String requestId = Const.PREFIX_REQUEST_ID_SERVICE_DESK + "12345";
        String proposedClientId = "someClientId";
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(Const.SERVICE_DESK_CLIENT_ID, result);
    }

    @Test
    void testGetClientId_NullProposedClientId() {
        String requestId = "someRequestId";
        String proposedClientId = null;
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(Const.CLIENT_ID_DELIVERY_PUSH, result);
    }

    @Test
    void testGetClientId_ValidProposedClientId() {
        String requestId = "someRequestId";
        String proposedClientId = "customClientId";
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals("customClientId", result);
    }

    @Test
    void testGetClientId_ServiceDeskPrefixAndNullProposedClientId() {
        String requestId = Const.PREFIX_REQUEST_ID_SERVICE_DESK + "67890";
        String proposedClientId = null;
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(Const.SERVICE_DESK_CLIENT_ID, result);
    }
}

