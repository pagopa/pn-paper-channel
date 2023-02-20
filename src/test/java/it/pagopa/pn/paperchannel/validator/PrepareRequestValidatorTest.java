package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.utils.Utility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrepareRequestValidatorTest {

    private PnDeliveryRequest deliveryRequest;
    private PrepareRequest prepareRequest;

    @BeforeEach
    void setUp(){
        setDeliveryRequest();
        setPrepareRequest();
    }

    @Test
    void prepareRequestValidatorOKTest() {
        PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true);
        assertTrue(true);
    }

    @Test
    void prepareRequestValidatorInvalidTest() {
        PrepareRequest notValid = new PrepareRequest();
        notValid.setRequestId("PPOOIi--22");
        notValid.setIun("dfdf-fdf4-223332");
        notValid.setAttachmentUrls(new ArrayList<>());
        notValid.setPrintType("BACK");
        notValid.setProposalProductType(ProposalTypeEnum.RS);
        notValid.setReceiverFiscalCode("PSkkll3333");
        notValid.setReceiverType("PG");
        Address address = getAddress();
        address.setCap("1993882");
        address.setCity("Venezia");
        notValid.setReceiverAddress(AddressMapper.toPojo(address));
        List<String> errors = new ArrayList<>();
        PnGenericException ex = Assertions.assertThrows(PnInputValidatorException.class,
                () -> PrepareRequestValidator.compareRequestEntity(notValid, deliveryRequest, true));
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(ExceptionTypeEnum.DIFFERENT_DATA_REQUEST.getMessage(), ex.getMessage());
    }

    private void setDeliveryRequest(){
        deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId("ABC-1234");
        deliveryRequest.setHashedFiscalCode(Utility.convertToHash("MDF1234JJJSSKK"));
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun("LOKKF-343222");
        deliveryRequest.setAddressHash(getAddress().convertToHash());
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setProductType("RN-AR");
        deliveryRequest.setPrintType("FRONTE-RETRO");
    }

    private void setPrepareRequest(){
        prepareRequest = new PrepareRequest();
        List<String> attachmentUrls = new ArrayList<>();
        AnalogAddress analogAddress= AddressMapper.toPojo(getAddress());
        prepareRequest.setRequestId("ABC-1234");
        prepareRequest.setReceiverFiscalCode("MDF1234JJJSSKK");
        prepareRequest.setProposalProductType(ProposalTypeEnum.AR);
        prepareRequest.setReceiverType("PF");
        prepareRequest.setPrintType("FRONTE-RETRO");
        prepareRequest.setIun("LOKKF-343222");
        prepareRequest.setAttachmentUrls(attachmentUrls);
        prepareRequest.setReceiverAddress(analogAddress);
    }

    private Address getAddress(){
        Address address = new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");
        return address;
    }
    
}
