import { PaperChannelTenderCosts } from '../types/dynamo-types';
import { findCosts } from '../dao/pn-cost-dao';


export const getCosts = async (tenderId: string, product?: string, lot?: string, zone?: string, deliveryDriverId?: string): Promise<PaperChannelTenderCosts[] | undefined> => {
  return await findCosts(tenderId, product, lot, zone, deliveryDriverId);
}