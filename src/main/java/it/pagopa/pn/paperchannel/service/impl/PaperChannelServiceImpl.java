package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.rest.v1.dto.BaseResponse;
import it.pagopa.pn.paperchannel.rest.v1.dto.ContractInsertRequestDto;
import it.pagopa.pn.paperchannel.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PaperChannelServiceImpl implements PaperChannelService {

    @Autowired
    private CostDAO costDAO;

    @Override
    public Mono<BaseResponse> createContract(ContractInsertRequestDto request) {
        // mapper di deliveryDriver from ContractInsertRequestDto
        // mapper list of PnPaperCost from ContractDTO -> request.getList()
        // chiamare CostDAO per la creazione ->
        //costDAO.createNewContract()
        return null;
    }
}
