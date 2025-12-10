package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.exception.*;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;

import static it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum.*;
import static it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum.PREPARE_PHASE_ONE_ASYNC_DEFAULT;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;

public class PrepareAsyncErrorUtils {

    private PrepareAsyncErrorUtils(){}

    public static StatusDeliveryEnum retrieveStatusDeliveryEnum(Throwable ex) {
        if(ex instanceof PnGenericException) {
            return StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR;
        }
        return PAPER_CHANNEL_ASYNC_ERROR;
    }

    public static ErrorFlowTypeEnum retrieveErrorFlowType(Throwable ex, boolean isPhaseOne) {
        if (ex instanceof CheckAddressFlowException checkAddressFlowException){
            return checkAddressFlowException.getFlowTypeEnum();
        }
        return isPhaseOne ? PREPARE_PHASE_ONE_ASYNC_DEFAULT : PREPARE_PHASE_TWO_ASYNC_DEFAULT;
    }


    public static String extractGeoKey(Throwable ex) {
        return ex instanceof StopFlowSecondAttemptException stopFlowSecondAttemptException
                ? stopFlowSecondAttemptException.getGeokey()
                : null;
    }


    public static PnRequestError buildError(String requestId, Throwable ex, String flowType) {
        return PnRequestError.builder()
                .requestId(requestId)
                .error(String.format("%s -> %s", ex.getClass(), ex.getMessage()))
                .geokey(ex instanceof CheckAddressFlowException addressFlowException ? addressFlowException.getGeoKey() : null)
                .flowThrow(flowType)
                .build();
    }

}
