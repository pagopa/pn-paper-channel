package it.pagopa.pn.paperchannel.middleware.db.entities;

import it.pagopa.pn.paperchannel.model.FileStatusCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PnDeliveryFileTest {

    private String uuid;
    private String status;
    private String url;
    private String filename;
    private PnErrorMessage errorMessage;

    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void toStringTest() {
        PnDeliveryFile pnDeliveryFile = initDeliveryFile();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnDeliveryFile.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("uuid=");
        stringBuilder.append(uuid);
        stringBuilder.append(", ");
        stringBuilder.append("status=");
        stringBuilder.append(status);
        stringBuilder.append(", ");
        stringBuilder.append("url=");
        stringBuilder.append(url);
        stringBuilder.append(", ");
        stringBuilder.append("filename=");
        stringBuilder.append(filename);
        stringBuilder.append(", ");
        stringBuilder.append("errorMessage=");
        stringBuilder.append(errorMessage);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnDeliveryFile.toString());
    }

    private PnDeliveryFile initDeliveryFile() {
        PnDeliveryFile pnDeliveryFile = new PnDeliveryFile();
        pnDeliveryFile.setUuid(uuid);
        pnDeliveryFile.setStatus(status);
        pnDeliveryFile.setUrl(url);
        pnDeliveryFile.setFilename(filename);
        pnDeliveryFile.setErrorMessage(errorMessage);
        return pnDeliveryFile;
    }

    private void initialize() {
        uuid = "5432106789";
        status = FileStatusCodeEnum.COMPLETE.getCode();
        url = "";
        filename = "filename";
        errorMessage = new PnErrorMessage();
    }
}
