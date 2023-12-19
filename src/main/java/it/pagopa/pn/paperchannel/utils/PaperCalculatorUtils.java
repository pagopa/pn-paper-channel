package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
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

    private Mono<BigDecimal> getAmount(List<AttachmentInfo> attachments, String cap, String zone, String productType, boolean isReversePrinter){
        String processName = "Get Amount";
        log.logStartingProcess(processName);
        return paperTenderService.getCostFrom(cap, zone, productType)
                .map(contract ->{
                    if (!pnPaperChannelConfig.getChargeCalculationMode().equalsIgnoreCase(AAR)){
                        Integer totPages = getNumberOfPages(attachments, isReversePrinter, false);
                        int totPagesWight = getLetterWeight(totPages);
                        BigDecimal basePriceForWeight = CostRanges.getBasePriceForWeight(contract, totPagesWight);
                        BigDecimal priceTotPages = contract.getPriceAdditional().multiply(BigDecimal.valueOf(totPages));
                        log.logEndingProcess(processName);
                        return basePriceForWeight.add(priceTotPages);
                    }else{
                        log.logEndingProcess(processName);
                        return contract.getPrice();
                    }
                });
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
