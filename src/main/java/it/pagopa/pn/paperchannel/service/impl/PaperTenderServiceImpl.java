package it.pagopa.pn.paperchannel.service.impl;


import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.mapper.CostMapper;
import it.pagopa.pn.paperchannel.mapper.PnPaperChannelCostMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.*;
import it.pagopa.pn.paperchannel.model.PnPaperChannelCostDTO;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;

@CustomLog
@Service
@RequiredArgsConstructor
public class PaperTenderServiceImpl implements PaperTenderService {
    private final PnPaperCostDAO pnPaperCostDAO;
    private final PnPaperTenderDAO pnPaperTenderDAO;
    private final PnPaperGeoKeyDAO pnPaperGeoKeyDAO;
    private final TenderDAO tenderDAO;
    private final CostDAO costDAO;
    private final ZoneDAO zoneDAO;

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
    public Mono<PnPaperChannelCostDTO> getSimplifiedCost(String cap, String zone, String productType) {
        String processName = "Get New Cost From";
        log.logStartingProcess(processName);
        String geoKeyValue = StringUtils.isNotEmpty(cap) ? cap : zone;
        return this.pnPaperTenderDAO.getActiveTender()
                .switchIfEmpty(Mono.error(new PnGenericException(ACTIVE_TENDER_NOT_FOUND, ACTIVE_TENDER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND)))
                .flatMap(tender -> pnPaperGeoKeyDAO.getGeoKey(tender.getTenderId(), productType, geoKeyValue)
                        .switchIfEmpty(Mono.error(new PnGenericException(GEOKEY_NOT_FOUND, GEOKEY_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND)))
                        .doOnNext(geoKey -> log.info("Geokey finded {}", geoKey))
                        .flatMap(geoKey -> pnPaperCostDAO.getCostByTenderIdProductLotZone(geoKey.getTenderId(), productType, geoKey.getLot(), geoKey.getZone()))
                        .switchIfEmpty(Mono.error(new PnGenericException(COST_DRIVER_OR_FSU_NOT_FOUND, COST_DRIVER_OR_FSU_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND)))
                        .doOnNext(paperCost -> log.info("Cost finded {}", paperCost))
                        .map(paperCost -> {
                            log.logEndingProcess(processName);
                            return PnPaperChannelCostMapper.toDTO(tender, paperCost);
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
