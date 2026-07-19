import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import { getListing } from './listings'

const listing = {
  id: 3,
  hostId: 1,
  title: 'Sea view loft',
  description: 'Bright and airy',
  city: 'Lisbon',
  address: '12 Rua Azul',
  pricePerNight: 120,
  maxGuests: 4,
  status: 'ACTIVE',
  amenities: [{ id: 1, name: 'Wifi' }],
  photoUrls: ['cover.jpg'],
}

describe('listings api', () => {
  it('getListing returns the listing detail', async () => {
    server.use(http.get('/api/listings/3', () => HttpResponse.json(listing)))

    const result = await getListing(3)

    expect(result.title).toBe('Sea view loft')
    expect(result.amenities[0].name).toBe('Wifi')
    expect(result.photoUrls).toEqual(['cover.jpg'])
  })

  it('getListing rejects when the listing is missing or deactivated', async () => {
    server.use(http.get('/api/listings/99', () => new HttpResponse(null, { status: 404 })))

    await expect(getListing(99)).rejects.toBeDefined()
  })
})
