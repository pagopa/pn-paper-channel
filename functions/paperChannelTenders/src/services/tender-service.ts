import { findActiveTender, findTenders } from '../dao/pn-tender-dao';


export const getAllTenders = async (page: number, size: number, from ?: string, to ?: string) => {
  const pageResponse = await findTenders(page, size, from, to);
  console.log("Get all tenders page response ", pageResponse);
  return pageResponse;
}


export const getActiveTender = async () => {
  return await findActiveTender();
}