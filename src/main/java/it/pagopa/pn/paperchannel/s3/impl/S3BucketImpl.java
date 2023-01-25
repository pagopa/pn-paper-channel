package it.pagopa.pn.paperchannel.s3.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import it.pagopa.pn.paperchannel.config.AwsBucketProperties;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.opc.internal.ContentType;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
public class S3BucketImpl implements S3Bucket {

    private final AmazonS3 s3Client;
    private final AwsBucketProperties awsBucketProperties;
    private GeneratePresignedUrlRequest generatePresignedUrlRequest;

    public S3BucketImpl(AmazonS3 s3Client, AwsBucketProperties awsBucketProperties) {
        this.s3Client = s3Client;
        this.awsBucketProperties = awsBucketProperties;
    }

    @Override
    public Mono<String> presignedUrl() {
        String fileName ="tender-".concat(UUID.randomUUID().toString());
        generatePresignedUrlRequest = new GeneratePresignedUrlRequest(this.awsBucketProperties.getName(), fileName)
                .withMethod(HttpMethod.PUT)
                .withExpiration(this.getExpirationDate());
        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return Mono.just(url.toString());
    }

    @Override
    public Mono<String> putObject(File file) {
        S3Object fullObject = null;
        try {
            PutObjectRequest request = new PutObjectRequest(this.awsBucketProperties.getName(), file.getName(), file);
            // set metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.addUserMetadata("title", file.getName());
            request.setMetadata(metadata);
            s3Client.putObject(request);
            fullObject = s3Client.getObject(new GetObjectRequest(this.awsBucketProperties.getName(), file.getName()));
        } catch (Exception e) {
            log.error("Error in upload object in s3", e.getMessage());
        } finally {
            //file.cl
        }
        return Mono.just(fullObject.getObjectContent().getHttpRequest().getURI().toString());
    }

    private Date getExpirationDate() {
        Date expiration = new Date();
        long expTimeMillis = Instant.now().toEpochMilli();
        expTimeMillis += this.awsBucketProperties.getExpiration();
        expiration.setTime(expTimeMillis);
        return expiration;
    }

}
