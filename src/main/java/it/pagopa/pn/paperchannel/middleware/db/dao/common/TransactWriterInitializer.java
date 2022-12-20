package it.pagopa.pn.paperchannel.middleware.db.dao.common;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;


@Component
public class TransactWriterInitializer {

    private TransactWriteItemsEnhancedRequest.Builder builder;


    public void init(){
        this.builder = TransactWriteItemsEnhancedRequest.builder();
    }

    public <A> void addRequestTransaction(MappedTableResource<A> mappedTableResource,
                                      A entity, Class<A> aClass) {
        if (builder == null) return;

        TransactPutItemEnhancedRequest<A> requestEntity =
                TransactPutItemEnhancedRequest.builder(aClass)
                        .item(entity)
                        .build();

        this.builder.addPutItem(mappedTableResource, requestEntity);
    }


    public TransactWriteItemsEnhancedRequest build(){
        if (builder == null) return null;
        return this.builder.build();
    }


}
