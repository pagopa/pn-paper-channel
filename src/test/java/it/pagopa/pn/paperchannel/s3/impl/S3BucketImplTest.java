package it.pagopa.pn.paperchannel.s3.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import it.pagopa.pn.paperchannel.config.AwsBucketProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@ExtendWith(MockitoExtension.class)
class S3BucketImplTest {

    @Mock
    AmazonS3 s3Client;
    @Mock
    S3Object s3Object;
    @Mock
    S3ObjectInputStream inputStream;
    @Spy
    AwsBucketProperties awsBucketProperties;

    @InjectMocks
    S3BucketImpl s3Bucket;


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
    void getObjectDataErrorTest() {
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