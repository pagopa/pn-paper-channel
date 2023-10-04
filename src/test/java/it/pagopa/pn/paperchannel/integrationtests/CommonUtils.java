package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;

import java.time.OffsetDateTime;

public class CommonUtils {
    public static PaperProgressStatusEventDto createSimpleAnalogMail() {
        PaperProgressStatusEventDto analogMail = new PaperProgressStatusEventDto();
        analogMail.requestId("PREPARE_ANALOG_DOMICILE.IUN_MUMR-VQMP-LDNZ-202303-H-1.RECINDEX_0.SENTATTEMPTMADE_0");
        analogMail.setClientRequestTimeStamp(OffsetDateTime.now());
        analogMail.setStatusDateTime(OffsetDateTime.now());
        analogMail.setStatusCode("RECRS002C");
        analogMail.setProductType("RS");
        analogMail.setStatusDescription("OK");

        return analogMail;
    }

    public static PaperProgressStatusEventDto createSimpleAnalogMail(String iun) {
        PaperProgressStatusEventDto analogMail = new PaperProgressStatusEventDto();
        analogMail.requestId("PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.SENTATTEMPTMADE_0");
        analogMail.setClientRequestTimeStamp(OffsetDateTime.now());
        analogMail.setStatusDateTime(OffsetDateTime.now());
        analogMail.setStatusCode("RECRS002C");
        analogMail.setProductType("RS");
        analogMail.setStatusDescription("OK");

        return analogMail;
    }

    public static PnDeliveryRequest createPnDeliveryRequest() {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_KREP-VHAD-TAQV-202302-P-1.RECINDEX_0.SENTATTEMPTMADE_1");
        pnDeliveryRequest.setCorrelationId("Self=1-63fe1166-09f74e174d4e13d26f7d08c0;Root=1-63fe1166-cdf14290b52666124be856be;Parent=a3bb560233ceb4ec;Sampled=1");
        pnDeliveryRequest.setFiscalCode("PF-a6c1350d-1d69-4209-8bf8-31de58c79d6e");
        pnDeliveryRequest.setHashedFiscalCode("81af12154dfaf8094715acadc8065fdde56c31fb52a9d1766f8f83470262c13a");
        pnDeliveryRequest.setHashOldAddress("60cba8d6dda57ac74ec15e5a4b78402672883ecdffdb01d1f19501cba176f7254b803f38a0359c42d8fe8459d0a6ecac8ca9e7539a64df346290c966dc9845444dee871c93f2d2d33a691daa7a5c75b10f504efc91a03dcb3882744f9");
        pnDeliveryRequest.setIun("KREP-VHAD-TAQV-202302-P-1");
        pnDeliveryRequest.setPrintType("BN_FRONTE_RETRO");
        pnDeliveryRequest.setProposalProductType("890");
        pnDeliveryRequest.setReceiverType("PF");
        pnDeliveryRequest.setRelatedRequestId("PREPARE_ANALOG_DOMICILE.IUN_KREP-VHAD-TAQV-202302-P-1.RECINDEX_0.SENTATTEMPTMADE_0");
        pnDeliveryRequest.setStartDate("2023-02-28T15:36:22.225");
        pnDeliveryRequest.setStatusCode("PROGRESS");
        pnDeliveryRequest.setStatusDate("2023-02-28T15:36:22.29");
        pnDeliveryRequest.setStatusDetail("In attesa di indirizzo da National Registry");

        return pnDeliveryRequest;
    }

    public static PnDeliveryRequest createPnDeliveryRequest(String iun) {
        PnDeliveryRequest pnDeliveryRequest = new PnDeliveryRequest();
        pnDeliveryRequest.setRequestId("PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.SENTATTEMPTMADE_1");
        pnDeliveryRequest.setCorrelationId("Self=1-63fe1166-09f74e174d4e13d26f7d08c0;Root=1-63fe1166-cdf14290b52666124be856be;Parent=a3bb560233ceb4ec;Sampled=1");
        pnDeliveryRequest.setFiscalCode("PF-a6c1350d-1d69-4209-8bf8-31de58c79d6e");
        pnDeliveryRequest.setHashedFiscalCode("81af12154dfaf8094715acadc8065fdde56c31fb52a9d1766f8f83470262c13a");
        pnDeliveryRequest.setHashOldAddress("60cba8d6dda57ac74ec15e5a4b78402672883ecdffdb01d1f19501cba176f7254b803f38a0359c42d8fe8459d0a6ecac8ca9e7539a64df346290c966dc9845444dee871c93f2d2d33a691daa7a5c75b10f504efc91a03dcb3882744f9");
        pnDeliveryRequest.setIun("KREP-VHAD-TAQV-202302-P-1");
        pnDeliveryRequest.setPrintType("BN_FRONTE_RETRO");
        pnDeliveryRequest.setProposalProductType("890");
        pnDeliveryRequest.setReceiverType("PF");
        pnDeliveryRequest.setRelatedRequestId("PREPARE_ANALOG_DOMICILE.IUN_KREP-VHAD-TAQV-202302-P-1.RECINDEX_0.SENTATTEMPTMADE_0");
        pnDeliveryRequest.setStartDate("2023-02-28T15:36:22.225");
        pnDeliveryRequest.setStatusCode("PROGRESS");
        pnDeliveryRequest.setStatusDate("2023-02-28T15:36:22.29");
        pnDeliveryRequest.setStatusDetail("In attesa di indirizzo da National Registry");

        return pnDeliveryRequest;
    }
}
