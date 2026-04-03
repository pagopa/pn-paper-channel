package it.pagopa.pn.paperchannel.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import static it.pagopa.pn.paperchannel.utils.Const.*;

@Slf4j
public class ClientIdHelper {

    private ClientIdHelper() {}

    public static String getClientId(String requestId, String proposedClientId) {
        log.info("Getting clientId for requestId: {} with proposedClientId: {}", requestId, proposedClientId);

        if (StringUtils.hasText(proposedClientId)) {
            return proposedClientId;
        } else if (requestId.startsWith(PREFIX_REQUEST_ID_SERVICE_DESK)) {
            return SERVICE_DESK_CLIENT_ID;
        }

        return DELIVERY_PUSH_CLIENT_ID;
    }
}
