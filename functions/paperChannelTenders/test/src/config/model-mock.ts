import { PaperChannelTender } from '../../../src/types/dynamo-types';
import { Page } from '../../../src/types/model-types';

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