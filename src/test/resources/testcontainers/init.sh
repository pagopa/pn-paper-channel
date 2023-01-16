echo "### CREATE QUEUES ###"
queues="local-delivery-push-inputs local-delivery-push-safestorage-inputs local-delivery-push-actions local-ext-channels-inputs local-ext-channels-outputs local-delivery-push-actions-done local-ext-channels-elab-res local-user-attributes-actions"
for qn in  $( echo $queues | tr " " "\n" ) ; do
    echo creating queue $qn ...
    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn
done

echo " - Create pn-paper-channel TABLES"
aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name RequestDeliveryDynamoTable \
    --attribute-definitions \
        AttributeName=requestId,AttributeType=S \
        AttributeName=fiscalCode,AttributeType=S \
    --key-schema \
        AttributeName=requestId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"fiscal-code-index\",
            \"KeySchema\": [{\"AttributeName\":\"fiscalCode\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        }
    ]"


aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name AddressDynamoTable  \
    --attribute-definitions \
        AttributeName=requestId,AttributeType=S \
    --key-schema \
        AttributeName=requestId,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name TenderDynamoTable \
    --attribute-definitions \
        AttributeName=idTender,AttributeType=S \
    --key-schema \
        AttributeName=idTender,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name DeliveryDriverDynamoTable \
    --attribute-definitions \
        AttributeName=uniqueCode,AttributeType=S \
		AttributeName=created,AttributeType=S \
		AttributeName=startDate,AttributeType=S \
    --key-schema \
        AttributeName=uniqueCode,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5
	--global-secondary-indexes \
    "[
        {
            \"IndexName\": \"created-index\",
            \"KeySchema\": [{\"AttributeName\":\"created\",\"KeyType\":\"HASH\"}, {\"AttributeName\":\"startDate\",\"KeyType\":\"RANGE\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 5,
                \"WriteCapacityUnits\": 5
            }
        }
	]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name CostDynamoTable \
    --attribute-definitions \
        AttributeName=idDeliveryDriver,AttributeType=S \
        AttributeName=uuid,AttributeType=S \
		AttributeName=cap,AttributeType=S \
		AttributeName=zone,AttributeType=S \
		AttributeName=idTender,AttributeType=S \
    --key-schema \
        AttributeName=idDeliveryDriver,KeyType=HASH \
		AttributeName=uuid,KeyType=SORT \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"cap-index\",
            \"KeySchema\": [{\"AttributeName\":\"cap\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 5,
                \"WriteCapacityUnits\": 5
            }
        },
		{
            \"IndexName\": \"zone-index\",
            \"KeySchema\": [{\"AttributeName\":\"zone\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 5,
                \"WriteCapacityUnits\": 5
            }
        },
		{
            \"IndexName\": \"tender-index\",
            \"KeySchema\": [{\"AttributeName\":\"idTender\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 5,
                \"WriteCapacityUnits\": 5
            }
        }

    ]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name ZoneDynamoTable \
    --attribute-definitions \
        AttributeName=countryIt,AttributeType=S \
		AttributeName=countryEn,AttributeType=S \
    --key-schema \
        AttributeName=countryIt,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
	--global-secondary-indexes \
    "[
        {
            \"IndexName\": \"countryEn-index\",
            \"KeySchema\": [{\"AttributeName\":\"countryEn\",\"KeyType\":\"HASH\"}],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 5,
                \"WriteCapacityUnits\": 5
            }
        }
	]"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name CapDynamoTable \
    --attribute-definitions \
        AttributeName=cap,AttributeType=S \
    --key-schema \
        AttributeName=cap,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5





echo "Initialization terminated"