import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
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

function renderDetail(id: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/listings/${id}`]}>
        <Routes>
          <Route path="/listings/:id" element={<ListingDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
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
})
