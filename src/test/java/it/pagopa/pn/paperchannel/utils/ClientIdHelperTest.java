package it.pagopa.pn.paperchannel.utils;

import org.junit.jupiter.api.Test;
import static it.pagopa.pn.paperchannel.utils.Const.PREFIX_REQUEST_ID_SERVICE_DESK;
import static org.junit.jupiter.api.Assertions.*;

class ClientIdHelperTest {

    @Test
    void testServiceDeskPrefixWithProposedClientId() {
        String requestId = PREFIX_REQUEST_ID_SERVICE_DESK + "123";
        String proposedClientId = "clientId";
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(proposedClientId, result);
    }

    @Test
    void testServiceDeskPrefixWithNullProposedClientId() {
        String requestId = PREFIX_REQUEST_ID_SERVICE_DESK + "456";
        String proposedClientId = null;
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(Const.SERVICE_DESK_CLIENT_ID, result);
    }

    @Test
    void testNullProposedClientId() {
        String requestId = "normalRequestId";
        String proposedClientId = null;
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(Const.DELIVERY_PUSH_CLIENT_ID, result);
    }

    @Test
    void testValidProposedClientId() {
        String requestId = "normalRequestId";
        String proposedClientId = "myClientId";
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals("myClientId", result);
    }

    @Test
    void testEmptyProposedClientId() {
        String requestId = "normalRequestId";
        String proposedClientId = "";
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(Const.DELIVERY_PUSH_CLIENT_ID, result);
    }

    @Test
    void testSpacesProposedClientId() {
        String proposedClientId = "   ";
        String result = ClientIdHelper.getClientId(PREFIX_REQUEST_ID_SERVICE_DESK, proposedClientId);
        assertEquals(Const.SERVICE_DESK_CLIENT_ID, result);
    }

    @Test
    void testEmptyRequestIdAndNullProposedClientId() {
        String requestId = "";
        String proposedClientId = null;
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(Const.DELIVERY_PUSH_CLIENT_ID, result);
    }
}
