package it.pagopa.pn.paperchannel;

import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PnPaperChannelConfigTest {
    private String clientSafeStorageBasepath;
    private String clientNationalRegistriesBasepath;
    private String clientExternalChannelBasepath;
    private String clientDataVaultBasepath;
    private String safeStorageCxId;
    private String xPagopaExtchCxId;
    private String nationalRegistryCxId;
    private String queueDeliveryPush;
    private String queueNationalRegistries;
    private String queueExternalChannel;
    private String queueInternal;
    private Integer attemptSafeStorage;
    private Integer attemptQueueSafeStorage;
    private Integer attemptQueueExternalChannel;
    private Integer attemptQueueNationalRegistries;
    private Long ttlPrepare;
    private Long ttlExecutionN_890;
    private Long ttlExecutionN_AR;
    private Long ttlExecutionN_RS;
    private Long ttlExecutionI_AR;
    private Long ttlExecutionI_RS;
    private Long ttlExecutionDaysMeta;
    private Long ttlExecutionDaysDemat;
    private Integer paperWeight;
    private Integer letterWeight;
    private String retryStatus;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void setGetTest() {
        PnPaperChannelConfig pnPaperChannelConfig = initPnPaperChannelConfig();
        Assertions.assertNotNull(pnPaperChannelConfig);
        Assertions.assertEquals(queueNationalRegistries, pnPaperChannelConfig.getQueueNationalRegistries());
        Assertions.assertEquals(queueExternalChannel, pnPaperChannelConfig.getQueueExternalChannel());
        Assertions.assertEquals(ttlExecutionN_890, pnPaperChannelConfig.getTtlExecutionN_890());
        Assertions.assertEquals(ttlExecutionN_RS, pnPaperChannelConfig.getTtlExecutionN_RS());
        Assertions.assertEquals(ttlExecutionI_RS, pnPaperChannelConfig.getTtlExecutionI_RS());
    }

    @Test
    void toStringTest() {
        PnPaperChannelConfig pnPaperChannelConfig = initPnPaperChannelConfig();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnPaperChannelConfig.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("clientSafeStorageBasepath=");
        stringBuilder.append(clientSafeStorageBasepath);
        stringBuilder.append(", ");
        stringBuilder.append("clientNationalRegistriesBasepath=");
        stringBuilder.append(clientNationalRegistriesBasepath);
        stringBuilder.append(", ");
        stringBuilder.append("clientExternalChannelBasepath=");
        stringBuilder.append(clientExternalChannelBasepath);
        stringBuilder.append(", ");
        stringBuilder.append("clientDataVaultBasepath=");
        stringBuilder.append(clientDataVaultBasepath);
        stringBuilder.append(", ");
        stringBuilder.append("safeStorageCxId=");
        stringBuilder.append(safeStorageCxId);
        stringBuilder.append(", ");
        stringBuilder.append("xPagopaExtchCxId=");
        stringBuilder.append(xPagopaExtchCxId);
        stringBuilder.append(", ");
        stringBuilder.append("nationalRegistryCxId=");
        stringBuilder.append(nationalRegistryCxId);
        stringBuilder.append(", ");
        stringBuilder.append("queueDeliveryPush=");
        stringBuilder.append(queueDeliveryPush);
        stringBuilder.append(", ");
        stringBuilder.append("queueNationalRegistries=");
        stringBuilder.append(queueNationalRegistries);
        stringBuilder.append(", ");
        stringBuilder.append("queueExternalChannel=");
        stringBuilder.append(queueExternalChannel);
        stringBuilder.append(", ");
        stringBuilder.append("queueInternal=");
        stringBuilder.append(queueInternal);
        stringBuilder.append(", ");
        stringBuilder.append("attemptSafeStorage=");
        stringBuilder.append(attemptSafeStorage);
        stringBuilder.append(", ");
        stringBuilder.append("attemptQueueSafeStorage=");
        stringBuilder.append(attemptQueueSafeStorage);
        stringBuilder.append(", ");
        stringBuilder.append("attemptQueueExternalChannel=");
        stringBuilder.append(attemptQueueExternalChannel);
        stringBuilder.append(", ");
        stringBuilder.append("attemptQueueNationalRegistries=");
        stringBuilder.append(attemptQueueNationalRegistries);
        stringBuilder.append(", ");
        stringBuilder.append("ttlPrepare=");
        stringBuilder.append(ttlPrepare);
        stringBuilder.append(", ");
        stringBuilder.append("ttlExecutionN_890=");
        stringBuilder.append(ttlExecutionN_890);
        stringBuilder.append(", ");
        stringBuilder.append("ttlExecutionN_AR=");
        stringBuilder.append(ttlExecutionN_AR);
        stringBuilder.append(", ");
        stringBuilder.append("ttlExecutionN_RS=");
        stringBuilder.append(ttlExecutionN_RS);
        stringBuilder.append(", ");
        stringBuilder.append("ttlExecutionI_AR=");
        stringBuilder.append(ttlExecutionI_AR);
        stringBuilder.append(", ");
        stringBuilder.append("ttlExecutionI_RS=");
        stringBuilder.append(ttlExecutionI_RS);
        stringBuilder.append(", ");
        stringBuilder.append("ttlExecutionDaysMeta=");
        stringBuilder.append(ttlExecutionDaysMeta);
        stringBuilder.append(", ");
        stringBuilder.append("ttlExecutionDaysDemat=");
        stringBuilder.append(ttlExecutionDaysDemat);
        stringBuilder.append(", ");
        stringBuilder.append("paperWeight=");
        stringBuilder.append(paperWeight);
        stringBuilder.append(", ");
        stringBuilder.append("letterWeight=");
        stringBuilder.append(letterWeight);
        stringBuilder.append(", ");
        stringBuilder.append("retryStatus=");
        stringBuilder.append(retryStatus);
        stringBuilder.append(")");
        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnPaperChannelConfig.toString());
    }

    private PnPaperChannelConfig initPnPaperChannelConfig() {
        PnPaperChannelConfig pnPaperChannelConfig = new PnPaperChannelConfig();
        pnPaperChannelConfig.setClientSafeStorageBasepath(clientSafeStorageBasepath);
        pnPaperChannelConfig.setClientNationalRegistriesBasepath(clientNationalRegistriesBasepath);
        pnPaperChannelConfig.setClientExternalChannelBasepath(clientExternalChannelBasepath);
        pnPaperChannelConfig.setClientDataVaultBasepath(clientDataVaultBasepath);
        pnPaperChannelConfig.setSafeStorageCxId(safeStorageCxId);
        pnPaperChannelConfig.setXPagopaExtchCxId(xPagopaExtchCxId);
        pnPaperChannelConfig.setNationalRegistryCxId(nationalRegistryCxId);
        pnPaperChannelConfig.setQueueDeliveryPush(queueDeliveryPush);
        pnPaperChannelConfig.setQueueNationalRegistries(queueNationalRegistries);
        pnPaperChannelConfig.setQueueExternalChannel(queueExternalChannel);
        pnPaperChannelConfig.setQueueInternal(queueInternal);
        pnPaperChannelConfig.setAttemptSafeStorage(attemptSafeStorage);
        pnPaperChannelConfig.setAttemptQueueSafeStorage(attemptQueueSafeStorage);
        pnPaperChannelConfig.setAttemptQueueExternalChannel(attemptQueueExternalChannel);
        pnPaperChannelConfig.setAttemptQueueNationalRegistries(attemptQueueNationalRegistries);
        pnPaperChannelConfig.setTtlPrepare(ttlPrepare);
        pnPaperChannelConfig.setTtlExecutionN_890(ttlExecutionN_890);
        pnPaperChannelConfig.setTtlExecutionN_AR(ttlExecutionN_AR);
        pnPaperChannelConfig.setTtlExecutionN_RS(ttlExecutionN_RS);
        pnPaperChannelConfig.setTtlExecutionI_AR(ttlExecutionI_AR);
        pnPaperChannelConfig.setTtlExecutionI_RS(ttlExecutionI_RS);
        pnPaperChannelConfig.setTtlExecutionDaysMeta(ttlExecutionDaysMeta);
        pnPaperChannelConfig.setTtlExecutionDaysDemat(ttlExecutionDaysDemat);
        pnPaperChannelConfig.setPaperWeight(paperWeight);
        pnPaperChannelConfig.setLetterWeight(letterWeight);
        pnPaperChannelConfig.setRetryStatus(retryStatus);
        return pnPaperChannelConfig;
    }

    private void initialize() {
        clientSafeStorageBasepath = "clientSafeStorageBasepath";
        clientNationalRegistriesBasepath = "clientNationalRegistriesBasepath";
        clientExternalChannelBasepath = "clientExternalChannelBasepath";
        clientDataVaultBasepath = "clientDataVaultBasepath";
        safeStorageCxId = "safeStorageCxId";
        xPagopaExtchCxId = "xPagopaExtchCxId";
        nationalRegistryCxId = "nationalRegistryCxId";
        queueDeliveryPush = "queueDeliveryPush";
        queueExternalChannel = "queueExternalChannel";
        queueInternal = "queueInternal";
        attemptSafeStorage = 1;
        attemptQueueSafeStorage = 2;
        attemptQueueExternalChannel = 3;
        attemptQueueNationalRegistries = 4;
        ttlPrepare = 0L;
        ttlExecutionN_890 = 1L;
        ttlExecutionN_AR = 2L;
        ttlExecutionN_RS = 3L;
        ttlExecutionI_AR = 4L;
        ttlExecutionI_RS = 5L;
        ttlExecutionDaysMeta = 6L;
        ttlExecutionDaysDemat = 7L;
        paperWeight = 5;
        letterWeight = 6;
        retryStatus = "retryStatus";
    }
}
