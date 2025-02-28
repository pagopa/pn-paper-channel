package it.pagopa.pn.paperchannel.middleware.db.dao.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.paperchannel.config.AwsPropertiesConfig;
import it.pagopa.pn.paperchannel.encryption.DataEncryption;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.common.BaseDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore.ATTRIBUTE_NOT_EXISTS;
import static it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest.COL_APPLY_RASTERIZATION;
import static it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest.COL_REQUEST_ID;


@Repository
@Slf4j
public class RequestDeliveryDAOImpl extends BaseDAO<PnDeliveryRequest> implements RequestDeliveryDAO {

    @Autowired
    @Qualifier("dataVaultEncryption")
    private DataEncryption dataVaultEncryption;
    @Autowired
    private AddressDAO addressDAO;


    public RequestDeliveryDAOImpl(DataEncryption dataVaultEncryption,
                                  DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  AwsPropertiesConfig awsPropertiesConfig) {
        super(dataVaultEncryption, dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient,
                awsPropertiesConfig.getDynamodbRequestDeliveryTable(), PnDeliveryRequest.class);
    }


    @Override
    public Mono<PnDeliveryRequest> createWithAddress(PnDeliveryRequest request, PnAddress pnAddress, PnAddress discoveredAddress) {
        String fiscalCode = request.getFiscalCode();
        return Mono.fromFuture(countOccurrencesEntity(request.getRequestId())
                        .thenCompose( total -> {
                            log.debug("Delivery request with same request id: {} and with reworkNeeded: {}", total, request.getReworkNeeded());
                            if ( total == 0 || Boolean.TRUE.equals(request.getReworkNeeded())) {
                                try {
                                    TransactWriteItemsEnhancedRequest.Builder builder =
                                            TransactWriteItemsEnhancedRequest.builder();

                                    if(pnAddress != null) {
                                        addressDAO.createTransaction(builder, pnAddress);
                                    }

                                    if (discoveredAddress != null) {
                                        discoveredAddress.setTypology(AddressTypeEnum.DISCOVERED_ADDRESS.name());
                                        addressDAO.createTransaction(builder, discoveredAddress);
                                    }

                                    request.setHashedFiscalCode(Utility.convertToHash(request.getFiscalCode()));
                                    TransactPutItemEnhancedRequest<PnDeliveryRequest> requestEntity =
                                            TransactPutItemEnhancedRequest.builder(PnDeliveryRequest.class)
                                                    .item(encode(request))
                                                    .build();

                                    builder.addPutItem(this.dynamoTable, requestEntity);
                                    return putWithTransact(builder.build()).thenApply(item-> {
                                        request.setFiscalCode(fiscalCode);
                                        return request;
                                    });
                                } catch (TransactionCanceledException tce) {
                                    log.error("Transaction Canceled {}", tce.getMessage());
                                    return null;
                                }
                            } else {
                                throw new PnHttpResponseException("Data already existed", HttpStatus.BAD_REQUEST.value());
                            }
                        })
                )
                .onErrorResume(throwable -> {
                    log.error("Error in createWithAddress", throwable);
                    return Mono.error(throwable);
                });
    }

