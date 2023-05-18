#!/bin/sh

# Script da utilizzare solo in ambienti di test in quanto il popolamento dei dati andrebbe eseguito tramite il portale Backoffice

# URL dell'ALB di PN Core (si può copiare lo script su un bastion host di PN Core oppure creare un tunnel in locale tramite SSM)
_BASEURI="$1"

#INSERIMENTO GARA : il "code" non deve già essere presente a db.
curl --location --request POST "${_BASEURI}/paper-channel-bo/v1/tender" --header 'Content-Type: application/json' --data '{ "code": "GARA2022", "name": "Gara 2022", "startDate": "2023-02-17", "endDate": "2023-12-31" }'

#INSERIMENTO DEL RECAPISTA : il tender (in questo caso GARA2022) deve già essere presente a db. TaxId univoco.
curl --location --request POST "${_BASEURI}/paper-channel-bo/v1/delivery-driver/GARA2022" --header 'Content-Type: application/json' --data-raw '{ "denomination": "Denominazione", "businessName": "Ragione sociale", "registeredOffice": "Sede legale", "pec": "email.example@pec.com", "fiscalCode": "ABCDEF22G12H345K", "taxId": "12345678900", "phoneNumber": "+39012345678", "uniqueCode": "A12C34D56789E0", "fsu": true }'

#INSERIMENTO DEL COSTO : inserimento con tender e taxId appartenente.
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 10, "priceAdditional": 12, "productType": "AR", "cap": [], "zone": "ZONE_1" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 15, "priceAdditional": 12, "productType": "AR", "cap": [], "zone": "ZONE_2" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 20, "priceAdditional": 12, "productType": "AR", "cap": [], "zone": "ZONE_3" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 18, "priceAdditional": 12, "productType": "RS", "cap": [], "zone": "ZONE_1" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 17, "priceAdditional": 12, "productType": "RS", "cap": [], "zone": "ZONE_2" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 29, "priceAdditional": 12, "productType": "RS", "cap": [], "zone": "ZONE_3" }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 35, "priceAdditional": 12, "productType": "890", "cap": [ "99999" ] }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 127, "priceAdditional": 12, "productType": "AR", "cap": [ "99999" ] }'

#INSERIMENTO DEL COSTO
curl -v --location --request POST "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/cost" --header 'Content-Type: application/json' --data '{ "scode": "string", "price": 241, "priceAdditional": 12, "productType": "RS", "cap": [ "99999" ] }'

#VALIDAZIONE
curl -v --location --request PUT "${_BASEURI}/paper-channel-bo/v1/tender/GARA2022" --header 'Content-Type: application/json' --data-raw '{ "statusCode": "VALIDATED"}'

#GET SINGOLA GARA - deve avere "tender.status" con valore "IN_PROGRESS"
curl -v --location --request GET "${_BASEURI}/paper-channel-bo/v1/tenders/GARA2022?page=1&size=10"

#GET COSTO GARA - deve avere una lista valorizzata in "content"
curl --location --request GET "${_BASEURI}/paper-channel-bo/v1/GARA2022/delivery-driver/12345678900/get-cost?page=1&size=10"