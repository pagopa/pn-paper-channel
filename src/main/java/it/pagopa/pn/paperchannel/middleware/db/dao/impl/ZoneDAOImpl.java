package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.KmsEncryption;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.ZoneDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnZone;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.COUNTRY_NOT_FOUND;

@Repository
@Import(PnAuditLogBuilder.class)
public class ZoneDAOImpl extends BaseDAO<PnZone> implements ZoneDAO {
    public ZoneDAOImpl(PnAuditLogBuilder auditLogBuilder,
                          KmsEncryption kmsEncryption,
                          DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                          DynamoDbAsyncClient dynamoDbAsyncClient,
                          AwsPropertiesConfig awsPropertiesConfig) {
        super(auditLogBuilder, kmsEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbZoneTable(), PnZone.class);
    }

    @Override
    public Mono<PnZone> getByCountry(String country) {
        return Mono.fromFuture(this.get(country,null).thenApply(item->item))
                .switchIfEmpty(this.getBySecondaryIndex(PnZone.COUNTRY_EN_INDEX,country,null)
                        .collectList()
                        .map(items->{
                            if (items.isEmpty()) {
                                throw new PnGenericException(COUNTRY_NOT_FOUND,COUNTRY_NOT_FOUND.getMessage());
                            }
                            return items.get(0);
                        })
                );
    }
}
