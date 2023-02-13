tail -n +1 caps.csv | parallel --pipe -N25 'head -c -1 | jq --slurp --raw-input "{ \"pn-PaperCap\": split(\"\n\") | map(split(\",\")) |
    map(
		{
			\"PutRequest\": {
				\"Item\": {
					\"author\": {
						\"S\": \"PN-PAPER-CHANNEL\"
					},
					\"cap\": {
						\"S\": .[0]
					},
					\"city\": {
						\"S\": .[1]
					}
				}
			}
		}
	)
} 
" >split_caps_{#}.json; aws dynamodb batch-write-item   --profile $PROFILE --region $REGION  --endpoint-url=$ENDPOINT --request-items file://split_caps_{#}.json'
rm -f split_caps_*.json
