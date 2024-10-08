import { PaperChannelTenderCosts } from '../types/dynamo-types';
import { findCost, findCosts } from '../dao/pn-cost-dao';
import { findGeokey } from '../dao/pn-geokey-dao';


export const getCosts = async (tenderId: string, product?: string, lot?: string, zone?: string, deliveryDriverId?: string): Promise<PaperChannelTenderCosts[] | undefined> => {
  return await findCosts(tenderId, product, lot, zone, deliveryDriverId);
}


export const getCost = async (tenderId: string, product: string, geokey: string): Promise<PaperChannelTenderCosts | undefined> => {
  console.log("Get cost from ", tenderId, " - ", product, " - ", geokey);
  const geokeyEntity = await findGeokey(tenderId, product, geokey);
  if (!geokeyEntity) return undefined;
  console.log("Geokey retrieved : ", geokeyEntity.lot, " - ", geokeyEntity.zone);
  const costEntity = await findCost(tenderId, product, geokeyEntity.lot, geokeyEntity.zone);
  if (!costEntity) return undefined;
  console.log("Cost retrieved : ", costEntity)
  return costEntity;
}