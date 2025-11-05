package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.exception.*;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;

import static it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum.*;
import static it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum.PREPARE_PHASE_ONE_ASYNC_DEFAULT;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;

public class PrepareAsyncErrorUtils {

    public static StatusDeliveryEnum retrieveStatusDeliveryEnum(Throwable ex) {
        if(ex instanceof PnGenericException pnGenericException) {
            return exceptionTypeMapper(pnGenericException.getExceptionType());
        }
        return PAPER_CHANNEL_ASYNC_ERROR;
    }

    public static ErrorFlowTypeEnum retrieveErrorFlowType(Throwable ex, boolean isPhaseOne) {
        return isPhaseOne ? PREPARE_PHASE_ONE_ASYNC_DEFAULT : PREPARE_PHASE_TWO_ASYNC_DEFAULT;
    }

    private static StatusDeliveryEnum exceptionTypeMapper(ExceptionTypeEnum ex){
        return switch (ex) {
            case DOCUMENT_NOT_DOWNLOADED, DOCUMENT_URL_NOT_FOUND -> StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR;
            default -> StatusDeliveryEnum.PAPER_CHANNEL_DEFAULT_ERROR;
        };
    }


    public static PnRequestError buildError(String requestId, Throwable ex, String flowType) {
        return PnRequestError.builder()
                .requestId(requestId)
                .error(ex.getMessage())
                .flowThrow(flowType)
                .build();
    }

}
