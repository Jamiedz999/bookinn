import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'
import { AuthContext } from '../auth/authContext'
import type { AuthContextValue } from '../auth/authContext'
import type { User } from '../api/types'
import { server } from '../test/mocks/server'
import { HostListingsPage } from './HostListingsPage'

const guest: User = { id: 1, email: 'g@b.com', name: 'G', demo: false, roles: ['GUEST'] }
const host: User = { id: 2, email: 'h@b.com', name: 'H', demo: false, roles: ['GUEST', 'HOST'] }

function renderPage(user: User, overrides: Partial<AuthContextValue> = {}) {
  const value: AuthContextValue = {
    user,
    loading: false,
    login: vi.fn(),
    register: vi.fn(),
    demoLogin: vi.fn(),
    becomeHost: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn(),
    ...overrides,
  }
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <AuthContext.Provider value={value}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <HostListingsPage />
        </MemoryRouter>
      </QueryClientProvider>
    </AuthContext.Provider>,
  )
  return value
}

describe('HostListingsPage', () => {
  it('offers to become a host when the user lacks the HOST role', async () => {
    const value = renderPage(guest)

    await userEvent.click(screen.getByRole('button', { name: /become a host/i }))

    await waitFor(() => expect(value.becomeHost).toHaveBeenCalled())
  })

  it("lists the host's own listings", async () => {
    server.use(
      http.get('/api/host/listings', () =>
        HttpResponse.json([
          {
            id: 5,
            title: 'Sea view loft',
            city: 'Lisbon',
            pricePerNight: 120,
            status: 'ACTIVE',
            coverPhotoUrl: null,
          },
        ]),
      ),
    )

    renderPage(host)

    expect(await screen.findByText('Sea view loft')).toBeInTheDocument()
    expect(screen.getByText(/lisbon/i)).toBeInTheDocument()
  })
})
