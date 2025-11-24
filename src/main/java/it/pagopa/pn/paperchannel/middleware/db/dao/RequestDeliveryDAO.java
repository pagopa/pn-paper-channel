package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

public interface RequestDeliveryDAO {

    Mono<PnDeliveryRequest> createWithAddress(PnDeliveryRequest request, PnAddress pnAddress, PnAddress discoveredAddress);

    Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest);
    Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest, boolean ignorableNulls);
    Mono<PnDeliveryRequest> updateDataWithoutGet(PnDeliveryRequest pnDeliveryRequest, boolean ignorableNulls);
    Mono<Void> updateStatus(String requestId,String statusCode, String statusDescription, String statusDetail, String statusDateString);

    Mono<PnDeliveryRequest> updateConditionalOnFeedbackStatus(PnDeliveryRequest pnDeliveryRequest, boolean ignorableNulls);

    Mono<PnDeliveryRequest> getByRequestId(String requestId);
    Mono<PnDeliveryRequest> getByRequestId(String requestId, boolean decode);
    Mono<PnDeliveryRequest> getByRequestIdStrongConsistency(String requestId, boolean decode);
    Mono<PnDeliveryRequest> getByCorrelationId(String requestId, boolean decode);
    Mono<PnDeliveryRequest> getByCorrelationId(String correlationId);
    Mono<UpdateItemResponse> updateApplyRasterization(String requestId, Boolean applyRasterization);
    Mono<PnDeliveryRequest> cleanDataForNotificationRework(PnDeliveryRequest pnDeliveryRequest, String reworkId);
}
