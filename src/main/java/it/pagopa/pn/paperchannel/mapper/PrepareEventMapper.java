package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.DeliveryAsyncModel;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.PrepareEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.StatusCodeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.util.Date;

public class PrepareEventMapper {

    private PrepareEventMapper() {
        throw new IllegalCallerException();
    }

    private static final BaseMapper <PnAddress, AnalogAddress> baseMapperAddress = new BaseMapperImpl(PnAddress.class, AnalogAddress.class);

    public static PrepareEvent fromResult(PnDeliveryRequest request, PnAddress address){
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(request.getRequestId());
        entityEvent.setStatusCode(StatusCodeEnum.OK);
        if (request.getStatusCode().equals(StatusDeliveryEnum.IN_PROCESSING.getCode()) || request.getStatusCode().equals(StatusDeliveryEnum.NATIONAL_REGISTRY_WAITING.getCode())){
            entityEvent.setStatusCode(StatusCodeEnum.PROGRESS);
        }

        else if (request.getStatusCode().equals(StatusDeliveryEnum.UNTRACEABLE.getCode())){
            entityEvent.setStatusCode(StatusCodeEnum.KOUNREACHABLE);
        }

        if (address != null){
           entityEvent.setReceiverAddress(baseMapperAddress.toDTO(address));
        }

        entityEvent.setStatusDetail(request.getStatusDetail());
        entityEvent.setProductType(request.getProposalProductType());
        entityEvent.setStatusDateTime((DateUtils.parseDateString(request.getStatusDate())));
        return entityEvent;
    }

    public static PrepareEvent toPrepareEvent(DeliveryAsyncModel model){
        PrepareEvent entityEvent = new PrepareEvent();
        entityEvent.setRequestId(model.getRequestId());
        entityEvent.setStatusCode(StatusCodeEnum.PROGRESS);
        if (model.getAddress() != null){
            entityEvent.setReceiverAddress(AddressMapper.toPojo(model.getAddress()));
        }
        entityEvent.setStatusDetail(StatusDeliveryEnum.TAKING_CHARGE.getDescription());
        entityEvent.setProductType(model.getProductType().getValue());
        entityEvent.setStatusDateTime(new Date());
        return entityEvent;
    }

}
