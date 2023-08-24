package it.pagopa.pn.paperchannel.dao.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class DeliveryAndCost {

    @ColumnExcel("DENOMINATION")
    private String denomination;

    @ColumnExcel("BUSINESS_NAME")
    private String businessName;
    @ColumnExcel("OFFICE_NAME")
    private String registeredOffice;
    @ColumnExcel("PEC")
    @ToString.Exclude
    private String pec;
    @ColumnExcel("FISCAL_CODE")
    @ToString.Exclude
    private String fiscalCode;
    @ColumnExcel("TAX_ID")
    @ToString.Exclude
    private String taxId;
    @ColumnExcel("PHONE_NUMBER")
    @ToString.Exclude
    private String phoneNumber;
    @ColumnExcel("UNIQUE_CODE")
    private String uniqueCode;
    @ColumnExcel("FSU")
    private Boolean fsu;
    @ColumnExcel("CAP")
    @ToString.Exclude
    private List<String> caps;
    @ColumnExcel("ZONE")
    @ToString.Exclude
    private String zone;
    @ColumnExcel("PRODUCT_TYPE")
    private String productType;
    @ColumnExcel("BASE_PRICE")
    private BigDecimal basePrice;
    @ColumnExcel("PAGE_PRICE")
    private BigDecimal pagePrice;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryAndCost that = (DeliveryAndCost) o;
        boolean controlValue = taxId.equals(that.taxId) && uniqueCode.equals(that.uniqueCode) && fsu.equals(that.fsu) && productType.equals(that.productType);
        boolean thisNational = (this.caps != null);
        boolean thatNational = (that.caps != null);
        if (!thisNational && !thatNational){
            return controlValue && StringUtils.equals(this.zone, that.zone);
        }
        return controlValue && (thisNational == thatNational);
    }

    @Override
    public int hashCode() {
        if (this.caps != null)
            return Objects.hash(taxId, uniqueCode, fsu, "NATIONAL", productType);
        return Objects.hash(taxId, uniqueCode, fsu, "INTERNATIONAL", productType);
    }
}
