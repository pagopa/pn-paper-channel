package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_RESULT;
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

        if (!sendRequest.getProductType().getValue().equals(pnDeliveryEntity.getProductType())) {
            errors.add("ProductType");
        }

        if (!sendRequest.getReceiverType().equals(pnDeliveryEntity.getReceiverType())){
            errors.add("ReceiverType");
        }

        if (!sendRequest.getPrintType().equals(pnDeliveryEntity.getPrintType())){
            errors.add("printType");
        }

        List<String> fromDb = pnDeliveryEntity.getAttachments().stream()
                .map(PnAttachmentInfo::getFileKey).collect(Collectors.toList());
        if (!AttachmentValidator.checkBetweenLists(sendRequest.getAttachmentUrls(), fromDb)){
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

    public static void compareProgressStatusRequestEntity(PaperProgressStatusEventDto paperProgressStatusEventDto, PnDeliveryRequest pnDeliveryEntity) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.equals(paperProgressStatusEventDto.getIun(), pnDeliveryEntity.getIun())) {
            errors.add("Iun");
        }

        if (!StringUtils.equals(paperProgressStatusEventDto.getProductType(), pnDeliveryEntity.getProposalProductType())) {
            errors.add("ProductType");
        }

        if (paperProgressStatusEventDto.getAttachments() != null && pnDeliveryEntity.getAttachments() != null) {
            List<String> fromDb = pnDeliveryEntity.getAttachments().stream()
                    .map(PnAttachmentInfo::getUrl).collect(Collectors.toList());
            List<String> fromExternal = paperProgressStatusEventDto.getAttachments().stream()
                    .map(AttachmentDetailsDto::getUrl).collect(Collectors.toList());

            if(!AttachmentValidator.checkBetweenLists(fromDb, fromExternal)) {
                errors.add("Attachments");
            }
        } else {
            errors.add("Attachments");
        }

        if (!errors.isEmpty()){
            throw new PnInputValidatorException(DIFFERENT_DATA_RESULT, DIFFERENT_DATA_RESULT.getMessage(), HttpStatus.CONFLICT, errors);
        }
    }
}
