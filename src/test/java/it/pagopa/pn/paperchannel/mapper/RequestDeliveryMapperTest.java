package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class RequestDeliveryMapperTest {

    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<RequestDeliveryMapper> constructor = RequestDeliveryMapper.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        Assertions.assertEquals(null, exception.getMessage());
    }

    @Test
    void requestDeliveryMapperTest () {
        PnDeliveryRequest response= RequestDeliveryMapper.toEntity(getPrepareRequest());
        Assertions.assertNotNull(response);
    }

    private PrepareRequest getPrepareRequest() {
        PrepareRequest prepareRequest = new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress= new AnalogAddress();
        String s ="url12345";
        attachmentUrls.add(s);

        analogAddress.setAddress("via roma");
        analogAddress.setAddressRow2("via lazio");
        analogAddress.setCap("00061");
        analogAddress.setCity("roma");
        analogAddress.setCity2("viterbo");
        analogAddress.setCountry("italia");
        analogAddress.setPr("PR");
        analogAddress.setFullname("Ettore Fieramosca");

        prepareRequest.setRequestId("12345ABC");
        prepareRequest.setAttachmentUrls(attachmentUrls);
        prepareRequest.setDiscoveredAddress(analogAddress);
        prepareRequest.setIun("iun");
        prepareRequest.setReceiverAddress(analogAddress);
        prepareRequest.setPrintType("BN_FRONTE_RETRO");
        prepareRequest.setRelatedRequestId(null);
        prepareRequest.setProposalProductType(ProposalTypeEnum.AR);
        prepareRequest.setReceiverFiscalCode("FRMTTR76M06B715E");
        prepareRequest.setReceiverType("PF");
        return prepareRequest;
    }
}
