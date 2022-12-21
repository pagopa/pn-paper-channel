package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;
import static it.pagopa.pn.paperchannel.mapper.AddressMapper.fromAnalogToAddress;

public class SendRequestValidator {
    private SendRequestValidator() {
        throw new IllegalCallerException();
    }

    public static void compareRequestEntity(SendRequest sendRequest, PnDeliveryRequest pnDeliveryEntity){
        List<String> errors = new ArrayList<>();

        if (!sendRequest.getRequestId().equals(pnDeliveryEntity.getRequestId())) {
            errors.add("RequestId");
        }

        if (!sendRequest.getReceiverFiscalCode().equals(pnDeliveryEntity.getFiscalCode())) {
            errors.add("FiscalCode");
        }

        if (!sendRequest.getProductType().getValue().equals(pnDeliveryEntity.getFinalLetterCode())) {
            errors.add("ProductType");
        }

        if (!sendRequest.getReceiverType().equals(pnDeliveryEntity.getReceiverType())){
            errors.add("ReceiverType");
        }

        if (!sendRequest.getPrintType().equals(pnDeliveryEntity.getPrintType())){
            errors.add("printType");
        }

        if (!AttachmentValidator.checkBetweenLists(sendRequest.getAttachmentUrls(), pnDeliveryEntity.getAttachments())){
            errors.add("Attachments");
        }

        if (sendRequest.getReceiverAddress() != null) {
            if (!fromAnalogToAddress(sendRequest.getReceiverAddress()).convertToHash().equals(pnDeliveryEntity.getAddressHash())) {
                errors.add("Address");
            }
        }
        else{
            errors.add("Address");
        }

        if (!errors.isEmpty()){
            throw new PnInputValidatorException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, errors);
        }
    }
}
