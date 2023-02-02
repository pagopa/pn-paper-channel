package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CapDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCap;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COUNTRY_NOT_FOUND;

@Repository
@Import(PnAuditLogBuilder.class)
public class CapDAOImpl extends BaseDAO<PnCap> implements CapDAO {

    public CapDAOImpl(KmsEncryption kmsEncryption,
                      DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                      DynamoDbAsyncClient dynamoDbAsyncClient,
                      AwsPropertiesConfig awsPropertiesConfig) {
        super(kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbZoneTable(), PnCap.class);
    }

    @Override
    public Mono<List<PnCap>> getAllCap(String cap) {
        return null;
//        if (StringUtils.isNotEmpty(cap)) {
//            return Mono.fromFuture
//                    (this.get(uuid, null)
//                            .thenApply(item -> item));
//        }
//
//
//        return Mono.fromFuture(this.get(cap,null).thenApply(item->item))
//                .switchIfEmpty(this.getBySecondaryIndex(PnCap.COL_CAP,cap,null)
//                        .collectList()
//                        .map(items->{
//                            if (items.isEmpty()) {
//                                throw new PnGenericException(COUNTRY_NOT_FOUND,COUNTRY_NOT_FOUND.getMessage());
//                            }
//                            return items.get(0);
//                        })
//                );
    }
}
