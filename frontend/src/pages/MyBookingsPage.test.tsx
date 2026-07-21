import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import type { Booking } from '../api/types'
import { MyBookingsPage } from './MyBookingsPage'

const base: Booking = {
  id: 10,
  listingId: 3,
  listingTitle: 'Sea view loft',
  listingCity: 'Lisbon',
  coverPhotoUrl: null,
  guestId: 7,
  guestName: 'Greta',
  checkIn: '2026-09-10',
  checkOut: '2026-09-13',
  nights: 3,
  guestCount: 2,
  totalPrice: 360,
  status: 'CONFIRMED',
  cancellable: true,
  createdAt: '2026-07-21T10:00:00Z',
  cancelledAt: null,
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <MyBookingsPage />
    </QueryClientProvider>,
  )
}

describe('MyBookingsPage', () => {
  it('disables Cancel for a booking past the 48h window', async () => {
    server.use(
      http.get('/api/bookings/my', () =>
        HttpResponse.json([{ ...base, id: 11, cancellable: false }]),
      ),
    )

    renderPage()

    expect(await screen.findByText('Sea view loft')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled()
  })

  it('cancels a cancellable booking and reflects the new status', async () => {
    let cancelled = false
    server.use(
      http.get('/api/bookings/my', () =>
        HttpResponse.json([
          cancelled ? { ...base, status: 'CANCELLED', cancellable: false } : base,
        ]),
      ),
      http.post('/api/bookings/10/cancel', () => {
        cancelled = true
        return HttpResponse.json({ ...base, status: 'CANCELLED', cancellable: false })
      }),
    )

    renderPage()

    const cancelButton = await screen.findByRole('button', { name: /cancel/i })
    expect(cancelButton).toBeEnabled()
    await userEvent.click(cancelButton)

    expect(await screen.findByText('CANCELLED')).toBeInTheDocument()
  })
})
