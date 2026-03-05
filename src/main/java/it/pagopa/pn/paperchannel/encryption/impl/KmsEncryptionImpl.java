package it.pagopa.pn.paperchannel.encryption.impl;

import it.pagopa.pn.paperchannel.config.AwsKmsProperties;
import it.pagopa.pn.paperchannel.encryption.EncryptedUtils;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.encryption.model.EncryptionModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptRequest;
import software.amazon.awssdk.services.kms.model.EncryptionAlgorithmSpec;

import java.util.Base64;
import java.util.Optional;


@Slf4j
@Qualifier("kmsEncryption")
public class KmsEncryptionImpl implements DataEncryption {

    private final KmsClient kms;
    private final AwsKmsProperties awsKmsProperties;

    public KmsEncryptionImpl(KmsClient kmsClient, AwsKmsProperties awsKmsProperties) {
        this.kms = kmsClient;
        this.awsKmsProperties = awsKmsProperties;
    }

    @Override
    public String encode(String data) {
        if(StringUtils.isNotEmpty(data)) {

            final EncryptRequest encryptRequest = EncryptRequest.builder()
                    .keyId(this.awsKmsProperties.getKeyId())
                    .plaintext(SdkBytes.fromUtf8String(data))
                    .build();

            final byte[] encryptedBytes = kms.encrypt(encryptRequest).ciphertextBlob().asByteArray();

            return Base64.getEncoder().encodeToString(encryptedBytes);
        } else {
            return data;
        }
    }

    @Override
    public String decode(String data) {
        if(StringUtils.isNotEmpty(data)) {
            final EncryptedUtils token = EncryptedUtils.parse(data);

            final EncryptionModel options = token.getModel();
            final String keyId = Optional.ofNullable(options.getKeyId()).orElse(awsKmsProperties.getKeyId());
            final String algorithm = Optional.ofNullable(options.getAlgorithm()).orElse("SYMMETRIC_DEFAULT");

            final DecryptRequest decryptRequest = DecryptRequest.builder()
                    .ciphertextBlob(SdkBytes.fromByteBuffer(token.getCipherBytes()))
                    .encryptionContext(token.getEncryptionContext())
                    .encryptionAlgorithm(EncryptionAlgorithmSpec.fromValue(algorithm))
                    .keyId(keyId)
                    .build();

            DecryptResponse decryptResponse = kms.decrypt(decryptRequest);
            return decryptResponse.plaintext().asUtf8String();
        } else {
            return data;
        }
    }

}
