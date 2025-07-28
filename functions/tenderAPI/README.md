# pn-paper-channel Tenders API

## Descrizione
Questa Lambda si occupa di recuperare le informazioni riguardanti le gare di pn-paper-channel.


## Installazione
- Node.js (v16 o successiva)
- Installare le dipendenze

  ```
  npm install
  ```

## Build
  ```
  npm run test-build
  ```


## API
### API per la ricerca delle gare

L'api si occupa di recuperare tutte le gare. Inoltre è possibile effettuare la ricerca circoscrivendo un range temporale (from, to) oppure filtrando per la sola gara attiva e viceversa

```sh
aws lambda
    --profile sso_pn-core-X\
    invoke\
    --function-name pn-paper-channel-tenderAPILambda\
    --payload '<payload>'\
    response.json
```

**Payload:**

```Typescript
type Payload {
  operation: "GET_TENDERS";
  page: number;             // Pagina richiesta (intero)
  size: number;             // Numero di elmenti per pagina (intero)
  from?: string;            // Formato: date (ISO 8601 UTC)
  to?: string;              // Formato: date (ISO 8601 UTC)
}
```

Esempio stringa ISO 8601 UTC: 2024-01-01T08:00:00.000Z

**Response:**

```Typescript
type Response {
  statusCode: 200 | 400;
  description: string;
  content?: PaperChannelTender[];
}

type PaperChannelTender {
  tenderId: string;
  name: string;
  activationDate: string;   // Formato: date (ISO 8601 UTC)
  vat: number;              // Intero
  nonDeductibleVat: number; // Intero
  pagePrice: number;        // Float
  basePriceAR: number;      // Float
  basePriceRS: number;      // Float
  basePrice890: number;     // Float
  fee: number;              // Float
  active: boolean;          // Booleano, campo aggiuntivo
  createdAt: string;        // Formato: date (ISO 8601 UTC)
}
```
Response in formato paginato. Verranno restituiti i dati relativi alla gare filtrate per i query params. Per ogni gara si prevede inoltre un attributo che evidenza se la gara è attiva o meno. Nel caso in cui nessun elemento sia stato trovato verrà restituito content con 0 elementi.

**Status codes:**
-   **200** - Gare filtrate per i query params.
-   **400** - Errore formato dati input.

### API per la ricerca della gara attiva

L'api si occupa di recuperare la gara attiva corrente.

```sh
aws lambda
    --profile sso_pn-core-X\
    invoke\
    --function-name pn-paper-channel-tenderAPILambda\
    --payload '<payload>'\
    response.json
```

**Payload:**

```Typescript
type Payload {
  operation: "GET_TENDER_ACTIVE";
}
```

**Response:**

```Typescript
type Response {
  statusCode: 200 | 400;
  description: string;
  content: TenderDTO;
}

type PaperChannelTender {
  tenderId: string;
  name: string;
  activationDate: string;   // Formato: date (ISO 8601 UTC)
  vat: number;              // Intero
  nonDeductibleVat: number; // Intero
  pagePrice: number;        // Float
  basePriceAR: number;      // Float
  basePriceRS: number;      // Float
  basePrice890: number;     // Float
  fee: number;              // Float
  createdAt: string;        // Formato: date (ISO 8601 UTC)
}
```

**Status codes:**
-   **200** - Gara attualmente attiva.
-   **400** - Errore formato dati input.
-   **404** - Gara non trovata.

### API per la ricerca dei costi associati ad una gara

L'api si occupa di recuperare tutti i costi di una determinata gara. L'api permette inoltre di effettuare la ricerca filtrando per: prodotto, lotto, zona, recapitista

```sh
aws lambda
    --profile sso_pn-core-X\
    invoke\
    --function-name pn-paper-channel-tenderAPILambda\
    --payload '<payload>'\
    response.json
```

**Payload**

```Typescript
type Payload {
  operation: "GET_COSTS";
  tenderId: string;
  product?: string;
  lot?: string;
  zone?: string;
  deliveryDriverId?: string;
}
```

