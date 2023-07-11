package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendRequestMapperTest {
    List<PnAddress> addressList;
    PnAddress addressReceiverAddress;
    PnAddress addressSenderAddress;
    PnAddress addressAr;
    PnDeliveryRequest pnDeliveryRequest;
    @BeforeEach
    void setUp(){
        this.initialize();
    }


    @Test
    void exceptionConstructorTest() throws  NoSuchMethodException {
        Constructor<SendRequestMapper> constructor = SendRequestMapper.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        Exception exception = Assertions.assertThrows(Exception.class, () -> constructor.newInstance());
        assertNull(exception.getMessage());
    }

    @Test
    void toDtoTest(){
        SendRequest sendRequest = SendRequestMapper.toDto(addressList, pnDeliveryRequest);
        Assertions.assertNotNull(sendRequest);
        Assertions.assertEquals(sendRequest.getRequestId(), pnDeliveryRequest.getRequestId());
        Assertions.assertEquals(sendRequest.getRequestPaId(), pnDeliveryRequest.getRequestPaId());
        Assertions.assertEquals(sendRequest.getIun(), pnDeliveryRequest.getIun());
        Assertions.assertEquals(sendRequest.getProductType().getValue(), pnDeliveryRequest.getProductType());
        Assertions.assertEquals(sendRequest.getPrintType(), pnDeliveryRequest.getPrintType());
        Assertions.assertEquals(addressReceiverAddress.getTypology(), AddressTypeEnum.RECEIVER_ADDRESS.toString());
        Assertions.assertEquals(addressSenderAddress.getTypology(), AddressTypeEnum.SENDER_ADDRES.toString());
        Assertions.assertEquals(addressAr.getTypology(), AddressTypeEnum.AR_ADDRESS.toString());
    }


    private void initialize(){
        addressReceiverAddress = new PnAddress();
        addressSenderAddress = new PnAddress();
        addressAr = new PnAddress();
        addressList = new ArrayList<>();
        addressReceiverAddress.setRequestId("requestId");
        addressReceiverAddress.setFullName("fullName");
        addressReceiverAddress.setNameRow2("nameRow2");
        addressReceiverAddress.setAddress("address");
        addressReceiverAddress.setAddressRow2("addressRow2");
        addressReceiverAddress.setCap("cap");
        addressReceiverAddress.setCity("city");
        addressReceiverAddress.setCity2("city2");
        addressReceiverAddress.setPr("pr");
        addressReceiverAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.toString());
        addressSenderAddress.setRequestId("requestId");
        addressSenderAddress.setFullName("fullName");
        addressSenderAddress.setNameRow2("nameRow2");
        addressSenderAddress.setAddress("address");
        addressSenderAddress.setAddressRow2("addressRow2");
        addressSenderAddress.setCap("cap");
        addressSenderAddress.setCity("city");
        addressSenderAddress.setCity2("city2");
        addressSenderAddress.setPr("pr");
        addressSenderAddress.setTypology(AddressTypeEnum.SENDER_ADDRES.toString());
        addressAr.setRequestId("requestId");
        addressAr.setFullName("fullName");
        addressAr.setNameRow2("nameRow2");
        addressAr.setAddress("address");
        addressAr.setAddressRow2("addressRow2");
        addressAr.setCap("cap");
        addressAr.setCity("city");
        addressAr.setCity2("city2");
        addressAr.setPr("pr");
        addressAr.setTypology(AddressTypeEnum.AR_ADDRESS.toString());
        addressList.add(addressReceiverAddress);
        addressList.add(addressSenderAddress);
        addressList.add(addressAr);
        pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("requestId");
        pnDeliveryRequest.setFiscalCode("fiscalCode");
        pnDeliveryRequest.setHashedFiscalCode("hashedFiscalCode");
        pnDeliveryRequest.setReceiverType("receiverType");
        pnDeliveryRequest.setIun("iun");
        pnDeliveryRequest.setAddressHash("addressHash");
        pnDeliveryRequest.setCorrelationId("correlationId");
        pnDeliveryRequest.setStatusCode("statusCode");
        pnDeliveryRequest.setStatusDetail("statusDetail");
        pnDeliveryRequest.setStatusDate("statusDate");
        pnDeliveryRequest.setProposalProductType("proposalProductType");
        pnDeliveryRequest.setPrintType("printType");
        pnDeliveryRequest.setStartDate("startDate");
        List<PnAttachmentInfo> pnAttachmentInfos = new ArrayList<>();
        PnAttachmentInfo attachmentInfo = new PnAttachmentInfo();
        attachmentInfo.setId("id");
        attachmentInfo.setDate("date");
        attachmentInfo.setUrl("url");
        attachmentInfo.setFileKey("fileKey");
        attachmentInfo.setChecksum("checksum");
        attachmentInfo.setDocumentType("documentType");
        attachmentInfo.setNumberOfPage(10);
        pnAttachmentInfos.add(attachmentInfo);
        pnDeliveryRequest.setAttachments(pnAttachmentInfos);
        pnDeliveryRequest.setProductType(ProductTypeEnum.RIR.getValue());
        pnDeliveryRequest.setHashOldAddress("hashOldAddress");
        pnDeliveryRequest.setRequestPaId("requestPaId");
    }
}