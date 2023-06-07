package it.pagopa.pn.paperchannel.service.impl;


import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.mapper.CostMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.ZoneDAO;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.ACTIVE_TENDER_NOT_FOUND;
import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COST_DRIVER_OR_FSU_NOT_FOUND;

@CustomLog
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
        String processName = "Get Cost From";
        log.logStartingProcess(processName);
        return this.tenderDAO.findActiveTender()
                .switchIfEmpty(Mono.error(new PnGenericException(ACTIVE_TENDER_NOT_FOUND, ACTIVE_TENDER_NOT_FOUND.getMessage())))
                .flatMap(tender -> costDAO.getByCapOrZoneAndProductType(tender.getTenderCode(), cap, zone, productType)
                            .switchIfEmpty(Mono.error(new PnGenericException(COST_DRIVER_OR_FSU_NOT_FOUND, COST_DRIVER_OR_FSU_NOT_FOUND.getMessage())))
                            .map(paperCost -> {
                                log.logEndingProcess(processName);
                                return CostMapper.toCostDTO(paperCost);
                            })
                );
    }

    @Override
    public Mono<String> getZoneFromCountry(String country) {
        String processName = "Get Zone From Country";
        log.logStartingProcess(processName);
        return zoneDAO.getByCountry(country)
                .map(pnZone -> {
                    log.logEndingProcess(processName);
                    return pnZone.getZone();
                });
    }
}
