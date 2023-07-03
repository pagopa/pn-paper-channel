package it.pagopa.pn.paperchannel.s3.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import it.pagopa.pn.paperchannel.config.AwsBucketProperties;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PresignedUrlResponseDto;
import it.pagopa.pn.paperchannel.mapper.PresignedUrlResponseMapper;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@CustomLog
public class S3BucketImpl implements S3Bucket {
    private static final String XSLS_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final AmazonS3 s3Client;
    private final AwsBucketProperties awsBucketProperties;

    private final String s3BucketName = "S3Bucket";

    public S3BucketImpl(AmazonS3 s3Client, AwsBucketProperties awsBucketProperties) {
        this.s3Client = s3Client;
        this.awsBucketProperties = awsBucketProperties;
    }

    @Override
    public Mono<PresignedUrlResponseDto> presignedUrl() {
        String processName = "Get Presigned Url";
        log.logInvokingExternalService(s3BucketName, processName);
        String uuid = UUID.randomUUID().toString();
        String fileName = PREFIX_URL.concat(uuid);
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(this.awsBucketProperties.getName(), fileName)
                .withMethod(HttpMethod.PUT)
                .withExpiration(this.getExpirationDate());
        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

        PresignedUrlResponseDto presignedUrlResponseDto = PresignedUrlResponseMapper.fromResult(url, uuid);
        return Mono.just(presignedUrlResponseDto);
    }

    @Override
    public Mono<File> putObject(File file) {
        try {
            String processName = "Create And Upload File Async";
            log.logInvokingAsyncExternalService(s3BucketName, processName, null);
            PutObjectRequest request = new PutObjectRequest(this.awsBucketProperties.getName(), file.getName(), file);
            // set metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.addUserMetadata("title", file.getName());
            metadata.setContentType(XSLS_CONTENT_TYPE);
            request.setMetadata(metadata);
            s3Client.putObject(request);
        } catch (Exception e) {
            log.error("Error in upload object in s3 {}", e.getMessage());
        }
        return Mono.just(file);
    }

    public byte[] getObjectData(String filename) {
        String processName = "Download File";
        log.logInvokingExternalService(s3BucketName, processName);
        byte[] data = null;
        S3Object fullObject = s3Client.getObject(new GetObjectRequest(this.awsBucketProperties.getName(), filename));
        if (fullObject != null) {
            try {
                data = fullObject.getObjectContent().readAllBytes();
            } catch (IOException ioException) {
                log.error("getObjectData {}", ioException.getMessage());
            }
        }
        return data;
    }

    public InputStream getFileInputStream(String filename) {
        String processName = "Notify Upload";
        log.logInvokingExternalService(s3BucketName, processName);
        S3Object fullObject = s3Client.getObject(new GetObjectRequest(this.awsBucketProperties.getName(), filename));
        if (fullObject != null) {
            return fullObject.getObjectContent().getDelegateStream();
        }
        return null;
    }

    private Date getExpirationDate() {
        Date expiration = new Date();
        long expTimeMillis = Instant.now().toEpochMilli();
        expTimeMillis += this.awsBucketProperties.getExpiration();
        expiration.setTime(expTimeMillis);
        return expiration;
    }

}
