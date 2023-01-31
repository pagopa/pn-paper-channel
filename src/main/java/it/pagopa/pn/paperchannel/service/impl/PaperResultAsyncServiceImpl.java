package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.mapper.AttachmentMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.msclient.NationalRegistryClient;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.DATA_NULL_OR_INVALID;

@Slf4j
@Service
public class PaperResultAsyncServiceImpl extends BaseService implements PaperResultAsyncService {


    public PaperResultAsyncServiceImpl(PnAuditLogBuilder auditLogBuilder, RequestDeliveryDAO requestDeliveryDAO,
                                       NationalRegistryClient nationalRegistryClient, SqsSender sqsSender) {
        super(auditLogBuilder, requestDeliveryDAO, null, nationalRegistryClient, sqsSender);
    }

    @Override
    public Mono<PnDeliveryRequest> resultAsyncBackground(SingleStatusUpdateDto singleStatusUpdateDto, Integer attempt) {
        if (singleStatusUpdateDto == null || singleStatusUpdateDto.getAnalogMail() == null || StringUtils.isBlank(singleStatusUpdateDto.getAnalogMail().getRequestId())){
            log.error("the message sent from external channel, includes errors. It cannot be processing");
            return Mono.error(new PnGenericException(DATA_NULL_OR_INVALID, DATA_NULL_OR_INVALID.getMessage()));
        }

        return requestDeliveryDAO.getByRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId())
                .flatMap(entity -> {
                    log.info("GETTED ENTITY: {}", entity.getRequestId());
                    pnLogAudit.addsReceive(entity.getIun(), String.format("prepare requestId = %s Response from external-channel", entity.getRequestId()),
                            String.format("prepare requestId = %s Response from external-channel status code %s",  entity.getRequestId(), entity.getStatusCode()));

                    return updateEntityResult(singleStatusUpdateDto, entity)
                            .flatMap(updatedEntity -> {
                                log.info("UPDATED ENTITY: {}", updatedEntity.getRequestId());
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

        sendEvent.setStatusCode(entity.getStatusCode());
        sendEvent.setStatusDetail(entity.getStatusDetail());
        sendEvent.setStatusDescription(entity.getStatusDetail());

        if (request.getAnalogMail() != null) {
            sendEvent.setRequestId(request.getAnalogMail().getRequestId());
            sendEvent.setStatusDateTime(DateUtils.getDatefromOffsetDateTime(request.getAnalogMail().getStatusDateTime()));
            sendEvent.setRegisteredLetterCode(request.getAnalogMail().getRegisteredLetterCode());
            sendEvent.setClientRequestTimeStamp(Date.from(request.getAnalogMail().getClientRequestTimeStamp().toInstant()));
            sendEvent.setDeliveryFailureCause(request.getAnalogMail().getDeliveryFailureCause());
            sendEvent.setDiscoveredAddress(AddressMapper.toPojo(request.getAnalogMail().getDiscoveredAddress()));

            if (request.getAnalogMail().getAttachments() != null && !request.getAnalogMail().getAttachments().isEmpty()) {
                sendEvent.setAttachments(
                        request.getAnalogMail().getAttachments()
                                .stream()
                                .map(AttachmentMapper::fromAttachmentDetailsDto)
                                .toList()
                );
            }
        }

        sqsSender.pushSendEvent(sendEvent);
    }

}
