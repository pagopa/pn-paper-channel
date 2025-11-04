package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnF24FlowException;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;

import static it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum.F24_FLOW;
import static it.pagopa.pn.paperchannel.model.ErrorFlowTypeEnum.PREPARE_PHASE_TWO_ASYNC_DEFAULT;
import static it.pagopa.pn.paperchannel.model.StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR;

public class PrepareAsyncErrorUtils {

    public static StatusDeliveryEnum determineStatusDeliveryEnum(Throwable ex) {
        if(ex instanceof PnGenericException pnGenericException) {
            return exceptionTypeMapper(pnGenericException.getExceptionType());
        }
        return PAPER_CHANNEL_ASYNC_ERROR;
    }

    public static ErrorFlowTypeEnum determineFlowType(Throwable ex) {
        if (ex instanceof PnF24FlowException) {
            return F24_FLOW;
        }
        return PREPARE_PHASE_TWO_ASYNC_DEFAULT;
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
