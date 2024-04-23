package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProposalTypeEnum;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.utils.Utility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;

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
       assertThatNoException().isThrownBy(() ->
               PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, true));
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
                () -> PrepareRequestValidator.compareRequestEntity(notValid, deliveryRequest, true, true));
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(ExceptionTypeEnum.DIFFERENT_DATA_REQUEST.getMessage(), ex.getMessage());
    }

    @Test
    void prepareRequestValidatorAttachmentTest_1() {
        List<String> errors = new ArrayList<>();

        prepareRequest.setAttachmentUrls(new ArrayList<>(List.of("safestorage://123")));

        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setFileKey("safestorage://123");
        deliveryRequest.setAttachments(new ArrayList<>(List.of(pnAttachmentInfo)));
        deliveryRequest.setRemovedAttachments(null);

        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, true));
        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, false));


        pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setFileKey("safestorage://456");
        deliveryRequest.setAttachments(new ArrayList<>(List.of(pnAttachmentInfo)));

        PnGenericException ex = Assertions.assertThrows(PnInputValidatorException.class,
                () -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, true));
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(ExceptionTypeEnum.DIFFERENT_DATA_REQUEST.getMessage(), ex.getMessage());

        ex = Assertions.assertThrows(PnInputValidatorException.class,
                () -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, false));
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(ExceptionTypeEnum.DIFFERENT_DATA_REQUEST.getMessage(), ex.getMessage());
    }

    @Test
    void prepareRequestValidatorAttachmentTest_2() {
        List<String> errors = new ArrayList<>();

        // caso in cui l'f24set della request non contiene f24set. skip a false deve dare errore
        prepareRequest.setAttachmentUrls(new ArrayList<>(List.of("safestorage://123", "f24set://abcd")));

        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setFileKey("safestorage://123");


        deliveryRequest.setAttachments(new ArrayList<>(List.of(pnAttachmentInfo)));
        deliveryRequest.setRemovedAttachments(null);


        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, true));
        PnGenericException ex = Assertions.assertThrows(PnInputValidatorException.class,
                () -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, false));
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(ExceptionTypeEnum.DIFFERENT_DATA_REQUEST.getMessage(), ex.getMessage());

        // caso in cui l'f24set della request è uguale da quello generato. tipico caso di check allegati nel caso di ritentativi. non deve dare errori
        PnAttachmentInfo pnAttachmentInfo1 = new PnAttachmentInfo();
        pnAttachmentInfo1.setFileKey("f24set://abcd");

        deliveryRequest.setAttachments(new ArrayList<>(List.of(pnAttachmentInfo, pnAttachmentInfo1)));
        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, true));
        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, false));

        // caso in cui l'f24set della request è diverso da quello generato. tipico caso di check allegati della seconda request sulla prima. skip a false deve dare errore
        pnAttachmentInfo1 = new PnAttachmentInfo();
        pnAttachmentInfo1.setFileKey("f24set://abcd_altro");

        deliveryRequest.setAttachments(new ArrayList<>(List.of(pnAttachmentInfo, pnAttachmentInfo1)));
        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, true));
        ex = Assertions.assertThrows(PnInputValidatorException.class,
                () -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, false));
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(ExceptionTypeEnum.DIFFERENT_DATA_REQUEST.getMessage(), ex.getMessage());
    }

    @Test
    void prepareRequestValidatorAttachmentTest_3() {
        List<String> errors = new ArrayList<>();


        prepareRequest.setAttachmentUrls(new ArrayList<>(List.of("safestorage://123", "f24set://abcd")));

        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setFileKey("safestorage://123");

        PnAttachmentInfo pnAttachmentInfo1 = new PnAttachmentInfo();
        pnAttachmentInfo1.setFileKey("safestorage://456");
        pnAttachmentInfo1.setGeneratedFrom("f24set://abcd");

        deliveryRequest.setAttachments(new ArrayList<>(List.of(pnAttachmentInfo,pnAttachmentInfo1)));
        deliveryRequest.setRemovedAttachments(null);


        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, true));
        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, false));


        pnAttachmentInfo1 = new PnAttachmentInfo();
        pnAttachmentInfo1.setFileKey("safestorage://456");
        pnAttachmentInfo1.setGeneratedFrom("f24set://abcd_altro");

        deliveryRequest.setAttachments(new ArrayList<>(List.of(pnAttachmentInfo,pnAttachmentInfo1)));


        Assertions.assertDoesNotThrow(() -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, true));
        PnGenericException ex = Assertions.assertThrows(PnInputValidatorException.class,
                () -> PrepareRequestValidator.compareRequestEntity(prepareRequest, deliveryRequest, true, false));
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(ExceptionTypeEnum.DIFFERENT_DATA_REQUEST.getMessage(), ex.getMessage());

    }

    private void setDeliveryRequest(){
        var attachmentInfo = new PnAttachmentInfo();
        attachmentInfo.setFileKey("safestorage://aar.pdf");
        attachmentInfo.setDate(Instant.now().toString());
        attachmentInfo.setDocumentType("AAR");
        attachmentInfo.setUrl("safestorage://aar.pdf");
        var attachmentInfoRemoved = new PnAttachmentInfo();
        attachmentInfoRemoved.setFileKey("safestorage://document.pdf");
        attachmentInfoRemoved.setDate(Instant.now().toString());
        attachmentInfoRemoved.setDocumentType("AAR");
        attachmentInfoRemoved.setUrl("safestorage://document.pdf");
        deliveryRequest = new PnDeliveryRequest();
        deliveryRequest.setRequestId("ABC-1234");
        deliveryRequest.setHashedFiscalCode(Utility.convertToHash("MDF1234JJJSSKK"));
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun("LOKKF-343222");
        deliveryRequest.setAddressHash(getAddress().convertToHash());
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setProductType("RN-AR");
        deliveryRequest.setPrintType("FRONTE-RETRO");
        deliveryRequest.setAttachments(List.of(
                attachmentInfo
        ));
        deliveryRequest.setRemovedAttachments(List.of(
                attachmentInfoRemoved
        ));
    }

    private void setPrepareRequest(){
        prepareRequest = new PrepareRequest();
        List<String> attachmentUrls = List.of(
                "safestorage://aar.pdf",
                "safestorage://document.pdf"
        );
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