    @Override
    public Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest) {
        return updateData(pnDeliveryRequest, false);
    }

    @Override
    public Mono<PnDeliveryRequest> updateData(PnDeliveryRequest pnDeliveryRequest, boolean ignorableNulls) {
        return Mono.fromFuture(this.dynamoTable.getItem(pnDeliveryRequest).thenApply(item -> item))
            .flatMap(entityDB -> {
                pnDeliveryRequest.setFiscalCode(entityDB.getFiscalCode());
                return Mono.fromFuture(this.update(pnDeliveryRequest, ignorableNulls).thenApply(saved -> pnDeliveryRequest));
            });
    }

    @Override
    public Mono<UpdateItemResponse> updateApplyRasterization(String requestId, Boolean applyRasterization) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(COL_REQUEST_ID, AttributeValue.builder().s(requestId).build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(table)
                .key(key)
                .updateExpression("SET #rast = :rastValue")
                .expressionAttributeNames(Map.of("#rast", COL_APPLY_RASTERIZATION))
                .expressionAttributeValues(Map.of(":rastValue", AttributeValue.builder().bool(applyRasterization).build()))
                .returnValues(ReturnValue.NONE)
                .build();

        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(updateRequest))
                .doOnError(throwable -> log.error("Error in updateApplyRasterization for requestId [{}]",requestId, throwable))
                .doOnNext(response -> log.debug("Updated applyRasterization value in [{}] for requestId [{}] completed successfully.",
                        applyRasterization, requestId));
    }


    public Mono<Void> updateStatus(String requestId, String statusCode, String statusDescription, String statusDetail, String statusDateString) {
        log.debug("[{}] Updating status in {}", requestId, statusCode);
        // Definizione della chiave primaria
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("requestId", AttributeValue.builder().s(requestId).build());

        // Creazione dell'espressione di aggiornamento
        String updateExpr = """
                SET #statusCode = :statusCodeValue, 
                    #statusDescription = :statusDescriptionValue, 
                    #statusDetail = :statusDetailValue, 
                    #statusDate = :statusDateValue
                """;

        // Mappatura degli alias per i nomi degli attributi
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#statusCode", "statusCode");
        expressionAttributeNames.put("#statusDescription", "statusDescription");
        expressionAttributeNames.put("#statusDetail", "statusDetail");
        expressionAttributeNames.put("#statusDate", "statusDate");

        // Valori da aggiornare
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":statusCodeValue", AttributeValue.builder().s(statusCode).build());
        expressionAttributeValues.put(":statusDescriptionValue", AttributeValue.builder().s(statusDescription).build());
        expressionAttributeValues.put(":statusDetailValue", AttributeValue.builder().s(statusDetail).build());
        expressionAttributeValues.put(":statusDateValue", AttributeValue.builder().s(statusDateString).build());

        // Creazione della richiesta di aggiornamento
        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(table)
                .key(key)
                .updateExpression(updateExpr)
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .returnValues(ReturnValue.NONE) // Specifica che non ti interessa il valore precedente
                .build();

        // Esecuzione dell'aggiornamento
        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(updateRequest)
                .thenAccept(response -> log.debug("Updated status in {} completed successfully.", statusCode)));
    }

    @Override
    public Mono<PnDeliveryRequest> updateConditionalOnFeedbackStatus(PnDeliveryRequest pnDeliveryRequest, boolean ignorableNulls) {
        String expression = String.format(
            "%s(%s) or %s = :null",
            ATTRIBUTE_NOT_EXISTS,
            PnDeliveryRequest.COL_FEEDBACK_STATUS_CODE,
            PnDeliveryRequest.COL_FEEDBACK_STATUS_CODE
        );

        Map<String, AttributeValue> expressionAttributeValues = Map.of(
            ":null", AttributeValue.fromNul(true)
        );

        Expression conditionExpressionUpdate = Expression.builder()
            .expression(expression)
            .expressionValues(expressionAttributeValues)
            .build();

        UpdateItemEnhancedRequest<PnDeliveryRequest> request = UpdateItemEnhancedRequest.builder(PnDeliveryRequest.class)
            .item(pnDeliveryRequest)
            .conditionExpression(conditionExpressionUpdate)
            .ignoreNulls(ignorableNulls)
            .build();

        return Mono.fromFuture(update(request)).thenReturn(pnDeliveryRequest)
            .onErrorResume(ConditionalCheckFailedException.class, e -> {
                    log.warn("ConditionalCheckFailed for updating entity: {}", pnDeliveryRequest);
                    return Mono.empty();
                }
            );
    }

    @Override
    public Mono<PnDeliveryRequest> getByRequestId(String requestId) {
        return Mono.fromFuture(this.dynamoTable.getItem(keyBuild(requestId, null)).thenApply(item -> item));
    }

    @Override
    public Mono<PnDeliveryRequest> getByRequestId(String requestId, boolean decode) {
        return this.getByRequestId(requestId).map(entity -> {
            if (decode) return this.decode(entity);
            return entity;
        });
    }

    @Override
    public Mono<PnDeliveryRequest> getByCorrelationId(String requestId, boolean decode) {
        return this.getByCorrelationId(requestId).map(entity -> {
            if (decode) return this.decode(entity);
            return entity;
        });
    }

    @Override
    public Mono<PnDeliveryRequest> getByCorrelationId(String correlationId) {
        return this.getBySecondaryIndex(PnDeliveryRequest.CORRELATION_INDEX, correlationId, null)
                .collectList()
                .flatMap(item -> {
                    if (item.isEmpty()) return Mono.empty();
                    return Mono.just(item.get(0));
                });
    }



    private CompletableFuture<Integer> countOccurrencesEntity(String requestId){
        String keyConditionExpression = PnDeliveryRequest.COL_REQUEST_ID + " = :requestId";
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":requestId",  AttributeValue.builder().s(requestId).build());
        return this.getCounterQuery(expressionValues, "", keyConditionExpression);
    }

}