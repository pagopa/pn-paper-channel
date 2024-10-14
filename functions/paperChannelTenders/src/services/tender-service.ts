import { findActiveTender, findTenders } from '../dao/pn-tender-dao';
import { PaperChannelTender } from '../types/dynamo-types';
import { NotFoundError } from '../types/error-types';


/**
 * Retrieves all tenders with optional date filtering, paginated by page and size.
 *
 * @param page - The page number to retrieve (0-based index).
 * @param size - The number of tenders per page.
 * @param from - Optional date to filter tenders from this date onwards.
 * @param to - Optional date to filter tenders up to this date.
 *
 * @returns A promise that resolves to the response containing the tenders for the specified page.
 */
export const getAllTenders = async (page: number, size: number, from ?: Date, to ?: Date) => {
  const pageResponse = await findTenders(page, size, from, to);
  console.log("Get all tenders page response ", pageResponse);
  return pageResponse;
}


/**
 * Retrieves the currently active tender.
 *
 * @throws {NotFoundError} If no active tender is found.
 *
 * @returns A promise that resolves to the active tender.
 */
export const getActiveTender = async (): Promise<PaperChannelTender> => {
  const activeTender = await findActiveTender();
  if (!activeTender) {
    throw new NotFoundError("Active tender not found");
  }
  return activeTender;
}
