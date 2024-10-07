
export type Response<T> = {
  statusCode: 200 | 400 | 404;
  description: string;
  body?: T ;
}

export type Page<T> = {
  content: T[],
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  isFirstPage: boolean;
  isLastPage: boolean;
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