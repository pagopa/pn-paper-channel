package it.pagopa.pn.paperchannel.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import static it.pagopa.pn.paperchannel.utils.Const.*;

@Slf4j
@AllArgsConstructor
public class ClientIdHelper {

    public static String getClientId(String requestId, String proposedClientId) {
        String clientId = proposedClientId;

        if (requestId.startsWith(PREFIX_REQUEST_ID_SERVICE_DESK)){
            clientId = SERVICE_DESK_CLIENT_ID;
        }

        if(!StringUtils.hasText(clientId)) {
            clientId = CLIENT_ID_DELIVERY_PUSH;
        }

        log.info("ClientId resolved: {}", clientId);
        return clientId;
    }
}
