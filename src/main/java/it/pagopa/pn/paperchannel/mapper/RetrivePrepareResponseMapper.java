package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.AddressEntity;
import it.pagopa.pn.paperchannel.middleware.db.entities.RequestDeliveryEntity;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.utils.DateUtils;

public class RetrivePrepareResponseMapper {

    private static final BaseMapper <AddressEntity, AnalogAddress> baseMapperAddress = new BaseMapperImpl(AddressEntity.class, AnalogAddress.class);

    public static PrepareEvent fromResult(RequestDeliveryEntity request){
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(request.getRequestId());
        entityEvent.setStatusCode(StatusDeliveryEnum.IN_PROCESSING.getCode());
        entityEvent.setStatusDetail(StatusDeliveryEnum.IN_PROCESSING.getDescription());
        entityEvent.setReceiverAddress(baseMapperAddress.toDTO(request.getAddress()));
        entityEvent.setStatusDateTime((DateUtils.parseDateString(request.getStatusDate())));
        return entityEvent;
    }
}
