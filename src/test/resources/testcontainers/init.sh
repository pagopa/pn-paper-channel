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
        AttributeName=tenderCode,AttributeType=S \
		    AttributeName=author,AttributeType=S \
		    AttributeName=date,AttributeType=S \
    --key-schema \
        AttributeName=tenderCode,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
	--global-secondary-indexes \
    "[
        {
            \"IndexName\": \"author-index\",
            \"KeySchema\": [{\"AttributeName\":\"author\",\"KeyType\":\"HASH\"},{\"AttributeName\":\"date\",\"KeyType\":\"RANGE\"}],
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
        AttributeName=author,AttributeType=S \
        AttributeName=cap,AttributeType=S \
    --key-schema \
        AttributeName=author,KeyType=HASH \
        AttributeName=cap,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5

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
    --table-name DeliveryFileDynamoTable \
    --attribute-definitions \
        AttributeName=uuid,AttributeType=S \
    --key-schema \
        AttributeName=uuid,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name DeliveryDriverDynamoTable \
    --attribute-definitions \
        AttributeName=uniqueCode,AttributeType=S \
        AttributeName=tenderCode,AttributeType=S \
		    AttributeName=author,AttributeType=S \
		    AttributeName=startDate,AttributeType=S \
    --key-schema \
        AttributeName=uniqueCode,KeyType=HASH \
        AttributeName=tenderCode,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
	--global-secondary-indexes \
    "[
        {
            \"IndexName\": \"author-index\",
            \"KeySchema\": [{\"AttributeName\":\"author\",\"KeyType\":\"HASH\"},{\"AttributeName\":\"startDate\",\"KeyType\":\"RANGE\"}],
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
            \"KeySchema\": [{\"AttributeName\":\"tenderCode\",\"KeyType\":\"HASH\"}],
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
        AttributeName=driverCode,AttributeType=S \
        AttributeName=uuidCode,AttributeType=S \
		    AttributeName=tenderCode,AttributeType=S \
    --key-schema \
        AttributeName=driverCode,KeyType=HASH \
		    AttributeName=uuidCode,KeyType=SORT \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
		{
            \"IndexName\": \"tender-index\",
            \"KeySchema\": [{\"AttributeName\":\"tenderCode\",\"KeyType\":\"HASH\"}],
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
    --table-name PaperRequestErrorDynamoTable  \
    --attribute-definitions \
        AttributeName=requestId,AttributeType=S \
        AttributeName=created,AttributeType=S \
    --key-schema \
        AttributeName=requestId,KeyType=HASH \
        AttributeName=created,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name CapDynamoTable  \
    --item '{"author": {"S": "PN-PAPER-CHANNEL"}, "cap": {"S": "35031"}, "city": {"S": "Abano Terme"}}'

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name DeliveryFileDynamoTable  \
    --item '{"uuid": {"S": "12345" }, "status": {"S": "UPLOADING"}, "url": {"S": "www.abcd.it"}}'

aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name DeliveryDriverDynamoTable  \
    --item '{"uniqueCode": {"S": "CXJ564" }, "tenderCode": {"S": "GARA-2022"},  "denomination": {"S": "GLS"}, "taxId": {"S": "1234957483"}, "phoneNumber": {"S": "351543654"}, "fsu": {"BOOL": false}, "author":{"S": "PN-PAPER-CHANNEL"}, "startDate": {"S": "2023-01-22T10:15:30Z"}}'


aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name DeliveryDriverDynamoTable  \
    --item '{"uniqueCode": {"S": "CXJ664" }, "tenderCode": {"S": "GARA-2022"},  "denomination": {"S": "NEXIVE"}, "taxId": {"S": "12312434324"}, "phoneNumber": {"S": "23432432234"}, "fsu": {"BOOL": false}, "author":{"S": "PN-PAPER-CHANNEL"}, "startDate": {"S": "2023-01-22T10:15:30Z"}}'


aws  --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb put-item \
    --table-name DeliveryDriverDynamoTable  \
    --item '{"uniqueCode": {"S": "LOP3222" }, "tenderCode": {"S": "GARA-2022"},  "denomination": {"S": "BRT"}, "taxId": {"S": "21432432342"}, "phoneNumber": {"S": "32423455322"}, "fsu": {"BOOL": false}, "author":{"S": "PN-PAPER-CHANNEL"}, "startDate": {"S": "2023-01-22T10:15:30Z"}}'

echo "Initialization terminated"