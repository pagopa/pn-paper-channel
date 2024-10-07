export type PaperChannelTender = {
  tenderId: string;
  activationDate: string;
  tenderName: string;
  vat: number;
  nonDeductibleVat: number;
  pagePrice: number;
  basePriceAR: number;
  basePriceRS: number;
  basePrice890: number;
  fee: number;
  createdAt: string;
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

export type PaperChannelTenderCosts = {
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
}

type PaperChannelTenderCostsRange = {
  cost: number;
  minWeight: number;
  maxWeight: number;
}