package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PaperChannelUpdate;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;


import java.util.Date;

public class PreparePaperResponseMapper {

    private PreparePaperResponseMapper(){
        throw new IllegalCallerException();
    }

    public static PaperChannelUpdate fromResult(PnDeliveryRequest item, PnAddress pnAddress){
        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
        if (hasPrepareStatusCode(item.getStatusCode())){
            paperChannelUpdate.setPrepareEvent(PrepareEventMapper.fromResult(item,pnAddress));
        } else {
            paperChannelUpdate.setSendEvent(SendEventMapper.fromResult(item,pnAddress));
        }
        return paperChannelUpdate;
    }


    private static boolean hasPrepareStatusCode(String statusCode) {
        return statusCode.equals(StatusDeliveryEnum.IN_PROCESSING.getCode())
                || statusCode.equals(StatusDeliveryEnum.TAKING_CHARGE.getCode())
                || statusCode.equals(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode())
                || statusCode.equals(StatusDeliveryEnum.PAPER_CHANNEL_ASYNC_ERROR.getCode())
                || statusCode.equals(StatusDeliveryEnum.SAFE_STORAGE_IN_ERROR.getCode());
    }

}
