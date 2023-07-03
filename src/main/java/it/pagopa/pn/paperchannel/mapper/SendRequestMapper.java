package it.pagopa.pn.paperchannel.mapper;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.SendRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAttachmentInfo;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;

import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;

import java.util.List;

public class SendRequestMapper {

    private SendRequestMapper(){
        throw new IllegalCallerException();
    }


    public static SendRequest toDto(List<PnAddress> addressList, PnDeliveryRequest pnDeliveryRequest){
        SendRequest sendRequest = new SendRequest();
        sendRequest.setRequestId(pnDeliveryRequest.getRequestId());
        sendRequest.setIun(pnDeliveryRequest.getIun());
        if (pnDeliveryRequest.getProductType().equals("890"))
            sendRequest.setProductType(ProductTypeEnum.valueOf(("_").concat(pnDeliveryRequest.getProductType())));
        else
            sendRequest.setProductType(ProductTypeEnum.valueOf(pnDeliveryRequest.getProductType()));
        sendRequest.setRequestPaId(pnDeliveryRequest.getRequestPaId());
        sendRequest.setPrintType(pnDeliveryRequest.getPrintType());
        sendRequest.setAttachmentUrls(pnDeliveryRequest.getAttachments().stream().map(PnAttachmentInfo::getUrl).toList());
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
