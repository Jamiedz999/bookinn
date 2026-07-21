export type Role = 'GUEST' | 'HOST'

export interface User {
  id: number
  email: string
  name: string
  demo: boolean
  roles: Role[]
}

export interface AuthResponse {
  accessToken: string
  user: User
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  name: string
}

export type ListingStatus = 'ACTIVE' | 'INACTIVE'

export interface Amenity {
  id: number
  name: string
}

/** Full public detail view of a listing (mirrors backend ListingResponse). */
export interface Listing {
  id: number
  hostId: number
  title: string
  description: string | null
  city: string
  address: string
  pricePerNight: number
  maxGuests: number
  status: ListingStatus
  amenities: Amenity[]
  photoUrls: string[]
}

/** Compact listing view for the host's "my listings" list (mirrors ListingSummaryResponse). */
export interface ListingSummary {
  id: number
  title: string
  city: string
  pricePerNight: number
  status: ListingStatus
  coverPhotoUrl: string | null
}

/** One page of results (mirrors backend PageResponse). */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Query parameters for the public listing search; all optional. */
export interface SearchParams {
  city?: string
  checkIn?: string
  checkOut?: string
  page?: number
}

/** Payload for create and update; amenities by id, photos as an ordered URL list. */
export interface ListingInput {
  title: string
  description: string
  city: string
  address: string
  pricePerNight: number
  maxGuests: number
  amenityIds: number[]
  photoUrls: string[]
}

export type BookingStatus = 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'

/** Price preview for a prospective stay (mirrors backend QuoteResponse). */
export interface Quote {
  listingId: number
  checkIn: string
  checkOut: string
  nights: number
  pricePerNight: number
  totalPrice: number
}

/** Payload to create a booking (mirrors backend CreateBookingRequest). */
export interface CreateBookingRequest {
  listingId: number
  checkIn: string
  checkOut: string
  guestCount: number
}

/**
 * A booking as shown on both the guest's "my bookings" and host's "bookings received" pages (mirrors
 * backend BookingResponse). {@code cancellable} is computed server-side against the 48h policy.
 */
export interface Booking {
  id: number
  listingId: number
  listingTitle: string
  listingCity: string
  coverPhotoUrl: string | null
  guestId: number
  guestName: string
  checkIn: string
  checkOut: string
  nights: number
  guestCount: number
  totalPrice: number
  status: BookingStatus
  cancellable: boolean
  createdAt: string
  cancelledAt: string | null
}
