package it.pagopa.pn.paperchannel.validator;

import it.pagopa.pn.paperchannel.exception.PnInputValidatorException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PrepareRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.service.impl.F24ServiceImpl;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DIFFERENT_DATA_REQUEST;
import static it.pagopa.pn.paperchannel.mapper.AddressMapper.fromAnalogToAddress;


@CustomLog
public class PrepareRequestValidator {

    private static final String VALIDATION_NAME = "Prepare Request Validator";

    private PrepareRequestValidator(){
        throw new IllegalCallerException("the constructor must not called");
    }

    public static void compareRequestEntity(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, boolean firstAttempt, boolean skipF24Check) {
        List<String> errors = new ArrayList<>();
        log.logChecking(VALIDATION_NAME);

        validateRequestId(prepareRequest, pnDeliveryEntity, errors);

        validateIun(prepareRequest, pnDeliveryEntity, errors);

        validateFiscalCode(prepareRequest, pnDeliveryEntity, errors);

        validateProductType(prepareRequest, pnDeliveryEntity, errors);

        validateReceiverType(prepareRequest, pnDeliveryEntity, errors);

        validatePrintType(prepareRequest, pnDeliveryEntity, errors);

        validateAttachments(prepareRequest, pnDeliveryEntity, skipF24Check, errors);

        validateReceiverAddress(prepareRequest, pnDeliveryEntity, firstAttempt, errors);

        if (!errors.isEmpty()) {
            log.logCheckingOutcome(VALIDATION_NAME, false, errors.toString());
            throw new PnInputValidatorException(DIFFERENT_DATA_REQUEST, DIFFERENT_DATA_REQUEST.getMessage(), HttpStatus.CONFLICT, errors);
        }
        log.logCheckingOutcome(VALIDATION_NAME, true);
    }

    private static void validatePrintType(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, List<String> errors) {
        if (!StringUtils.equals(prepareRequest.getPrintType(), (pnDeliveryEntity.getPrintType()))) {
            errors.add("PrintType");
            log.debug("Comparison between request and entity failed, different data: PrintType");
        }
    }

    private static void validateReceiverType(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, List<String> errors) {
        if (!StringUtils.equals(prepareRequest.getReceiverType(), pnDeliveryEntity.getReceiverType())) {
            errors.add("ReceiverType");
            log.debug("Comparison between request and entity failed, different data: ReceiverType");
        }
    }

    private static void validateProductType(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, List<String> errors) {
        if (!StringUtils.equals(prepareRequest.getProposalProductType().getValue(), pnDeliveryEntity.getProposalProductType())) {
            errors.add("ProductType");
            log.debug("Comparison between request and entity failed, different data: ProductType");
        }
    }

    private static void validateFiscalCode(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, List<String> errors) {
        if (!StringUtils.equals(Utility.convertToHash(prepareRequest.getReceiverFiscalCode()), pnDeliveryEntity.getHashedFiscalCode())) {
            errors.add("FiscalCode");
            log.debug("Comparison between request and entity failed, different data: FiscalCode");
        }
    }

    private static void validateIun(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, List<String> errors) {
        if (!StringUtils.equals(prepareRequest.getIun(), pnDeliveryEntity.getIun())) {
            errors.add("Iun");
            log.debug("Comparison between request and entity failed, different data: Iun");
        }
    }

    private static void validateRequestId(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, List<String> errors) {
        if (!StringUtils.equals(prepareRequest.getRequestId(), pnDeliveryEntity.getRequestId())) {
            errors.add("RequestId");
            log.debug("Comparison between request and entity failed, different data: RequestId");
        }
    }

    private static void validateAttachments(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, boolean skipF24Check, List<String> errors) {
        if (pnDeliveryEntity.getAttachments() != null) {
            // escludo dal check di idempotenza gli url f24set e le filekey generate da un urlf24set
            // questo perchè:
            // 1) in caso di doppia richiesta della stessa prepare, il doppione potrebbe arrivare "prima" della generazione f24
            //    (e allora le liste son uguali), o "dopo" (e allora le liste son diverse, non ho più filekey f24set ma ho N filekey con generatedFrom )
            // 2) in caso di check tra prepare seconda raccomandata e vs prepare prima raccomandata, qui sicuramente le liste
            //    son diverse perchè nella prima prepare ho già generato gli f24 mentre nella seconda no (e le filekey potrebbero essere diversi, cambia il costo).
            // NB: new arraylist perchè sennò il tolist è immutabile
            List<String> fromDb = getFileKeyExcludingF24(pnDeliveryEntity.getAttachments());

            //nella prepare secondo tentativo, vengono mandati da delivery-push tutti gli attachment, quindi questi ultimi devono essere
            //confrontati con tutti gli attachments dell'entità con ATTEMPT=0 (compresi gli eventuali removedAttachments)
            List<String> attachmentsRemovedFromDb = getFileKeyExcludingF24(pnDeliveryEntity.getRemovedAttachments());
            fromDb.addAll(attachmentsRemovedFromDb);


            List<String> fromRequest = new ArrayList<>(prepareRequest.getAttachmentUrls().stream()
                    .filter(x -> !x.startsWith(F24ServiceImpl.URL_PROTOCOL_F24))
                    .map(AttachmentsConfigUtils::cleanFileKey)
                    .toList());

            if (!AttachmentValidator.checkBetweenLists(fromRequest, fromDb)) {
                errors.add("Attachments");
                log.debug("Comparison between request and entity failed, different data: Attachments");
            }

            // nel caso vi siano stati filtri su filekey f24set (size diverse vuol dire che era presente una filekey f24set)
            // gestisco puntualmente la validazione, se richiesto (non va fatto su check tra 2a prepare vs 1a prepare)
            if (!skipF24Check && fromRequest.size() != prepareRequest.getAttachmentUrls().size()) {
                Optional<String> optF24set = prepareRequest.getAttachmentUrls().stream().filter(x -> x.startsWith(F24ServiceImpl.URL_PROTOCOL_F24)).findFirst();
                optF24set.ifPresent(f24set -> {
                    Optional<PnAttachmentInfo> opt = pnDeliveryEntity.getAttachments().stream().filter(x -> f24set.equals(x.getFileKey()) || f24set.equals(x.getGeneratedFrom())).findFirst();
                    if (opt.isEmpty()) {
                        errors.add("Attachments");
                        log.debug("Comparison between request and entity failed, different data: Attachments F24");
                    }
                });
            }
        }
    }

    private static List<String> getFileKeyExcludingF24(List<PnAttachmentInfo> pnAttachments) {
        if(CollectionUtils.isEmpty(pnAttachments)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(pnAttachments.stream()
                .filter(x -> x.getGeneratedFrom() == null && !x.getFileKey().startsWith(F24ServiceImpl.URL_PROTOCOL_F24))
                .map(PnAttachmentInfo::getFileKey)
                .map(AttachmentsConfigUtils::cleanFileKey)
                .toList());
    }


    private static void validateReceiverAddress(PrepareRequest prepareRequest, PnDeliveryRequest pnDeliveryEntity, boolean firstAttempt, List<String> errors) {
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
    }

}
