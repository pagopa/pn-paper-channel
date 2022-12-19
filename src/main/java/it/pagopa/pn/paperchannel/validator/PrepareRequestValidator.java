package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;
import static it.pagopa.pn.paperchannel.mapper.AddressMapper.fromAnalogToAddress;

@Component
public class PrepareRequestValidator {

    public PnDeliveryRequest compareRequestEntity(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity){
        List<String> errors = new ArrayList<>();

        if (!prepareRequest.getRequestId().equals(pnDeliveryEntity.getRequestId())) {
            errors.add("RequestId");
        }

        if (!prepareRequest.getReceiverFiscalCode().equals(pnDeliveryEntity.getFiscalCode())) {
            errors.add("FiscalCode");
        }

        if (!prepareRequest.getProductType().equals(pnDeliveryEntity.getRegisteredLetterCode())) {
            errors.add("ProductType");
        }

        if (!prepareRequest.getReceiverType().equals(pnDeliveryEntity.getReceiverType())){
            errors.add("ReceiverType");
        }

        if (!prepareRequest.getPrintType().equals(pnDeliveryEntity.getPrintType())){
            errors.add("printType");
        }

        if (!checkBetweenLists(prepareRequest, pnDeliveryEntity)){
            errors.add("Attachments");
        }

        if (prepareRequest.getReceiverAddress() != null) {
            if (!fromAnalogToAddress(prepareRequest.getReceiverAddress()).convertToHash().equals(pnDeliveryEntity.getAddressHash())) {
                errors.add("Address");
            }
        }
        else{
            errors.add("Address");
        }

        if (!errors.isEmpty()){
            throw new PnInputValidatorException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, errors);
        }

        //caso in cui tutti i campi sono uguali tra loro -> 200 ok
        else {
            return pnDeliveryEntity;
        }
    }

    public boolean checkBetweenLists(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryRequest){
        List<String> attachmentPrepare = prepareRequest.getAttachmentUrls();
        List<PnAttachmentInfo> attachmentDeliveryRequest = pnDeliveryRequest.getAttachments();
        attachmentPrepare.sort(Comparator.naturalOrder());
        attachmentDeliveryRequest.sort(Comparator.comparing(PnAttachmentInfo::getFileKey));
        if (attachmentPrepare.size() != attachmentDeliveryRequest.size()) {
            return false;
        }
        else {
            for (int i = 0; i < attachmentPrepare.size() ; i++){
                if (!attachmentPrepare.get(i).equals(attachmentDeliveryRequest.get(i).getFileKey())){
                    return false;
                }
            }
            return true;
        }
    }
}
