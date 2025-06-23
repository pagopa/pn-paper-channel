echo "### CREATE AWS OBJECTS FOR IT TESTS ###"

bash <(curl -s https://raw.githubusercontent.com/pagopa/pn-paper-channel/1bd29cd4cb43f513ac86387ce29fec02cbe3ee07/src/test/resources/testcontainers/init.sh)


echo "### CREATE TEST IT QUEUES ###"
queues="local-ext-channels-outputs-test local-ext-channels-outputs-test-DLQ local-radd-alt-to-paper-channel local-paperchannel_to_delayer local-paper-normalize-address-test"
for qn in  $( echo $queues | tr " " "\n" ) ; do
    echo creating queue $qn ...
    aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"0"}' \
        --queue-name $qn
    echo ending create queue
done

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 sqs set-queue-attributes --queue-url local-ext-channels-outputs-test --attributes '{"RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:local-ext-channels-outputs-test-DLQ\",\"maxReceiveCount\":\"2\"}", "DelaySeconds":"0", "VisibilityTimeout": "5"}'
