package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.AddressEntity;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;

public class PrepareRequestValidator {

    public RequestDeliveryEntity compareRequestEntity(PrepareRequest prepareRequest, RequestDeliveryEntity requestDeliveryEntity) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.equals(prepareRequest.getRequestId(), requestDeliveryEntity.getRequestId())){
            errors.add("RequestId");
        }
        if (!StringUtils.equals(prepareRequest.getReceiverFiscalCode(), requestDeliveryEntity.getFiscalCode())){
            errors.add("FiscalCode");
        }
        if (!StringUtils.equals(prepareRequest.getProductType(), requestDeliveryEntity.getRegisteredLetterCode())){
            errors.add("ProductType");
        }

        //TO DO
        //attachments

        if (!errors.isEmpty()){
            throw new PnGenericException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, errors);
        }
        else {
            return requestDeliveryEntity;
        }
    }


    public boolean checkAddressInfo(PrepareRequest prepareRequest, AddressEntity addressEntity){

        if(!prepareRequest.getReceiverAddress().getAddress().equals(addressEntity.getAddress()) ||
                !prepareRequest.getReceiverAddress().getFullname().equals(addressEntity.getFullName()) ||
                !prepareRequest.getReceiverAddress().getNameRow2().equals(addressEntity.getNameRow2()) ||
                !prepareRequest.getReceiverAddress().getAddressRow2().equals(addressEntity.getAddressRow2()) ||
                !prepareRequest.getReceiverAddress().getCap().equals(addressEntity.getCap()) ||
                !prepareRequest.getReceiverAddress().getCity().equals(addressEntity.getCity()) ||
                !prepareRequest.getReceiverAddress().getCity2().equals(addressEntity.getCity2()) ||
                !prepareRequest.getReceiverAddress().getPr().equals(addressEntity.getPr()) ||
                !prepareRequest.getReceiverAddress().getCountry().equals(addressEntity.getCountry())){
            return true;
        }
        else{
            return false;
        }
    }
}
