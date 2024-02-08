#!/bin/bash

_BASEURI=$1
_GARA=$2

curl -v --location --request PUT "${_BASEURI}/paper-channel-bo/v1/tender/$_GARA" --header 'Content-Type: application/json' --data-raw '{ "statusCode": "VALIDATED"}'