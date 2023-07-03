package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_RESULT;
import static it.pagopa.pn.paperchannel.mapper.AddressMapper.fromAnalogToAddress;


@CustomLog
public class SendRequestValidator {

    static String VALIDATION_NAME = "Send request validator";
    private SendRequestValidator() {
        throw new IllegalCallerException();
    }

    public static void compareRequestEntity(SendRequest sendRequest, PnDeliveryRequest pnDeliveryEntity){
        List<String> errors = new ArrayList<>();
        log.logChecking(VALIDATION_NAME);
        if (!sendRequest.getRequestId().equals(pnDeliveryEntity.getRequestId())) {
            errors.add("RequestId");
            log.debug("Comparison between request and entity failed, different data: RequestId");
        }

        if (!Utility.convertToHash(sendRequest.getReceiverFiscalCode()).equals(pnDeliveryEntity.getHashedFiscalCode())) {
            errors.add("FiscalCode");
            log.debug("Comparison between request and entity failed, different data: FiscalCode");
        }

        if (!sendRequest.getProductType().getValue().equals(pnDeliveryEntity.getProductType())) {
            errors.add("ProductType");
            log.debug("Comparison between request and entity failed, different data: ProductType");
        }

        if (!sendRequest.getReceiverType().equals(pnDeliveryEntity.getReceiverType())){
            errors.add("ReceiverType");
            log.debug("Comparison between request and entity failed, different data: ReceiverType");
        }

        if (!sendRequest.getPrintType().equals(pnDeliveryEntity.getPrintType())){
            errors.add("PrintType");
            log.debug("Comparison between request and entity failed, different data: PrintType");
        }

        List<String> fromDb = pnDeliveryEntity.getAttachments().stream()
                .map(PnAttachmentInfo::getFileKey).collect(Collectors.toList());
        if (!AttachmentValidator.checkBetweenLists(sendRequest.getAttachmentUrls(), fromDb)){
            errors.add("Attachments");
            log.debug("Comparison between request and entity failed, different data: Attachments");
        }

        if (!fromAnalogToAddress(sendRequest.getReceiverAddress()).convertToHash().equals(pnDeliveryEntity.getAddressHash())) {
            errors.add("Address");
            log.debug("Comparison between request and entity failed, different data: Address");
        }

        if (!errors.isEmpty()){
            log.logCheckingOutcome(VALIDATION_NAME, false, errors.toString());
            throw new PnInputValidatorException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, errors);
        }
        log.logCheckingOutcome(VALIDATION_NAME, true);
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
                    .map(AttachmentDetailsDto::getUri).collect(Collectors.toList());

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
