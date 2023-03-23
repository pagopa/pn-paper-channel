package it.pagopa.pn.paperchannel.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileInfoTest {

    private byte[] data;
    private String url;

    @BeforeEach
    void setUp(){
        this.initialize();
    }
    @Test
    void setGetTest() {
        FileInfo fileInfo = initFileInfo();
        Assertions.assertNotNull(fileInfo);
        Assertions.assertEquals(data, fileInfo.getData());
        Assertions.assertEquals(url, fileInfo.getUrl());

        byte[] data = new byte[20];
        String url = "url";

        fileInfo.setData(data);
        fileInfo.setUrl(url);

        Assertions.assertEquals(data, fileInfo.getData());
        Assertions.assertEquals(url, fileInfo.getUrl());
    }

    private FileInfo initFileInfo() {
        return new FileInfo(data, url);
    }

    private void initialize() {
        data = new byte[10];
        url = "";
    }
}
