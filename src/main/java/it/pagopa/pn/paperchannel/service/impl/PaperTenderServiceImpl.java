package it.pagopa.pn.paperchannel.service.impl;


import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.mapper.CostMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.ZoneDAO;
import it.pagopa.pn.paperchannel.rest.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ACTIVE_TENDER_NOT_FOUND;

@Slf4j
@Service
public class PaperTenderServiceImpl implements PaperTenderService {

    @Autowired
    private TenderDAO tenderDAO;
    @Autowired
    private CostDAO costDAO;
    @Autowired
    private ZoneDAO zoneDAO;

    @Override
    public Mono<CostDTO> getCostFrom(String cap, String zone, String productType){
        return this.tenderDAO.findActiveTender()
                .switchIfEmpty(Mono.error(new PnGenericException(ACTIVE_TENDER_NOT_FOUND, ACTIVE_TENDER_NOT_FOUND.getMessage())))
                .flatMap(tender -> {
                    return costDAO.getByCapOrZoneAndProductType(tender.getTenderCode(), cap, zone, productType)
                            //.switchIfEmpty() get cost of FSU
                            .map(CostMapper::toCostDTO);
                });
    }

    @Override
    public Mono<String> getZoneFromCountry(String country) {
        //TODO decommentare quando la tabella sarà popolata
//        return zoneDAO.getByCountry(country)
//                .map(item -> item.getZone());
        return Mono.just("ZONA_1");
    }
}
