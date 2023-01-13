package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.mapper.CostMapper;
import it.pagopa.pn.paperchannel.mapper.DeliveryDriverMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnPaperDeliveryDriver;
import it.pagopa.pn.paperchannel.model.DeliveryDriverFilter;
import it.pagopa.pn.paperchannel.rest.v1.dto.BaseResponse;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractInsertRequestDto;
import it.pagopa.pn.paperchannel.rest.v1.dto.PageableDeliveryDriverResponseDto;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaperChannelServiceImpl implements PaperChannelService {

    @Autowired
    private CostDAO costDAO;

    @Autowired
    private DeliveryDriverDAO deliveryDriverDAO;


    @Override
    public Mono<BaseResponse> createContract(ContractInsertRequestDto request) {
        PnPaperDeliveryDriver pnPaperDeliveryDriver = DeliveryDriverMapper.toContractRequest(request);
        List<PnPaperCost> costs = request.getList().stream().map(CostMapper::fromContractDTO).collect(Collectors.toList());
        return this.costDAO.createNewContract(pnPaperDeliveryDriver, costs).map(deliveryDriver -> {
            BaseResponse baseResponse = new BaseResponse();
            baseResponse.setResult(true);
            baseResponse.setCode(BaseResponse.CodeEnum.NUMBER_0);
            return baseResponse;
        });
    }

    @Override
    public Mono<PageableDeliveryDriverResponseDto> takeDeliveryDriver(DeliveryDriverFilter filter) {
        Pageable pageable = PageRequest.of(filter.getPage()-1, filter.getSize());
        return deliveryDriverDAO.getDeliveryDriver(filter)
                .map(list -> DeliveryDriverMapper.paginateList(pageable, list))
                .map(DeliveryDriverMapper::deliveryDriverToPageableDeliveryDriverDto);
        // recupero i dati
        // Paginiamo i dati
        // costruisco la response -> PageableDeliveryDriverResponseDto
    }

    @Override
    public Mono<PnPaperDeliveryDriver> addDeliveryDriver(PnPaperDeliveryDriver pnPaperDeliveryDriver) {
        return deliveryDriverDAO.addDeliveryDriver(pnPaperDeliveryDriver);
    }

}
