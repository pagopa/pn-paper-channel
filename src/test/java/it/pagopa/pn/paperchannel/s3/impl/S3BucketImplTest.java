package it.pagopa.pn.paperchannel.s3.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import it.pagopa.pn.paperchannel.config.AwsBucketProperties;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.model.PrepareAsyncRequest;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.PREPARE_ASYNC_LISTENER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.*;

class S3BucketImplTest extends BaseTest {

    @MockBean
    AmazonS3 s3Client;
    @Mock
    S3Object s3Object;
    @Mock
    S3ObjectInputStream inputStream;
    @Autowired
    @SpyBean
    AwsBucketProperties awsBucketProperties;

    @Autowired
    S3Bucket s3Bucket;

    @BeforeEach
    void setUp() throws IOException {
        Mockito.when(this.s3Client.getObject(Mockito.any())).thenReturn(this.s3Object);
        Mockito.when(this.s3Object.getObjectContent()).thenReturn(this.inputStream);
        Mockito.when(this.inputStream.getDelegateStream()).thenReturn(null);
        Mockito.when(this.inputStream.readAllBytes()).thenReturn(null);
    }

    @Test
    void presignedUrlTest() throws MalformedURLException {
        Mockito.when(this.s3Client.generatePresignedUrl(Mockito.any())).thenReturn(new URL("https://www.computerhope.com"));
        this.s3Bucket.presignedUrl().block();
    }

    @Test
    void putObjectOkTest(){
        Mockito.when(this.s3Client.putObject(Mockito.any())).thenReturn(new PutObjectResult());
        this.s3Bucket.putObject(new File("pippo.txt"));
    }

    @Test
    void putObjectErrorTest(){
        Mockito.when(this.s3Client.putObject(Mockito.any())).thenThrow(new AmazonClientException("ERROR"));
        this.s3Bucket.putObject(new File("pippo.txt"));

    }

   @Test
    void getObjectDataOkTest() throws IOException {
        String file = "pippo.txt";
        Assertions.assertNull(this.s3Bucket.getObjectData(file));
    }

    @Test
    void getObjectDataErrorTest() throws IOException {
        Mockito.when(this.inputStream.readAllBytes()).thenThrow(new IOException());
        this.s3Bucket.getObjectData("pippo.txt");
    }

    @Test
    void getObjectDataNullTest(){
        String file = "pippo.txt";
        Mockito.when(this.s3Client.getObject(Mockito.any())).thenReturn(null);
        Assertions.assertNull(this.s3Bucket.getObjectData(file));
    }

    @Test
    void getFileInputStreamTest(){
        String file = "pippo.txt";
        Assertions.assertNull(this.s3Bucket.getFileInputStream(file));
    }
    @Test
    void getFileInputStreamNullTest(){
        String file = "pippo.txt";
        Mockito.when(this.s3Client.getObject(Mockito.any())).thenReturn(null);
        Assertions.assertNull(this.s3Bucket.getFileInputStream(file));
    }
}