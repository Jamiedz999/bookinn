import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import {
  changeListingStatus,
  createListing,
  getHostListing,
  getHostListings,
  getListing,
  searchListings,
  updateListing,
} from './listings'
import type { ListingInput } from './types'

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

const input: ListingInput = {
  title: 'New loft',
  description: '',
  city: 'Porto',
  address: '1 Rua Nova',
  pricePerNight: 90,
  maxGuests: 2,
  amenityIds: [1],
  photoUrls: ['x.jpg'],
}

describe('listings api', () => {
  it('searchListings returns the page and forwards city and date params', async () => {
    let requestedUrl = ''
    server.use(
      http.get('/api/listings', ({ request }) => {
        requestedUrl = request.url
        return HttpResponse.json({
          content: [
            { id: 1, title: 'A', city: 'Porto', pricePerNight: 90, status: 'ACTIVE', coverPhotoUrl: null },
          ],
          page: 0,
          size: 12,
          totalElements: 1,
          totalPages: 1,
        })
      }),
    )

    const result = await searchListings({ city: 'Porto', checkIn: '2026-09-10', checkOut: '2026-09-12' })

    expect(result.content[0].title).toBe('A')
    expect(result.totalElements).toBe(1)
    const query = new URL(requestedUrl).searchParams
    expect(query.get('city')).toBe('Porto')
    expect(query.get('checkIn')).toBe('2026-09-10')
    expect(query.get('checkOut')).toBe('2026-09-12')
  })

  it('searchListings omits undefined params', async () => {
    let requestedUrl = ''
    server.use(
      http.get('/api/listings', ({ request }) => {
        requestedUrl = request.url
        return HttpResponse.json({ content: [], page: 0, size: 12, totalElements: 0, totalPages: 0 })
      }),
    )

    await searchListings({})

    expect(new URL(requestedUrl).search).toBe('')
  })

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

  it('getHostListings returns the host summaries', async () => {
    server.use(
      http.get('/api/host/listings', () =>
        HttpResponse.json([
          { id: 1, title: 'A', city: 'Lisbon', pricePerNight: 100, status: 'INACTIVE', coverPhotoUrl: null },
        ]),
      ),
    )

    const result = await getHostListings()

    expect(result).toHaveLength(1)
    expect(result[0].status).toBe('INACTIVE')
  })

  it('getHostListing returns an owned listing regardless of status', async () => {
    server.use(
      http.get('/api/host/listings/3', () =>
        HttpResponse.json({ ...listing, status: 'INACTIVE' }),
      ),
    )

    const result = await getHostListing(3)

    expect(result.status).toBe('INACTIVE')
    expect(result.title).toBe('Sea view loft')
  })

  it('createListing posts the input and returns the created listing', async () => {
    let posted: unknown = null
    server.use(
      http.post('/api/listings', async ({ request }) => {
        posted = await request.json()
        return HttpResponse.json({ ...listing, title: input.title })
      }),
    )

    const result = await createListing(input)

    expect((posted as ListingInput).title).toBe('New loft')
    expect(result.title).toBe('New loft')
  })

  it('updateListing puts to the listing id', async () => {
    server.use(http.put('/api/listings/3', () => HttpResponse.json({ ...listing, title: 'Updated' })))

    const result = await updateListing(3, input)

    expect(result.title).toBe('Updated')
  })

  it('changeListingStatus patches the status', async () => {
    server.use(
      http.patch('/api/listings/3/status', async ({ request }) => {
        const body = (await request.json()) as { status: string }
        return HttpResponse.json({ ...listing, status: body.status })
      }),
    )

    const result = await changeListingStatus(3, 'INACTIVE')

    expect(result.status).toBe('INACTIVE')
  })
})
