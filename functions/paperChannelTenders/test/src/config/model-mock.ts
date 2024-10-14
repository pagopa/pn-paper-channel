import {
  PaperChannelTenderCosts,
  PaperChannelGeokey,
  PaperChannelTender,
} from '../../../src/types/dynamo-types';
import { Page } from '../../../src/types/model-types';
import { AttributeValue, GetItemCommandOutput, QueryCommandOutput } from '@aws-sdk/client-dynamodb';
import { QueryOutput } from '@aws-sdk/client-dynamodb/dist-types/models/models_0';

const currentDate = new Date();

const addDays = (date: Date, days: number) => {
  const current = new Date(date);
  current.setDate(date.getDate() + days);
  return current;
}

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
  dismissed: false,
  createdAt: "2024-10-07T14:30:15.000Z"
} as PaperChannelGeokey;

export const costItem: PaperChannelTenderCosts = {
  tenderId: "12345",
  productLotZone: "AR#LOT_1#EU",
  product: "AR",
  lot: "LOT_1",
  zone: "EU",
  deliveryDriverName: "GLS",
  deliveryDriverId: "121212",
  dematerializationCost: 12.89,
  rangedCosts: [
    {
      minWeight: 1,
      maxWeight: 10,
      cost: 12.34
    }
  ],
  createdAt: "2024-10-07T14:30:15.000Z",
}



export const getGeokey = (dismissed: boolean = false, activationDate ?: string):Record<string, AttributeValue> => ({
  tenderProductGeokey: {
    "S": "12345#AR#85965"
  },
  activationDate: {
    "S": activationDate || "2024-10-07T14:30:15.000Z",
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
    "BOOL": dismissed
  },
  createdAt: {
    "S": "2024-10-07T14:30:15.000Z"
  }
})

const getItem: Record<string, AttributeValue> = {
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
    "N": "12.89",
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
}

export const getItemCostOutput: GetItemCommandOutput = {
  Item: getItem,
  $metadata: {}
};

export const getItemCostListOutput: QueryOutput = {
  Items: [getItem]
}

export const getItemGeokeyOutput: QueryCommandOutput = {
  Items: [
    getGeokey(false, addDays(currentDate, 10).toISOString()),
    getGeokey(false, "2024-10-07T14:30:15.000Z"),
    getGeokey(false, addDays(new Date("2024-10-07T14:30:15.000Z"), -11).toISOString()),
  ],
  Count: 3,
  $metadata: {}
};