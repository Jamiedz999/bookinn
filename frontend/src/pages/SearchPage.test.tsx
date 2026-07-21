import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import { SearchPage } from './SearchPage'

interface Summary {
  id: number
  title: string
  city: string
  pricePerNight: number
  status: string
  coverPhotoUrl: string | null
}

function pageOf(content: Summary[]) {
  return { content, page: 0, size: 12, totalElements: content.length, totalPages: 1 }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <SearchPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('SearchPage', () => {
  it('lists active listings on load', async () => {
    server.use(
      http.get('/api/listings', () =>
        HttpResponse.json(
          pageOf([
            { id: 1, title: 'Sea view loft', city: 'Lisbon', pricePerNight: 120, status: 'ACTIVE', coverPhotoUrl: null },
          ]),
        ),
      ),
    )

    renderPage()

    expect(await screen.findByText('Sea view loft')).toBeInTheDocument()
  })

  it('sends the city filter when searching', async () => {
    let lastUrl = ''
    server.use(
      http.get('/api/listings', ({ request }) => {
        lastUrl = request.url
        const city = new URL(request.url).searchParams.get('city')
        return HttpResponse.json(
          pageOf(
            city === 'Porto'
              ? [{ id: 2, title: 'Porto flat', city: 'Porto', pricePerNight: 90, status: 'ACTIVE', coverPhotoUrl: null }]
              : [],
          ),
        )
      }),
    )

    renderPage()
    await userEvent.type(screen.getByLabelText(/city/i), 'Porto')
    await userEvent.click(screen.getByRole('button', { name: /search/i }))

    expect(await screen.findByText('Porto flat')).toBeInTheDocument()
    await waitFor(() => expect(lastUrl).toContain('city=Porto'))
  })

  it('warns and does not search when only one date is provided', async () => {
    let calls = 0
    server.use(
      http.get('/api/listings', () => {
        calls += 1
        return HttpResponse.json(pageOf([]))
      }),
    )

    renderPage()
    // Let the initial browse-all request settle.
    await screen.findByText(/no places match/i)
    const callsAfterLoad = calls

    fireEvent.change(screen.getByLabelText(/check-in/i), { target: { value: '2026-09-10' } })
    await userEvent.click(screen.getByRole('button', { name: /search/i }))

    expect(await screen.findByText(/both check-in and check-out/i)).toBeInTheDocument()
    expect(calls).toBe(callsAfterLoad) // no extra request fired
  })
})
