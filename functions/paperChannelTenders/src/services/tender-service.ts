import { findActiveTender, findTenders } from '../dao/pn-tender-dao';
import { PaperChannelTender } from '../types/dynamo-types';
import { NotFoundError } from '../types/error-types';


export const getAllTenders = async (page: number, size: number, from ?: Date, to ?: Date) => {
  const pageResponse = await findTenders(page, size, from, to);
  console.log("Get all tenders page response ", pageResponse);
  return pageResponse;
}


export const getActiveTender = async (): Promise<PaperChannelTender> => {
  const activeTender = await findActiveTender();
  if (!activeTender) {
    throw new NotFoundError("Active tender not found");
  }
  return activeTender;
}