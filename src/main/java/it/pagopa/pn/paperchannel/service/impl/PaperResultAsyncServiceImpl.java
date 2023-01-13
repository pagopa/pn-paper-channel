package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.AttachmentDetails;
import it.pagopa.pn.paperchannel.rest.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.stream.Collectors;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DATA_NULL_OR_INVALID;
import static it.pagopa.pn.paperchannel.validator.SendRequestValidator.compareProgressStatusRequestEntity;

@Slf4j
@Service
public class PaperResultAsyncServiceImpl implements PaperResultAsyncService {

    @Autowired
    private RequestDeliveryDAO requestDeliveryDAO;
    @Autowired
    private SqsSender sqsSender;

    @Override
    public Mono<PnDeliveryRequest> resultAsyncBackground(SingleStatusUpdateDto singleStatusUpdateDto) {
        if (singleStatusUpdateDto == null || singleStatusUpdateDto.getAnalogMail() == null || StringUtils.isBlank(singleStatusUpdateDto.getAnalogMail().getRequestId())){
            log.error("the message sent from external channel, includes errors. It cannot be processing");
            return Mono.error(new PnGenericException(DATA_NULL_OR_INVALID, DATA_NULL_OR_INVALID.getMessage()));
        }

        return requestDeliveryDAO.getByRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId())
                .flatMap(entity -> {
                    log.info("GETTED ENTITY: {}", entity);

                    return updateEntityResult(singleStatusUpdateDto, entity)
                            .flatMap(updatedEntity -> {
                                log.info("UPDATED ENTITY: {}", updatedEntity);
                                sendPaperResponse(updatedEntity, singleStatusUpdateDto);
                                return Mono.just(updatedEntity);
                            });
                })
                //TODO case of retry event from external-channel queue
                .onErrorResume(ex -> {
                    ex.printStackTrace();
                    return Mono.error(ex);
                });
    }

    private Mono<PnDeliveryRequest> updateEntityResult(SingleStatusUpdateDto singleStatusUpdateDto, PnDeliveryRequest pnDeliveryRequestMono) {
        pnDeliveryRequestMono.setStatusCode(singleStatusUpdateDto.getAnalogMail().getStatusCode());
        pnDeliveryRequestMono.setStatusDetail(singleStatusUpdateDto.getAnalogMail().getStatusDescription());
        pnDeliveryRequestMono.setStatusDate(DateUtils.formatDate(Date.from(singleStatusUpdateDto.getAnalogMail().getStatusDateTime().toInstant())));
        return requestDeliveryDAO.updateData(pnDeliveryRequestMono);
    }

    private void sendPaperResponse(PnDeliveryRequest entity, SingleStatusUpdateDto request) {
        SendEvent sendEvent = new SendEvent();
        sendEvent.setRequestId(entity.getRequestId());
        sendEvent.setStatusCode(entity.getStatusCode());
        sendEvent.setStatusDetail(entity.getStatusDetail());
        sendEvent.setStatusDescription(entity.getStatusDetail());
        sendEvent.setStatusDateTime(DateUtils.parseDateString(entity.getStatusDate()));
        sendEvent.setRegisteredLetterCode(entity.getProposalProductType());
        sendEvent.setClientRequestTimeStamp(Date.from(request.getAnalogMail().getClientRequestTimeStamp().toInstant()));

        if (entity.getAttachments() != null && !entity.getAttachments().isEmpty())
            sendEvent.setAttachments(entity.getAttachments().stream().map(AttachmentMapper::toAttachmentDetails).collect(Collectors.toList()));

        if (request.getAnalogMail() != null) {
            sendEvent.setDeliveryFailureCause(request.getAnalogMail().getDeliveryFailureCause());
            sendEvent.setDiscoveredAddress(AddressMapper.toPojo(request.getAnalogMail().getDiscoveredAddress()));
        }

        sqsSender.pushSendEvent(sendEvent);
    }

}
