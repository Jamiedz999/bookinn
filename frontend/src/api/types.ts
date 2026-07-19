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
