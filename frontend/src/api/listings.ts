import { api } from './client'
import type { Listing } from './types'

/** Fetches a single listing's public detail. Rejects (404) when it is missing or deactivated. */
export async function getListing(id: number): Promise<Listing> {
  const response = await api.get<Listing>(`/listings/${id}`)
  return response.data
}
