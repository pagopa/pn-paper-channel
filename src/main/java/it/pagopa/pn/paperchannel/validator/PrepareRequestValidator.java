package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;
import static it.pagopa.pn.paperchannel.mapper.AddressMapper.fromAnalogToAddress;

public class PrepareRequestValidator {

    private PrepareRequestValidator(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static void compareRequestEntity(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity){
        List<String> errors = new ArrayList<>();

        if (!prepareRequest.getRequestId().equals(pnDeliveryEntity.getRequestId())) {
            errors.add("RequestId");
        }

        if (!prepareRequest.getIun().equals(pnDeliveryEntity.getIun())) {
            errors.add("Iun");
        }

        if (!prepareRequest.getReceiverFiscalCode().equals(pnDeliveryEntity.getFiscalCode())) {
            errors.add("FiscalCode");
        }

        if (!prepareRequest.getProposalProductType().getValue().equals(pnDeliveryEntity.getProposalProductType())) {
            errors.add("ProductType");
        }

        if (!prepareRequest.getReceiverType().equals(pnDeliveryEntity.getReceiverType())){
            errors.add("ReceiverType");
        }

        if (!prepareRequest.getPrintType().equals(pnDeliveryEntity.getPrintType())){
            errors.add("printType");
        }

        if (!AttachmentValidator.checkBetweenLists(prepareRequest.getAttachmentUrls(), pnDeliveryEntity.getAttachments())){
            errors.add("Attachments");
        }

        if (prepareRequest.getReceiverAddress() != null) {
            if (!fromAnalogToAddress(prepareRequest.getReceiverAddress()).convertToHash().equals(pnDeliveryEntity.getAddressHash())) {
                errors.add("Address");
            }
        } else{
            errors.add("Address");
        }

        if (!errors.isEmpty()){
            throw new PnInputValidatorException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, errors);
        }
    }

}
