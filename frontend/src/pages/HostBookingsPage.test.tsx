import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import { HostBookingsPage } from './HostBookingsPage'

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <HostBookingsPage />
    </QueryClientProvider>,
  )
}

describe('HostBookingsPage', () => {
  it('lists bookings received with the guest name and status', async () => {
    server.use(
      http.get('/api/host/bookings', () =>
        HttpResponse.json([
          {
            id: 10,
            listingId: 3,
            listingTitle: 'Sea view loft',
            listingCity: 'Lisbon',
            coverPhotoUrl: null,
            guestId: 7,
            guestName: 'Greta Guest',
            checkIn: '2026-09-10',
            checkOut: '2026-09-13',
            nights: 3,
            guestCount: 2,
            totalPrice: 360,
            status: 'CONFIRMED',
            cancellable: true,
            createdAt: '2026-07-21T10:00:00Z',
            cancelledAt: null,
          },
        ]),
      ),
    )

    renderPage()

    expect(await screen.findByText('Sea view loft')).toBeInTheDocument()
    expect(screen.getByText(/greta guest/i)).toBeInTheDocument()
    expect(screen.getByText('CONFIRMED')).toBeInTheDocument()
  })

  it('shows an empty state when there are no bookings', async () => {
    server.use(http.get('/api/host/bookings', () => HttpResponse.json([])))

    renderPage()

    expect(await screen.findByText(/no bookings on your listings yet/i)).toBeInTheDocument()
  })
})
