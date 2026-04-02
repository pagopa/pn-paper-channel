package it.pagopa.pn.paperchannel.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static it.pagopa.pn.paperchannel.utils.Const.PREFIX_REQUEST_ID_SERVICE_DESK;
import static org.junit.jupiter.api.Assertions.*;

class ClientIdHelperTest {

    @ParameterizedTest
    @MethodSource("provideClientIdTestCases")
    void testGetClientId(String requestId, String proposedClientId, String expectedClientId) {
        String result = ClientIdHelper.getClientId(requestId, proposedClientId);
        assertEquals(expectedClientId, result);
    }

    private static Stream<Arguments> provideClientIdTestCases() {
        return Stream.of(
                // requestId, proposedClientId, expectedResult
                Arguments.of(PREFIX_REQUEST_ID_SERVICE_DESK + "123", "clientId", "clientId"),
                Arguments.of(PREFIX_REQUEST_ID_SERVICE_DESK + "456", null, Const.SERVICE_DESK_CLIENT_ID),
                Arguments.of("normalRequestId", null,  Const.DELIVERY_PUSH_CLIENT_ID),
                Arguments.of("normalRequestId", "myClientId", "myClientId"),
                Arguments.of("normalRequestId", "", Const.DELIVERY_PUSH_CLIENT_ID),
                Arguments.of(PREFIX_REQUEST_ID_SERVICE_DESK, "   ", Const.SERVICE_DESK_CLIENT_ID),
                Arguments.of("", null, Const.DELIVERY_PUSH_CLIENT_ID)
        );
    }
}
