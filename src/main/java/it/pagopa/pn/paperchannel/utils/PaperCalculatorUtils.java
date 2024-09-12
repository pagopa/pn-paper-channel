package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateRequest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ShipmentCalculateResponse;
import it.pagopa.pn.paperchannel.model.PnPaperChannelCostDTO;
import it.pagopa.pn.paperchannel.utils.costutils.CostRanges;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import it.pagopa.pn.paperchannel.utils.costutils.CostWithDriver;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import static it.pagopa.pn.paperchannel.utils.Const.*;


@Component
@CustomLog
@AllArgsConstructor
public class PaperCalculatorUtils {
    private final PaperTenderService paperTenderService;
    private final PnPaperChannelConfig pnPaperChannelConfig;
    private final DateChargeCalculationModesUtils chargeCalculationModeUtils;


    public Mono<CostWithDriver> calculator(List<AttachmentInfo> attachments, Address address, ProductTypeEnum productType, boolean isReversePrinter){
        boolean isNational = Utility.isNational(address.getCountry());

        if(pnPaperChannelConfig.isEnableSimplifiedTenderFlow()) {
            log.info("SimplifiedTenderFlow");
            return paperTenderService.getSimplifiedCost(address.getCap(), address.getCountry(), productType.getValue())
                    .map(contract -> getSimplifiedCostWithDriver(contract, attachments, productType.getValue(), isReversePrinter));
        }
        log.info("OldTenderFlow");

        if (StringUtils.isNotBlank(address.getCap()) && isNational) {
            return getAmount(attachments, address.getCap(), null, getProductType(address, productType), isReversePrinter)
                    .map(item -> item);
        }
        return paperTenderService.getZoneFromCountry(address.getCountry())
                .flatMap(zone -> getAmount(attachments,null, zone, getProductType(address, productType), isReversePrinter).map(item -> item));
    }

    /**
     * Retrieve simulation cost of notification through parameters specified into request
     *
     * @param tenderId  the id of a tender
     * @param request   request containing all parameters needs to calculate cost
     *
     * @return          response containing the final cost provided by simulation
     **/
    public Mono<ShipmentCalculateResponse> costSimulator(String tenderId, ShipmentCalculateRequest request) {
        String geokey = request.getGeokey();
        return paperTenderService.getCostFromTenderId(tenderId, geokey, request.getProduct().getValue())
                .map(costDTO -> getCostSimulated(costDTO, request.getNumSides(), request.getPageWeight(), request.getProduct().getValue(), request.getIsReversePrinter()))
                .map(cost -> {
                    ShipmentCalculateResponse response = new ShipmentCalculateResponse();
                    response.setCost(cost.multiply(BigDecimal.valueOf(100)).intValue());
                    return response;
                });
    }

    /**
     * Provides parameters needs to getSimplifiedAmount() method for completing cost calculation
     *
     * @param contract          contract from which costs are calculated
     * @param numSides          number of faces that make up the document
     * @param pageWeight        weight of the single sheet
     * @param productType       type of product based on address (AR, 890, etc)
     * @param isReversePrinter  true if print with BN_FRONTE_RETRO
     *
     * @return                  the amount final cost of notification
     **/
    private BigDecimal getCostSimulated(PnPaperChannelCostDTO contract, Integer numSides, Integer pageWeight, String productType, boolean isReversePrinter) {
        var attachments = getAttachments(numSides);
        Integer numbersPage = getNumberOfPages(attachments, isReversePrinter, true);
        Integer finalPageWeight = pageWeight == null ? pnPaperChannelConfig.getPaperWeight() : pageWeight;
        int totPagesWeight = getLetterWeight(numbersPage, finalPageWeight, pnPaperChannelConfig.getLetterWeight());
        return getSimplifiedAmount(totPagesWeight, numbersPage, contract, productType);
    }

    /**
     * Algoritmo del calcolo del costo per modalità COMPLETE:
     * <b>${prezzo base scaglione di peso} + ( (${numero di pagine}-1) * ${prezzo pagina aggiuntiva} )</b>
     *
     * @param attachments lista di allegati della notifica
     * @param cap di spedizione (null se la spedizione è estera)
     * @param zone eventuale zona recuperata dalla country (è valorizzato a null se è presente un CAP ed è italiano)
     * @param productType tipo di prodotto (AR, 890, etc)
     * @param isReversePrinter true, se il printType della request della SEND è di tipo BN_FRONTE_RETRO
     *
     * @return calcolo del costo della notifica con le informazioni del recapitista
     */
    private Mono<CostWithDriver> getAmount(List<AttachmentInfo> attachments, String cap, String zone, String productType, boolean isReversePrinter){
        String processName = "Get Amount";
        log.logStartingProcess(processName);

        return paperTenderService.getCostFrom(cap, zone, productType)
                .map(contract -> getCostWithDriver(contract, attachments, isReversePrinter))
                .doOnNext(totalCost -> log.logEndingProcess(processName));
    }

