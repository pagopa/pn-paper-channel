package it.pagopa.pn.paperchannel.utils;

public class Const {

    public static final String PN_PAPER_CHANNEL = "PN-PAPER-CHANNEL";
    public static final String PN_AAR = "PN_AAR";
    public static final String AAR = "AAR";
    public static final String ATTO = "ATTO";
    public static final String BN_FRONTE_RETRO = "BN_FRONTE_RETRO";
    public static final String RACCOMANDATA_SEMPLICE = "RS";
    public static final String RACCOMANDATA_890 = "890";
    public static final String RACCOMANDATA_AR = "AR";
    public static final String RACCOMANDATA_RIR = "RIR";
    public static final String RACCOMANDATA_RIS = "RIS";
    public static final String ZONE_1 = "ZONE_1";
    public static final String ZONE_2 = "ZONE_2";
    public static final String ZONE_3 = "ZONE_3";
    public static final String CAP_DEFAULT = "99999";
    public static final String ZONE_DEFAULT = "zone_default";
    public static final String capRegex = "(\\d{5})?+";
    public static final String zoneRegex = "^ZONE_[1-9]$";
    public static final String taxIdRegex = "^[0-9]{11}$";
    public static final String pecRegex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\\\.[A-Za-z0-9-]+)*(\\\\.[A-Za-z]{2,})$";
    public static final String uniqueCodeRegex = "^[A-Za-z0-9~\\-_]{7}$";
    public static final String phoneNumberRegex = "^[\\+]?[(]?[0-9]{1,3}[)]?[!@#$%&]{0,}[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{3,7}$";
    public static final String fiscalCodeRegex = "^([A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST]{1}[0-9LMNPQRSTUV]{2}[A-Z]{1}[0-9LMNPQRSTUV]{3}[A-Z]{1})$";
    public static final Integer maxElements = 10;
    public static final Integer maxErrorsElements = 5;

    public static final String EXECUTION = "EXECUTION";
    public static final String PREPARE = "PREPARE";
    public static final String RETRY = ".PCRETRY_";
    public static final String PAPERSEND = "PAPERSEND";
    public static final String DISCARDNOTIFICATION = "DISCARDNOTIFICATION";
    public static final String PREFIX_REQUEST_ID_SERVICE_DESK = "SERVICE_DESK_OPID-";
    public static final String HEADER_CLIENT_ID = "x-pagopa-paperchannel-cx-id";
    public static final String CONTEXT_KEY_CLIENT_ID = "CLIENT_ID";
    public static final String CONTEXT_KEY_PREFIX_CLIENT_ID = "PREFIX_CLIENT_ID";

    public static final String DOCUMENT_TYPE_F24_SET = "PN_F24_SET";

    /* Demat document type constants */
    public static final String DEMAT_AR = "AR";
    public static final String DEMAT_ARCAD = "ARCAD";
    public static final String DEMAT_CAD = "CAD";
    public static final String DEMAT_23L = "23L";
    public static final String DEMAT_PLICO = "Plico";
    public static final String DEMAT_INDAGINE = "Indagine";

}
