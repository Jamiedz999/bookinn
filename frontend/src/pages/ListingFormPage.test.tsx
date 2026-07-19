import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { AuthContext } from '../auth/authContext'
import type { AuthContextValue } from '../auth/authContext'
import type { User } from '../api/types'
import { server } from '../test/mocks/server'
import { ListingFormPage } from './ListingFormPage'

const host: User = { id: 2, email: 'h@b.com', name: 'H', demo: false, roles: ['GUEST', 'HOST'] }
const guest: User = { id: 1, email: 'g@b.com', name: 'G', demo: false, roles: ['GUEST'] }

const listing = {
  id: 3,
  hostId: 2,
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

function renderForm(user: User, initialPath: string) {
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
        <MemoryRouter initialEntries={[initialPath]}>
          <Routes>
            <Route path="/host/listings/new" element={<ListingFormPage />} />
            <Route path="/host/listings/:id/edit" element={<ListingFormPage />} />
            <Route path="/host/listings" element={<div>My listings page</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </AuthContext.Provider>,
  )
}

describe('ListingFormPage', () => {
  it('creates a listing and returns to the list', async () => {
    let posted: { title: string; pricePerNight: number } | null = null
    server.use(
      http.get('/api/amenities', () => HttpResponse.json([{ id: 1, name: 'Wifi' }])),
      http.post('/api/listings', async ({ request }) => {
        posted = (await request.json()) as { title: string; pricePerNight: number }
        return HttpResponse.json(listing)
      }),
    )

    renderForm(host, '/host/listings/new')

    await userEvent.type(screen.getByLabelText(/title/i), 'New loft')
    await userEvent.type(screen.getByLabelText(/city/i), 'Porto')
    await userEvent.type(screen.getByLabelText(/address/i), '1 Rua Nova')
    await userEvent.type(screen.getByLabelText(/price per night/i), '90')
    await userEvent.click(screen.getByRole('button', { name: /create listing/i }))

    expect(await screen.findByText(/my listings page/i)).toBeInTheDocument()
    expect(posted).not.toBeNull()
    expect(posted!.title).toBe('New loft')
    expect(posted!.pricePerNight).toBe(90)
  })

  it('prefills the form when editing an existing listing', async () => {
    server.use(
      http.get('/api/amenities', () => HttpResponse.json([{ id: 1, name: 'Wifi' }])),
      http.get('/api/listings/3', () => HttpResponse.json(listing)),
    )

    renderForm(host, '/host/listings/3/edit')

    expect(await screen.findByDisplayValue('Sea view loft')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /save changes/i })).toBeInTheDocument()
  })

  it('redirects a non-host away from the form', () => {
    server.use(http.get('/api/amenities', () => HttpResponse.json([])))

    renderForm(guest, '/host/listings/new')

    expect(screen.getByText(/my listings page/i)).toBeInTheDocument()
  })
})
