package it.pagopa.pn.paperchannel.middleware.queue.consumer.handler;

import it.pagopa.pn.paperchannel.exception.PnDematNotValidException;
import it.pagopa.pn.paperchannel.middleware.db.dao.EventDematDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnEventDemat;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.AttachmentDetailsDto;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.service.SqsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.MetaDematUtils.*;

//Tutti gli eventi, fatta eccezione di quelli evidenziati (*), dovranno essere memorizzati nella tabella come entità DEMAT.
// Tutti gli eventi che contengono dematerializzazioni del tipo (Plico, 23L, Indagine, AR) dovranno essere inviati come
// eventi PROGRESS verso delivery-push (oltre che salvati a db)
@Slf4j
public class SaveDematMessageHandler extends SendToDeliveryPushHandler {


    private static final List<String> ATTACHMENT_TYPES_SEND_TO_DELIVERY_PUSH = List.of(
            "Plico",
            "AR",
            "Indagine",
            "23L"
    );

    protected final EventDematDAO eventDematDAO;

    private final Long ttlDays;

    public SaveDematMessageHandler(SqsSender sqsSender, EventDematDAO eventDematDAO, Long ttlDays) {
        super(sqsSender);
        this.eventDematDAO = eventDematDAO;
        this.ttlDays = ttlDays;
    }


    @Override
    public Mono<Void> handleMessage(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        //negli eventi DEMAT è presente sempre almeno un attachment
        if(CollectionUtils.isEmpty(paperRequest.getAttachments())) {
            return Mono.error(new PnDematNotValidException("Demat is not valid. Attachments are empty"));
        }

        var attachments = new ArrayList<>(paperRequest.getAttachments());

        return Flux.fromIterable(attachments)
                .flatMap(attachmentDetailsDto -> {
                    PnEventDemat pnEventDemat = buildPnEventDemat(paperRequest, attachmentDetailsDto);
                    return eventDematDAO.createOrUpdate(pnEventDemat)
                            .doOnNext(savedEntity -> log.info("[{}] Saved PaperRequest from ExcChannel: {}", paperRequest.getRequestId(), savedEntity))
                            .flatMap(savedEntity -> checkAndSendToDeliveryPush(entity, paperRequest, attachmentDetailsDto));
                })
                .then();

    }

    private Mono<Void> checkAndSendToDeliveryPush(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest, AttachmentDetailsDto attachmentDetailsDto) {
        if(sendToDeliveryPush(attachmentDetailsDto.getDocumentType())) {
            //invio a delivery push lo stesso evento quanti sono gli attachment
            //ogni evento inviato avrà l'attachment diverso
            paperRequest.setAttachments(List.of(attachmentDetailsDto));
            return sendToDeliveryPush(entity, paperRequest);
        }
        return Mono.empty();
    }

    protected PnEventDemat buildPnEventDemat(PaperProgressStatusEventDto paperRequest, AttachmentDetailsDto attachmentDetailsDto) {
        PnEventDemat pnEventDemat = new PnEventDemat();
        pnEventDemat.setDematRequestId(buildDematRequestId(paperRequest.getRequestId()));
        pnEventDemat.setDocumentTypeStatusCode(buildDocumentTypeStatusCode(attachmentDetailsDto.getDocumentType(), paperRequest.getStatusCode()));
        pnEventDemat.setTtl(paperRequest.getStatusDateTime().plusDays(ttlDays).toEpochSecond());

        pnEventDemat.setRequestId(paperRequest.getRequestId());
        pnEventDemat.setStatusCode(paperRequest.getStatusCode());
        pnEventDemat.setDocumentType(attachmentDetailsDto.getDocumentType());
        pnEventDemat.setDocumentDate(attachmentDetailsDto.getDate().toInstant());
        pnEventDemat.setStatusDateTime(paperRequest.getStatusDateTime().toInstant());
        pnEventDemat.setUri(attachmentDetailsDto.getUrl());
        return pnEventDemat;
    }

    protected Mono<Void> sendToDeliveryPush(PnDeliveryRequest entity, PaperProgressStatusEventDto paperRequest) {
        return super.handleMessage(entity, paperRequest);
    }


    private boolean sendToDeliveryPush(String attachmentType) {
        return ATTACHMENT_TYPES_SEND_TO_DELIVERY_PUSH.contains(attachmentType);
    }

}
