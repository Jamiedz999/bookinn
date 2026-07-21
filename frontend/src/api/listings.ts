import { api } from './client'
import type { Listing, ListingInput, ListingStatus, ListingSummary, Page, SearchParams } from './types'

/**
 * Public listing search. Undefined parameters are dropped by axios, so an empty object browses all
 * active listings; supplying both dates filters out listings with an overlapping booking.
 */
export async function searchListings(params: SearchParams): Promise<Page<ListingSummary>> {
  const response = await api.get<Page<ListingSummary>>('/listings', { params })
  return response.data
}

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

/**
 * Fetches one of the host's own listings for editing, regardless of status. Uses the host endpoint
 * (not public detail) so a deactivated listing can still be loaded into the edit form.
 */
export async function getHostListing(id: number): Promise<Listing> {
  const response = await api.get<Listing>(`/host/listings/${id}`)
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
