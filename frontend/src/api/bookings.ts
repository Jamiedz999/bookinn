import { api } from './client'
import type { Booking, CreateBookingRequest, Quote } from './types'

/** Prices a prospective stay and confirms availability. Rejects (409) when the dates are taken. */
export async function getQuote(
  listingId: number,
  checkIn: string,
  checkOut: string,
): Promise<Quote> {
  const response = await api.get<Quote>(`/listings/${listingId}/quote`, {
    params: { checkIn, checkOut },
  })
  return response.data
}

/** Creates a booking for the authenticated guest. Rejects (409) if the dates were just taken. */
export async function createBooking(body: CreateBookingRequest): Promise<Booking> {
  const response = await api.post<Booking>('/bookings', body)
  return response.data
}

/** Lists the authenticated guest's own bookings, newest first. */
export async function getMyBookings(): Promise<Booking[]> {
  const response = await api.get<Booking[]>('/bookings/my')
  return response.data
}

/** Lists bookings received on the authenticated host's listings, newest first. */
export async function getHostBookings(): Promise<Booking[]> {
  const response = await api.get<Booking[]>('/host/bookings')
  return response.data
}

/** Cancels one of the caller's own bookings, subject to the 48h policy and state machine. */
export async function cancelBooking(id: number): Promise<Booking> {
  const response = await api.post<Booking>(`/bookings/${id}/cancel`)
  return response.data
}
