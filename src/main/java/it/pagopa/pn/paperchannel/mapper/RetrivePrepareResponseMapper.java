package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;

public class RetrivePrepareResponseMapper {

    private static final BaseMapper <PnAddress, AnalogAddress> baseMapperAddress = new BaseMapperImpl(PnAddress.class, AnalogAddress.class);

    public static PrepareEvent fromResult(PnDeliveryRequest request){
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(request.getRequestId());
        entityEvent.setStatusCode(StatusCodeEnum.PROGRESS);
        entityEvent.setStatusDetail(StatusDeliveryEnum.IN_PROCESSING.getDescription());
      //  entityEvent.setReceiverAddress(baseMapperAddress.toDTO(request.getAddress()));
        entityEvent.setStatusDateTime((DateUtils.parseDateString(request.getStatusDate())));
        return entityEvent;
    }

}
