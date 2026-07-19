import { api } from './client'
import type { Listing, ListingInput, ListingStatus, ListingSummary } from './types'

/** Fetches a single listing's public detail. Rejects (404) when it is missing or deactivated. */
export async function getListing(id: number): Promise<Listing> {
  const response = await api.get<Listing>(`/listings/${id}`)
  return response.data
}

/** Lists the authenticated host's own listings, including INACTIVE ones. */
export async function getHostListings(): Promise<ListingSummary[]> {
  const response = await api.get<ListingSummary[]>('/host/listings')
  return response.data
}

/** Creates a listing owned by the authenticated host. */
export async function createListing(body: ListingInput): Promise<Listing> {
  const response = await api.post<Listing>('/listings', body)
  return response.data
}

/** Replaces the editable fields of a listing the host owns. */
export async function updateListing(id: number, body: ListingInput): Promise<Listing> {
  const response = await api.put<Listing>(`/listings/${id}`, body)
  return response.data
}

/** Activates or deactivates a listing the host owns. */
export async function changeListingStatus(id: number, status: ListingStatus): Promise<Listing> {
  const response = await api.patch<Listing>(`/listings/${id}/status`, { status })
  return response.data
}
