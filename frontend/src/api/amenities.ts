import { api } from './client'
import type { Amenity } from './types'

/** Fetches the public amenity dictionary hosts pick from when creating listings. */
export async function getAmenities(): Promise<Amenity[]> {
  const response = await api.get<Amenity[]>('/amenities')
  return response.data
}
