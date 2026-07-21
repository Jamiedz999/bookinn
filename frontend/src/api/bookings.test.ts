import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import {
  cancelBooking,
  createBooking,
  getHostBookings,
  getMyBookings,
  getQuote,
} from './bookings'
import type { Booking } from './types'

const booking: Booking = {
  id: 10,
  listingId: 3,
  listingTitle: 'Sea view loft',
  listingCity: 'Lisbon',
  coverPhotoUrl: 'cover.jpg',
  guestId: 7,
  guestName: 'Greta Guest',
  checkIn: '2026-09-10',
  checkOut: '2026-09-13',
  nights: 3,
  guestCount: 2,
  totalPrice: 300,
  status: 'CONFIRMED',
  cancellable: true,
  createdAt: '2026-07-21T10:00:00Z',
  cancelledAt: null,
}

describe('bookings api', () => {
  it('getQuote forwards the date params and returns the breakdown', async () => {
    let requestedUrl = ''
    server.use(
      http.get('/api/listings/3/quote', ({ request }) => {
        requestedUrl = request.url
        return HttpResponse.json({
          listingId: 3,
          checkIn: '2026-09-10',
          checkOut: '2026-09-13',
          nights: 3,
          pricePerNight: 100,
          totalPrice: 300,
        })
      }),
    )

    const quote = await getQuote(3, '2026-09-10', '2026-09-13')

    expect(quote.totalPrice).toBe(300)
    expect(quote.nights).toBe(3)
    const query = new URL(requestedUrl).searchParams
    expect(query.get('checkIn')).toBe('2026-09-10')
    expect(query.get('checkOut')).toBe('2026-09-13')
  })

  it('getQuote rejects when the dates are unavailable', async () => {
    server.use(
      http.get('/api/listings/3/quote', () => new HttpResponse(null, { status: 409 })),
    )

    await expect(getQuote(3, '2026-09-10', '2026-09-13')).rejects.toBeDefined()
  })

  it('createBooking posts the payload and returns the booking', async () => {
    let posted: unknown = null
    server.use(
      http.post('/api/bookings', async ({ request }) => {
        posted = await request.json()
        return HttpResponse.json(booking)
      }),
    )

    const result = await createBooking({
      listingId: 3,
      checkIn: '2026-09-10',
      checkOut: '2026-09-13',
      guestCount: 2,
    })

    expect((posted as { listingId: number }).listingId).toBe(3)
    expect(result.status).toBe('CONFIRMED')
    expect(result.totalPrice).toBe(300)
  })

  it('getMyBookings returns the guest bookings', async () => {
    server.use(http.get('/api/bookings/my', () => HttpResponse.json([booking])))

    const result = await getMyBookings()

    expect(result).toHaveLength(1)
    expect(result[0].listingTitle).toBe('Sea view loft')
  })

  it('getHostBookings returns the received bookings', async () => {
    server.use(http.get('/api/host/bookings', () => HttpResponse.json([booking])))

    const result = await getHostBookings()

    expect(result).toHaveLength(1)
    expect(result[0].guestName).toBe('Greta Guest')
  })

  it('cancelBooking posts to the cancel path and returns the updated booking', async () => {
    server.use(
      http.post('/api/bookings/10/cancel', () =>
        HttpResponse.json({ ...booking, status: 'CANCELLED', cancellable: false }),
      ),
    )

    const result = await cancelBooking(10)

    expect(result.status).toBe('CANCELLED')
    expect(result.cancellable).toBe(false)
  })
})
