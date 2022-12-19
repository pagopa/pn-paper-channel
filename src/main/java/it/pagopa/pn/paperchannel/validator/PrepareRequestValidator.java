package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;
@Component
public class PrepareRequestValidator {

    public PnDeliveryRequest compareRequestEntity(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryRequest) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.equals(prepareRequest.getRequestId(), pnDeliveryRequest.getRequestId())){
            errors.add("RequestId");
        }
        if (!StringUtils.equals(prepareRequest.getReceiverFiscalCode(), pnDeliveryRequest.getFiscalCode())){
            errors.add("FiscalCode");
        }
        if (!StringUtils.equals(prepareRequest.getProposalProductType().getValue(), pnDeliveryRequest.getRegisteredLetterCode())){
            errors.add("ProductType");
        }

        //TO DO
        //attachments

        if (!errors.isEmpty()){
            throw new PnInputValidatorException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, errors);
        }
        else {
            return pnDeliveryRequest;
        }
    }


    public boolean checkAddressInfo(PrepareRequest prepareRequest, PnAddress pnAddress){

        if(!prepareRequest.getReceiverAddress().getAddress().equals(pnAddress.getAddress()) ||
                !prepareRequest.getReceiverAddress().getFullname().equals(pnAddress.getFullName()) ||
                !prepareRequest.getReceiverAddress().getNameRow2().equals(pnAddress.getNameRow2()) ||
                !prepareRequest.getReceiverAddress().getAddressRow2().equals(pnAddress.getAddressRow2()) ||
                !prepareRequest.getReceiverAddress().getCap().equals(pnAddress.getCap()) ||
                !prepareRequest.getReceiverAddress().getCity().equals(pnAddress.getCity()) ||
                !prepareRequest.getReceiverAddress().getCity2().equals(pnAddress.getCity2()) ||
                !prepareRequest.getReceiverAddress().getPr().equals(pnAddress.getPr()) ||
                !prepareRequest.getReceiverAddress().getCountry().equals(pnAddress.getCountry())){
            return true;
        }
        else{
            return false;
        }
    }
}
