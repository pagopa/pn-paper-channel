import {
  PaperChannelCost,
  PaperChannelGeokey,
  PaperChannelTender,
} from '../../../src/types/dynamo-types';
import { Page } from '../../../src/types/model-types';
import { GetItemCommandOutput } from '@aws-sdk/client-dynamodb';

export const tender = {
  tenderId: "1234",
  activationDate: new Date().toISOString(),
  tenderName: "Gara 2024",
  vat: 1,
  nonDeductibleVat: 1,
  pagePrice: 1,
  basePriceAR: 1,
  basePriceRS: 1,
  basePrice890: 1,
  fee: 1,
  createdAt: new Date().toISOString(),
} as PaperChannelTender;

export const pageTender = {
  content: [
    tender,
    tender,
    tender,
    tender
  ],
  number: 1,
  size: 10,
  isLastPage: true,
  isFirstPage: true,
  totalPages: 1,
  totalElements: 4,
} as Page<PaperChannelTender>

export const geokeyItem = {
  tenderProductGeokey: "12345#AR#85965",
  activationDate: "2024-10-07T14:30:15.000Z",
  tenderId: "12345",
  product: "AR",
  geokey: "85965",
  lot: "ZON1",
  zone: "EU",
  coverFlag: true,
  dismissed: true,
  createdAt: "2024-10-07T14:30:15.000Z"
} as PaperChannelGeokey;

export const costItem: PaperChannelCost = {
  tenderId: "12345",
  productLotZone: "AR#LOT_1#EU",
  product: "AR",
  lot: "LOT_1",
  zone: "EU",
  deliveryDriverName: "GLS",
  deliveryDriverId: "121212",
  dematerializationCost: "12.89",
  rangedCosts: [
    {
      minWeight: 1,
      maxWeight: 10,
      cost: 12.34
    }
  ],
  createdAt: "2024-10-07T14:30:15.000Z",
}

export const getItemGeokeyOutput: GetItemCommandOutput = {
  Item: {
    tenderProductGeokey: {
      "S": "12345#AR#85965"
    },
    activationDate: {
      "S": "2024-10-07T14:30:15.000Z",
    },
    tenderId: {
      "S": "12345"
    },
    product: {
      "S": "AR"
    },
    geokey: {
      "S": "85965"
    },
    lot: {
      "S": "ZON1"
    },
    zone: {
      "S": "EU"
    },
    coverFlag: {
      "BOOL": true
    },
    dismissed: {
      "BOOL": true
    },
    createdAt: {
      "S": "2024-10-07T14:30:15.000Z"
    }
  },
  $metadata: {}
};


export const getItemCostOutput: GetItemCommandOutput = {
  Item: {
    tenderId: {
      "S": "12345",
    },
    productLotZone: {
      "S": "AR#LOT_1#EU",
    },
    product: {
      "S": "AR",
    },
    lot: {
      "S": "LOT_1",
    },
    zone: {
      "S": "EU",
    },
    deliveryDriverName: {
      "S": "GLS",
    },
    deliveryDriverId: {
      "S": "121212",
    },
    dematerializationCost: {
      "S": "12.89",
    },
    rangedCosts: {
      "L": [
        {
          M: {
            minWeight: { N: "1" },
            maxWeight: { N: "10" },
            cost: { N: "12.34" },
          }
        }
      ],
    },
    createdAt: {
      "S": "2024-10-07T14:30:15.000Z",
    },
  },
  $metadata: {}
};