    /**
     * Retrieve the notification cost with the driver information
     *
     * @param contract          contract from which costs are calculated
     * @param attachments       notification attachments for which calculate pages and costs
     * @param productType       type of product based on address (AR, 890, etc)
     * @param isReversePrinter  true if print with BN_FRONTE_RETRO
     *
     * @return                  the amount cost of notification with driver information
     * */
    private CostWithDriver getSimplifiedCostWithDriver(PnPaperChannelCostDTO contract, List<AttachmentInfo> attachments, String productType, boolean isReversePrinter) {
        ChargeCalculationModeEnum calculationModeEnum = chargeCalculationModeUtils.getChargeCalculationMode();
        log.debug("calculationMode found: {}", calculationModeEnum);

        BigDecimal amount = calculationModeEnum.equals(ChargeCalculationModeEnum.AAR)
                ? contract.getBasePriceAR()
                : getSimplifiedPriceForCOMPLETEMode(attachments, contract, productType, isReversePrinter);

        return CostWithDriver.builder()
                .cost(amount)
                .driverCode(contract.getDeliveryDriverId())
                .tenderCode(contract.getTenderId())
                .build();
    }

    /**
     * Retrieve the notification cost with the driver information
     *
     * @param contract          contract from which costs are calculated
     * @param attachments       notification attachments for which calculate pages and costs
     * @param isReversePrinter  true if print with BN_FRONTE_RETRO
     *
     * @return                  the amount cost of notification with driver information
     * */
    private CostWithDriver getCostWithDriver(CostDTO contract, List<AttachmentInfo> attachments, boolean isReversePrinter) {
        ChargeCalculationModeEnum calculationModeEnum = chargeCalculationModeUtils.getChargeCalculationMode();
        log.debug("calculationMode found: {}", calculationModeEnum);

        BigDecimal amount = calculationModeEnum.equals(ChargeCalculationModeEnum.AAR)
                ? contract.getPrice()
                : getPriceForCOMPLETEMode(attachments, contract, isReversePrinter);

        return CostWithDriver.builder()
                .cost(amount)
                .driverCode(contract.getDriverCode())
                .tenderCode(contract.getTenderCode())
                .build();
    }

    private BigDecimal getPriceForCOMPLETEMode(List<AttachmentInfo> attachments, CostDTO costDTO, boolean isReversePrinter){
        Integer totPagesIgnoringAAR = getNumberOfPages(attachments, isReversePrinter, false);
        Integer totPages = getNumberOfPages(attachments, isReversePrinter, true);
        int totPagesWight = getLetterWeight(totPages, pnPaperChannelConfig.getPaperWeight(), pnPaperChannelConfig.getLetterWeight());
        BigDecimal basePriceForWeight = CostRanges.getBasePriceForWeight(costDTO, totPagesWight);
        BigDecimal priceTotPages = costDTO.getPriceAdditional().multiply(BigDecimal.valueOf(totPagesIgnoringAAR));
        BigDecimal completedPrice = basePriceForWeight.add(priceTotPages);
        log.info("Calculating cost COMPLETE mode, totPages={}, totPagesWight={}, basePriceForWeight={}, priceTotPages={}, completedPrice={}, costDTO: {}",
                totPages, totPagesWight, basePriceForWeight, priceTotPages, completedPrice, costDTO);
        return completedPrice;
    }

    private BigDecimal getSimplifiedPriceForCOMPLETEMode(List<AttachmentInfo> attachments, PnPaperChannelCostDTO costDTO, String productType,  boolean isReversePrinter) {
        Integer totPages = getNumberOfPages(attachments, isReversePrinter, true);
        int totPagesWeight = getLetterWeight(totPages, pnPaperChannelConfig.getPaperWeight(), pnPaperChannelConfig.getLetterWeight());
        int totPlicoWeight = totPagesWeight + pnPaperChannelConfig.getLetterWeight();
        return getSimplifiedAmount(totPlicoWeight, totPages, costDTO, productType);
    }

