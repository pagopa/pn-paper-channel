package it.pagopa.pn.paperchannel.dao.model;

import lombok.Getter;
import lombok.Setter;

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
    private String pec;
    @ColumnExcel("FISCAL_CODE")
    private String fiscalCode;
    @ColumnExcel("TAX_ID")
    private String taxId;
    @ColumnExcel("PHONE_NUMBER")
    private String phoneNumber;
    @ColumnExcel("UNIQUE_CODE")
    private String uniqueCode;
    @ColumnExcel("FSU")
    private Boolean fsu;
    @ColumnExcel("CAP")
    public String cap;
    @ColumnExcel("ZONE")
    public String zone;
    @ColumnExcel("PRODUCT_TYPE")
    public String productType;
    @ColumnExcel("BASE_PRICE")
    public Float basePrice;
    @ColumnExcel("PAGE_PRICE")
    public Float pagePrice;



}
