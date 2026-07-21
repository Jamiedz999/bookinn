import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { AuthContext } from '../auth/authContext'
import type { AuthContextValue } from '../auth/authContext'
import type { User } from '../api/types'
import { server } from '../test/mocks/server'
import { ListingDetailPage } from './ListingDetailPage'

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
  amenities: [
    { id: 1, name: 'Wifi' },
    { id: 2, name: 'Kitchen' },
  ],
  photoUrls: ['cover.jpg'],
}

const guest: User = { id: 7, email: 'g@b.com', name: 'Greta', demo: false, roles: ['GUEST'] }

function renderDetail(id: string, user: User | null = null) {
  const value: AuthContextValue = {
    user,
    loading: false,
    login: vi.fn(),
    register: vi.fn(),
    demoLogin: vi.fn(),
    becomeHost: vi.fn(),
    logout: vi.fn(),
  }
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <AuthContext.Provider value={value}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[`/listings/${id}`]}>
          <Routes>
            <Route path="/listings/:id" element={<ListingDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </AuthContext.Provider>,
  )
}

describe('ListingDetailPage', () => {
  it('renders the listing with its amenities and photos', async () => {
    server.use(http.get('/api/listings/3', () => HttpResponse.json(listing)))

    renderDetail('3')

    expect(await screen.findByRole('heading', { name: /sea view loft/i })).toBeInTheDocument()
    expect(screen.getByText(/lisbon/i)).toBeInTheDocument()
    expect(screen.getByText('Wifi')).toBeInTheDocument()
    expect(screen.getByText('Kitchen')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: /sea view loft/i })).toHaveAttribute('src', 'cover.jpg')
  })

  it('shows a not-found message when the listing is missing or deactivated', async () => {
    server.use(http.get('/api/listings/99', () => new HttpResponse(null, { status: 404 })))

    renderDetail('99')

    expect(await screen.findByText(/listing not found/i)).toBeInTheDocument()
  })

  it('prompts a logged-out visitor to log in to book', async () => {
    server.use(http.get('/api/listings/3', () => HttpResponse.json(listing)))

    renderDetail('3')

    expect(await screen.findByText(/to book this place/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /log in/i })).toHaveAttribute('href', '/login')
  })

  it('quotes and books a stay for a logged-in guest', async () => {
    server.use(
      http.get('/api/listings/3', () => HttpResponse.json(listing)),
      http.get('/api/listings/3/quote', () =>
        HttpResponse.json({
          listingId: 3,
          checkIn: '2026-09-10',
          checkOut: '2026-09-13',
          nights: 3,
          pricePerNight: 120,
          totalPrice: 360,
        }),
      ),
      http.post('/api/bookings', () =>
        HttpResponse.json({
          id: 10,
          listingId: 3,
          listingTitle: 'Sea view loft',
          listingCity: 'Lisbon',
          coverPhotoUrl: 'cover.jpg',
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
        }),
      ),
    )

    renderDetail('3', guest)

    await screen.findByRole('heading', { name: /sea view loft/i })
    fireEvent.change(screen.getByLabelText('Check-in'), { target: { value: '2026-09-10' } })
    fireEvent.change(screen.getByLabelText('Check-out'), { target: { value: '2026-09-13' } })
    await userEvent.click(screen.getByRole('button', { name: /get price/i }))

    const bookButton = await screen.findByRole('button', { name: /book for/i })
    await userEvent.click(bookButton)

    expect(await screen.findByText(/booking confirmed/i)).toBeInTheDocument()
  })

  it('warns when the chosen dates are unavailable', async () => {
    server.use(
      http.get('/api/listings/3', () => HttpResponse.json(listing)),
      http.get('/api/listings/3/quote', () => new HttpResponse(null, { status: 409 })),
    )

    renderDetail('3', guest)

    await screen.findByRole('heading', { name: /sea view loft/i })
    fireEvent.change(screen.getByLabelText('Check-in'), { target: { value: '2026-09-10' } })
    fireEvent.change(screen.getByLabelText('Check-out'), { target: { value: '2026-09-13' } })
    await userEvent.click(screen.getByRole('button', { name: /get price/i }))

    expect(await screen.findByText(/aren't available/i)).toBeInTheDocument()
  })
})
