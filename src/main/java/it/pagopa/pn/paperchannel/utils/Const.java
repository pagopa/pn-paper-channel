package it.pagopa.pn.paperchannel.utils;

public class Const {

    public static final String PN_PAPER_CHANNEL = "PN-PAPER-CHANNEL";
    public static final String PN_AAR = "PN_AAR";
    public static final String RACCOMANDATA_SEMPLICE = "RS";
    public static final String RACCOMANDATA_890 = "890";
    public static final String RACCOMANDATA_AR = "AR";
    public static final String ZONA_1 = "ZONA_1";
    public static final String ZONA_2 = "ZONA_2";
    public static final String ZONA_3 = "ZONA_3";
    public static final String CAP_DEFALUT = "99999";
    public static final String ZONE_DEFAULT = "zone_default";
    public static final String capRegex = "(\\d{5})(-\\d{5})?+";
    public static final String zoneRegex = "^ZONE_[1-9]$";
    public static final String taxIdRegex = "^[0-9]{11}$";
    public static final String uniqueCodeRegex = "^[A-Za-z0-9~\\-_]{7}$";
    public static final String phoneNumberRegex = "^[\\+]?[(]?[0-9]{1,3}[)]?[!@#$%&]{0,}[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{3,7}$";
    public static final String fiscalCodeRegex = "^[a-zA-Z0-9-]+$";
    public static final Integer maxElements = 10;

    public static final String EXECUTION = "EXECUTION";
    public static final String PREPARE = "PREPARE";
    public static final String RETRY = ";PCRETRY_";
}
