package it.pagopa.pn.paperchannel.encryption.impl;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;
import it.pagopa.pn.paperchannel.config.AwsKmsProperties;
import it.pagopa.pn.paperchannel.encryption.EncryptedUtils;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.encryption.model.EncryptionModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Optional;


@Slf4j
public class KmsEncryptionImpl implements KmsEncryption {

    private final AWSKMS kms;
    private final AwsKmsProperties awsKmsProperties;

    public KmsEncryptionImpl(AWSKMS awskms, AwsKmsProperties awsKmsProperties) {
        this.kms = awskms;
        this.awsKmsProperties = awsKmsProperties;
    }

    @Override
    public String encode(String data) {
        log.info("Encode :  {}", data);
        if(StringUtils.isNotEmpty(data)) {
            final EncryptRequest encryptRequest = new EncryptRequest()
                    .withKeyId(this.awsKmsProperties.getKeyId())
                    .withPlaintext(ByteBuffer.wrap(data.getBytes()));

            final ByteBuffer encryptedBytes = kms.encrypt(encryptRequest).getCiphertextBlob();

            return extractString(encryptedBytes, false);
        } else {
            return data;
        }
    }

    @Override
    public String decode(String data) {
        if(StringUtils.isNotEmpty(data)) {
            final EncryptedUtils token = EncryptedUtils.parse(data);

            final DecryptRequest decryptRequest = new DecryptRequest()
                    .withCiphertextBlob(token.getCipherBytes())
                    .withEncryptionContext(token.getEncryptionContext());
            final EncryptionModel options = token.getModel();
            final String keyId = Optional.ofNullable(options.getKeyId()).orElse(awsKmsProperties.getKeyId());
            final String algorithm = Optional.ofNullable(options.getAlgorithm()).orElse("SYMMETRIC_DEFAULT");
            decryptRequest.setEncryptionAlgorithm(algorithm);
            decryptRequest.setKeyId(keyId);

            return extractString(kms.decrypt(decryptRequest).getPlaintext(), true);
        } else {
            return data;
        }
    }


    private static String extractString(final ByteBuffer bb, boolean isText) {
        if (bb.hasRemaining()) {
            final byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes, bb.arrayOffset(), bb.remaining());
            if (isText)
                return new String(bytes);

            return Base64.getEncoder().encodeToString(bytes);
        } else {
            return "";
        }
    }
}
