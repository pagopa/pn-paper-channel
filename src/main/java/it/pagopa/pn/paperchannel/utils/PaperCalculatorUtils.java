package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.CostDTO;
import it.pagopa.pn.paperchannel.utils.costutils.CostRanges;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.ProductTypeEnum;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.AttachmentInfo;
import it.pagopa.pn.paperchannel.service.PaperTenderService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static it.pagopa.pn.paperchannel.utils.Const.*;

@Component
@CustomLog
@AllArgsConstructor
public class PaperCalculatorUtils {

    private static final String COUNTRY_IT = "it";
    private static final String COUNTRY_ITALIA = "italia";
    private static final String COUNTRY_ITALY = "italy";

    private final PaperTenderService paperTenderService;
    private final PnPaperChannelConfig pnPaperChannelConfig;

    public Mono<BigDecimal> calculator(List<AttachmentInfo> attachments, Address address, ProductTypeEnum productType, boolean isReversePrinter){
        boolean isNational = StringUtils.isBlank(address.getCountry()) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_IT) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_ITALIA) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_ITALY);

        if (StringUtils.isNotBlank(address.getCap()) && isNational) {
            return getAmount(attachments, address.getCap(), null, getProductType(address, productType), isReversePrinter)
                    .map(item -> item);
        }
        return paperTenderService.getZoneFromCountry(address.getCountry())
                .flatMap(zone -> getAmount(attachments,null, zone, getProductType(address, productType), isReversePrinter).map(item -> item));

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
     * @return calcolo del costp della notifica
     */
    private Mono<BigDecimal> getAmount(List<AttachmentInfo> attachments, String cap, String zone, String productType, boolean isReversePrinter){
        String processName = "Get Amount";
        log.logStartingProcess(processName);
        return paperTenderService.getCostFrom(cap, zone, productType)
                .map(contract ->
                    switch (pnPaperChannelConfig.getChargeCalculationMode()) {
                        case AAR -> contract.getPrice();
                        case COMPLETE -> getPriceForCOMPLETEMode(attachments, contract, isReversePrinter);
                    }
                )
                .doOnNext(totalCost -> log.logEndingProcess(processName));
    }

    private BigDecimal getPriceForCOMPLETEMode(List<AttachmentInfo> attachments, CostDTO costDTO, boolean isReversePrinter){
        Integer totPages = getNumberOfPages(attachments, isReversePrinter, false);
        int totPagesWight = getLetterWeight(totPages);
        BigDecimal basePriceForWeight = CostRanges.getBasePriceForWeight(costDTO, totPagesWight);
        BigDecimal priceTotPages = costDTO.getPriceAdditional().multiply(BigDecimal.valueOf(totPages));
        BigDecimal completedPrice = basePriceForWeight.add(priceTotPages);
        log.info("Calculating cost COMPLETE mode, totPages={}, totPagesWight={}, basePriceForWeight={}, priceTotPages={}, completedPrice={}, costDTO: {}",
                totPages, totPagesWight, basePriceForWeight, priceTotPages, completedPrice, costDTO);
        return completedPrice;
    }


    public int getLetterWeight(int numberOfPages){
        int weightPaper = this.pnPaperChannelConfig.getPaperWeight();
        int weightLetter = this.pnPaperChannelConfig.getLetterWeight();
        return (weightPaper * numberOfPages) + weightLetter;
    }



    public Integer getNumberOfPages(List<AttachmentInfo> attachments, boolean isReversePrinter, boolean ignoreAAR){
        if (attachments == null || attachments.isEmpty()) return 0;
        return attachments.stream().map(attachment -> {
            int numberOfPages = attachment.getNumberOfPage();
            if (isReversePrinter) numberOfPages = (int) Math.ceil(((double) attachment.getNumberOfPage())/2);
            return (!ignoreAAR && StringUtils.equals(attachment.getDocumentType(), Const.PN_AAR)) ? numberOfPages-1 : numberOfPages;
        }).reduce(0, Integer::sum);
    }




    protected String getProductType(Address address, ProductTypeEnum productTypeEnum){
        String productType = "";
        boolean isNational = StringUtils.isBlank(address.getCountry()) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_IT) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_ITALIA) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_ITALY);

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
        boolean isNational = StringUtils.isBlank(address.getCountry()) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_IT) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_ITALIA) ||
                StringUtils.equalsIgnoreCase(address.getCountry(), COUNTRY_ITALY);
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
}
