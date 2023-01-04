package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.mapper.AddressMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
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
        return requestDeliveryDAO.getByRequestId(singleStatusUpdateDto.getAnalogMail().getRequestId())
                .flatMap(entity -> {
                    log.info("GETTED ENTITY: {}", entity);
                    compareProgressStatusRequestEntity(singleStatusUpdateDto.getAnalogMail(), entity);
                    return updateEntityResult(singleStatusUpdateDto, entity)
                            .flatMap(updatedEntity -> {
                                log.info("UPDATED ENTITY: {}", updatedEntity);
                                sendPaperResponse(singleStatusUpdateDto, updatedEntity);
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

    private void sendPaperResponse(SingleStatusUpdateDto singleStatusUpdateDto, PnDeliveryRequest pnDeliveryRequestMono) {
        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        SendEvent sendEvent = new SendEvent();
        paperChannelUpdate.sendEvent(sendEvent);
        sendEvent.setDiscoveredAddress(AddressMapper.toPojo(singleStatusUpdateDto.getAnalogMail().getDiscoveredAddress()));
        sendEvent.setClientRequestTimeStamp(Date.from(singleStatusUpdateDto.getAnalogMail().getClientRequestTimeStamp().toInstant()));
        if(StringUtils.isNotEmpty(singleStatusUpdateDto.getAnalogMail().getDeliveryFailureCause())) {
            sendEvent.setDeliveryFailureCause(singleStatusUpdateDto.getAnalogMail().getDeliveryFailureCause());
        }
        sqsSender.pushSendEvent(sendEvent);
    }

}