**Response:**

```Typescript
type Response {
  statusCode: 200 | 400 | 404;
  description: string;
  content: PaperChannelTenderCosts[];
}

type PaperChannelTenderCosts = {
  tenderId: string;
  productLotZone: string;
  product: string;
  lot: string;
  zone: string;
  deliveryDriverName: string;
  deliveryDriverId: string;
  dematerializationCost: number;
  rangedCosts: PaperChannelTenderCostsRange[];
  createdAt: string;
};

type PaperChannelTenderCostsRange = {
  cost: number;
  minWeight: number;
  maxWeight: number;
};
```

**Status codes:**
-   **200** - Costi associati a tenderId.
-   **400** - Errore formato dati input.
-   **404** - Gara non trovata.

### API per la ricerca puntuale di un costo

L'api si occupa di recuperare uno specifico costo a partire da gara, prodotto e geokey

```sh
aws lambda
    --profile sso_pn-core-X\
    invoke\
    --function-name pn-paper-channel-tenderAPILambda\
    --payload '<payload>'\
    response.json
```

**Payload:**

```Typescript
type Payload {
  operation: "GET_COST";
  tenderId: string;
  product: string;
  geokey: string;
}
```

**Response:**

```Typescript
type Response {
  statusCode: 200 | 400 | 404;
  description: string;
  content: PaperChannelTenderCosts;
}

type PaperChannelTenderCosts = {
  tenderId: string;
  productLotZone: string;
  product: string;
  lot: string;
  zone: string;
  deliveryDriverName: string;
  deliveryDriverId: string;
  dematerializationCost: number;
  rangedCosts: PaperChannelTenderCostsRange[];
  createdAt: string;
};

type PaperChannelTenderCostsRange = {
  cost: number;
  minWeight: number;
  maxWeight: number;
};
```

**Status codes:**
-   **200** - Costo associato a tenderId, product e geokey.
-   **400** - Errore formato dati input.
-   **404** - Gara o Geokey non trovata.

### API ricerca puntuale versioni geokey

L'api si occupa di recuperare le versioni di una specifica geokey.

```sh
aws lambda
    --profile sso_pn-core-X\
    invoke\
    --function-name pn-paper-channel-tenderAPILambda\
    --payload '<payload>'\
    response.json
```

**Payload:**

```Typescript
type Payload {
  operation: "GET_GEOKEY";
  tenderId: string;
  product: string;
  geokey: string;
}
```

**Response:**

```Typescript
type Response {
  statusCode: 200 | 400;
  description: string;
  content: PaperChannelGeokey[];
}

export type PaperChannelGeokey = {
  tenderProductGeokey: string;
  activationDate: string;
  tenderId: string;
  product: string;
  geokey: string;
  lot: string;
  zone: string;
  coverFlag: boolean;
  dismissed: boolean;
  createdAt: string;
}
```

**Status codes:**
-   **200** - Lista versioni geokey.
-   **400** - Errore formato dati input.

### API che restituisce l'elenco di tutti i recapitisti

L'api si occupa di recuperare tutti i recapitisti

```sh
aws lambda
    --profile sso_pn-core-X\
    invoke\
    --function-name pn-paper-channel-tenderAPILambda\
    --payload '<payload>'\
    response.json
```

**Payload:**

```Typescript
type Payload {
  operation: "GET_DELIVERY_DRIVERS";
}
```

**Response:**

```Typescript
type Response {
  statusCode: 200 | 400;
  description: string;
  content: PaperChannelDeliveryDriver[];
}

type PaperChannelDeliveryDriver = {
  deliveryDriverId: string;
  taxId: string;
  businessName: string;
  fiscalCode: string;
  pec: string;
  phoneNumber: string;
  registeredOffice: string;
  createdAt: string;
};
```

**Status codes:**
-   **200** - Lista dei recapitisti.
-   **400** - Errore formato dati input.