    /**
     * Applies formula to calculate complete cost of notification
     *
     * @param totPlicoWeight       amount of total weight of plico
     * @param totPages             amount of total pages
     * @param contract             contract from which costs are calculated
     * @param productType          type of product based on address (AR, 890, etc)
     *
     * @return                     the amount final cost of notification
     **/
    private BigDecimal getSimplifiedAmount(Integer totPlicoWeight, Integer totPages, PnPaperChannelCostDTO contract, String productType) {
        log.info("Calculating cost Simplified COMPLETE mode, costDTO={}", contract);

        BigDecimal rangePriceFromWeight = contract.getBasePriceForWeight(totPlicoWeight);
        BigDecimal priceTotPages = contract.getPagePrice().multiply(BigDecimal.valueOf(totPages).subtract(BigDecimal.ONE));
        BigDecimal totPricePages = rangePriceFromWeight.add(priceTotPages);

        log.info("Calculating cost Simplified COMPLETE mode, totPages={}, totPlicoWeight={} rangePriceFromWeight={}, totPricePages={}, priceTotPages={}",
                totPages, totPlicoWeight, rangePriceFromWeight, priceTotPages, totPricePages);

        BigDecimal priceOfProduct = contract.getBasePriceFromProductType(productType);
        BigDecimal pricePlico = priceOfProduct.add(contract.getDematerializationCost()).add(totPricePages).add(contract.getFee());
        BigDecimal vatPlico = pricePlico.multiply(BigDecimal.valueOf(contract.getVat()/100.0)).multiply(BigDecimal.valueOf(contract.getNonDeductibleVat()/100.0));
        BigDecimal completedPrice = pricePlico.add(vatPlico);

        log.info("Calculating cost Simplified COMPLETE mode, priceOfProduct={}, pricePlico={}, vatPlico={}, completedPrice={}",
                priceOfProduct, pricePlico, vatPlico, completedPrice);
        return completedPrice.setScale(2, RoundingMode.HALF_UP);
    }

    public int getLetterWeight(int numberOfPages, int weightPaper, int weightLetter){
        return (weightPaper * numberOfPages) + weightLetter;
    }

    public Integer getNumberOfPages(List<AttachmentInfo> attachments, boolean isReversePrinter, boolean includeAAR){
        if (attachments == null || attachments.isEmpty()) return 0;
        return attachments.stream().map(attachment -> {
            int numberOfPages = attachment.getNumberOfPage();
            if (isReversePrinter) numberOfPages = (int) Math.ceil(((double) attachment.getNumberOfPage())/2);
            return (!includeAAR && StringUtils.equals(attachment.getDocumentType(), Const.PN_AAR)) ? numberOfPages-1 : numberOfPages;
        }).reduce(0, Integer::sum);
    }

    protected String getProductType(Address address, ProductTypeEnum productTypeEnum){
        String productType = "";
        boolean isNational = Utility.isNational(address.getCountry());

        if (StringUtils.isNotBlank(address.getCap()) && isNational) {
            if (productTypeEnum.equals(ProductTypeEnum.AR)) {
                productType = RACCOMANDATA_AR;
            } else if (productTypeEnum.equals(ProductTypeEnum.RS)){
                productType = RACCOMANDATA_SEMPLICE;
            } else if (productTypeEnum.equals(ProductTypeEnum._890)){
                productType = RACCOMANDATA_890;
            }
        } else {
            if (productTypeEnum.equals(ProductTypeEnum.RIR) || productTypeEnum.equals(ProductTypeEnum._890)) {
                productType = RACCOMANDATA_AR;
            } else if (productTypeEnum.equals(ProductTypeEnum.RIS)){
                productType = RACCOMANDATA_SEMPLICE;
            }
        }
        return productType;
    }

    public String getProposalProductType(Address address, String productType){
        String proposalProductType = "";
        boolean isNational = Utility.isNational(address.getCountry());
        //nazionale
        if (StringUtils.isNotBlank(address.getCap()) && isNational) {
            if(productType.equals(RACCOMANDATA_SEMPLICE)){
                proposalProductType = ProductTypeEnum.RS.getValue();
            }
            if(productType.equals(RACCOMANDATA_890)){
                proposalProductType = ProductTypeEnum._890.getValue();
            }
            if(productType.equals(RACCOMANDATA_AR)){
                proposalProductType = ProductTypeEnum.AR.getValue();
            }
        }
        //internazionale
        else {
            if(productType.equals(RACCOMANDATA_SEMPLICE)){
                proposalProductType = ProductTypeEnum.RIS.getValue();
            }
            if(productType.equals(RACCOMANDATA_AR) || productType.equals(RACCOMANDATA_890)){
                proposalProductType = ProductTypeEnum.RIR.getValue();
            }
        }
        return proposalProductType;
    }


    private List<AttachmentInfo> getAttachments(Integer numSides) {
        var attachment = new AttachmentInfo();
        attachment.setDocumentType("");
        attachment.setNumberOfPage(numSides);

        return Collections.singletonList(attachment);
    }
}
