package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.model.PrepareRequestInt;
import it.pagopa.pn.paperchannel.model.CommunicationTypeEnum;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrepareRequestMapperTest {
    private final PrepareRequestMapper mapper = Mappers.getMapper(PrepareRequestMapper.class);

    @Test
    void testPrepareRequestToInternal() {
        PrepareRequest request = new PrepareRequest();
        request.setRequestId("REQ123");
        request.setIun("IUN123");
        request.setReceiverFiscalCode("CF123");
        request.setReceiverType("PF");
        request.setPrintType("BN_FRONTE");
        request.setProposalProductType(ProposalTypeEnum.AR);
        request.setAttachmentUrls(List.of("url1", "url2"));
        request.setRelatedRequestId("REL123");
        request.setSenderPaId("PAID");
        request.setNotificationSentAt(Instant.parse("2024-01-01T10:00:00Z"));
        request.setAarWithRadd(Boolean.TRUE);
        AnalogAddress address = new AnalogAddress();
        address.setAddress("Via Roma");
        request.setReceiverAddress(address);
        request.setDiscoveredAddress(address);

        String clientId = "CLIENT1";
        PrepareRequestInt result = mapper.prepareRequestToInternal(request, clientId);

        assertEquals(CommunicationTypeEnum.LEGAL.name(), result.getCommunicationType());
        assertEquals(clientId, result.getClientId());
        assertEquals(request.getRequestId(), result.getRequestId());
        assertEquals(request.getIun(), result.getIun());
        assertEquals(request.getReceiverFiscalCode(), result.getReceiverFiscalCode());
        assertEquals(request.getReceiverType(), result.getReceiverType());
        assertEquals(request.getPrintType(), result.getPrintType());
        assertEquals(request.getProposalProductType(), result.getProposalProductType());
        assertEquals(request.getAttachmentUrls(), result.getAttachmentUrls());
        assertEquals(request.getRelatedRequestId(), result.getRelatedRequestId());
        assertEquals(request.getSenderPaId(), result.getSenderPaId());
        assertEquals(request.getNotificationSentAt(), result.getNotificationSentAt());
        assertEquals(request.getReceiverAddress(), result.getReceiverAddress());
        assertEquals(request.getDiscoveredAddress(), result.getDiscoveredAddress());
        assertEquals(request.getAarWithRadd(), result.getAarWithRadd());
    }

    @Test
    void testPrepareRequestToInternal_NullInput() {
        PrepareRequestInt result = mapper.prepareRequestToInternal(null, null);
        assertNull(result, "Should return null if both request and clientId are null");
    }
}
