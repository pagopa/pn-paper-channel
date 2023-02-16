package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.mapper.common.BaseMapper;
import it.pagopa.pn.paperchannel.mapper.common.BaseMapperImpl;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.msclient.generated.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.paperchannel.rest.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendEvent;
import it.pagopa.pn.paperchannel.rest.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;

import java.util.List;
import java.util.stream.Collectors;

public class SendRequestMapper {

    private SendRequestMapper(){
        throw new IllegalCallerException();
    }

    private static final BaseMapper<PnAddress, SendRequest> baseMapperAddress = new BaseMapperImpl(PnAddress.class, SendRequest.class);

    public static SendRequest toDto(List<PnAddress> addressList, PnDeliveryRequest pnDeliveryRequest){
        SendRequest sendRequest = new SendRequest();
        sendRequest.setRequestId(pnDeliveryRequest.getRequestId());
        sendRequest.setIun(pnDeliveryRequest.getIun());
        sendRequest.setProductType(ProductTypeEnum.valueOf(pnDeliveryRequest.getProductType()));
        sendRequest.setAttachmentUrls(pnDeliveryRequest.getAttachments().stream().map(i -> i.getUrl()).collect(Collectors.toList()));
        addressList.forEach(address -> {
            if (address.getTypology().equals(AddressTypeEnum.RECEIVER_ADDRESS.toString()))  {
                sendRequest.setReceiverAddress(AddressMapper.fromEntity(address));
            } else if (address.getTypology().equals(AddressTypeEnum.SENDER_ADDRES.toString()))  {
                sendRequest.setSenderAddress(AddressMapper.fromEntity(address));
            } else if (address.getTypology().equals(AddressTypeEnum.AR_ADDRESS.toString()))  {
                sendRequest.setArAddress(AddressMapper.fromEntity(address));
            }
        });
        return sendRequest;
    }
}
