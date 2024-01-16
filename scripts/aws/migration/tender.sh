#!/bin/sh

# Script da utilizzare solo in ambienti di test in quanto il popolamento dei dati andrebbe eseguito tramite il portale Backoffice

# URL dell'ALB di PN Core (si può copiare lo script su un bastion host di PN Core oppure creare un tunnel in locale tramite SSM)
_BASEURI="$1"

#INSERIMENTO GARA : il "code" non deve già essere presente a db.
curl --location --request POST "${_BASEURI}/paper-channel-bo/v1/tender" --header 'Content-Type: application/json' --data '{ "code": "TEST_GARA2024", "name": "TestGara 2024", "startDate": "2024-01-03", "endDate": "2024-01-06" }'

#INSERIMENTO DEL RECAPISTA : il tender (in questo caso GARA2022) deve già essere presente a db. TaxId univoco.
curl --location --request POST "${_BASEURI}/paper-channel-bo/v1/delivery-driver/TEST_GARA2024" --header 'Content-Type: application/json' --data-raw '{ "denomination": "Denominazione", "businessName": "Ragione sociale", "registeredOffice": "Sede legale", "pec": "email.example@pec.com", "fiscalCode": "ABCDEF22G12H345K", "taxId": "12345678900", "phoneNumber": "+39012345678", "uniqueCode": "A12C34D56789E0-TEST", "fsu": true }'

#INSERIMENTO DEL COSTO : inserimento con tender e taxId appartenente.
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 10, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "AR", "cap": [], "zone": "ZONE_1" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 15, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "AR", "cap": [], "zone": "ZONE_2" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 20, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "AR", "cap": [], "zone": "ZONE_3" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 18, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "RS", "cap": [], "zone": "ZONE_1" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 17, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "RS", "cap": [], "zone": "ZONE_2" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 29, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "RS", "cap": [], "zone": "ZONE_3" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 35, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "890", "cap": [ "99999" ] }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 127, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "AR", "cap": [ "99999" ] }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 241, "price50": 50, "price100": 60, "price250": 70, "price350": 80, "price1000": 90, "price2000": 100, "priceAdditional": 12, "productType": "RS", "cap": [ "99999" ] }'

#VALIDAZIONE
curl -v --location --request PUT "${_BASEURI}/paper-channel-bo/v1/tender/TEST_GARA2024" --header 'Content-Type: application/json' --data-raw '{ "statusCode": "VALIDATED"}'

#GET SINGOLA GARA - deve avere "tender.status" con valore "IN_PROGRESS"
curl -v --location --request GET "${_BASEURI}/paper-channel-bo/v1/tenders/TEST_GARA2024?page=1&size=10"

#GET COSTO GARA - deve avere una lista valorizzata in "content"
curl --location --request GET "${_BASEURI}/paper-channel-bo/v1/TEST_GARA2024/delivery-driver/12345678900/get-cost?page=1&size=10"