package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
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
import static it.pagopa.pn.paperchannel.mapper.AddressMapper.fromAnalogToAddress;


@CustomLog
public class PrepareRequestValidator {

    static String VALIDATION_NAME = "Prepare Request Validator";

    private PrepareRequestValidator(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static void compareRequestEntity(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, boolean firstAttempt) {
        List<String> errors = new ArrayList<>();
        log.logChecking(VALIDATION_NAME);

        if (!StringUtils.equals(prepareRequest.getRequestId(), pnDeliveryEntity.getRequestId())) {
            errors.add("RequestId");
            log.debug("Comparison between request and entity failed, different data: RequestId");
        }

        if (!StringUtils.equals(prepareRequest.getIun(), pnDeliveryEntity.getIun())) {
            errors.add("Iun");
            log.debug("Comparison between request and entity failed, different data: Iun");
        }

        if (!StringUtils.equals(Utility.convertToHash(prepareRequest.getReceiverFiscalCode()), pnDeliveryEntity.getHashedFiscalCode())) {
            errors.add("FiscalCode");
            log.debug("Comparison between request and entity failed, different data: FiscalCode");
        }

        if (!StringUtils.equals(prepareRequest.getProposalProductType().getValue(), pnDeliveryEntity.getProposalProductType())) {
            errors.add("ProductType");
            log.debug("Comparison between request and entity failed, different data: ProductType");
        }

        if (!StringUtils.equals(prepareRequest.getReceiverType(), pnDeliveryEntity.getReceiverType())) {
            errors.add("ReceiverType");
            log.debug("Comparison between request and entity failed, different data: ReceiverType");
        }

        if (!StringUtils.equals(prepareRequest.getPrintType(), (pnDeliveryEntity.getPrintType()))) {
            errors.add("PrintType");
            log.debug("Comparison between request and entity failed, different data: PrintType");
        }

        if (pnDeliveryEntity.getAttachments() != null) {
            List<String> fromDb = pnDeliveryEntity.getAttachments().stream()
                    .map(PnAttachmentInfo::getFileKey).collect(Collectors.toList());
            if (!AttachmentValidator.checkBetweenLists(prepareRequest.getAttachmentUrls(), fromDb)) {
                errors.add("Attachments");
                log.debug("Comparison between request and entity failed, different data: Attachments");
            }
        }


        if (firstAttempt) {
            if (prepareRequest.getReceiverAddress() != null) {
                if (!StringUtils.equals(fromAnalogToAddress(prepareRequest.getReceiverAddress()).convertToHash(), (pnDeliveryEntity.getAddressHash()))) {
                    errors.add("Address");
                    log.debug("Comparison between request and entity failed, different data: Address");
                }

            } else {
                errors.add("Address");
                log.debug("Comparison between request and entity failed, different data: Address");
            }
        }

        if (!errors.isEmpty()) {
            log.logCheckingOutcome(VALIDATION_NAME, false, errors.toString());
            throw new PnInputValidatorException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, errors);
        }
        log.logCheckingOutcome(VALIDATION_NAME, true);
    }

}
