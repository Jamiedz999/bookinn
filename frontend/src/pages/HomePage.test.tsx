import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext } from '../auth/authContext'
import type { AuthContextValue } from '../auth/authContext'
import type { User } from '../api/types'
import { HomePage } from './HomePage'

function renderHome(user: User) {
  const value: AuthContextValue = {
    user,
    loading: false,
    login: vi.fn(),
    register: vi.fn(),
    demoLogin: vi.fn(),
    becomeHost: vi.fn(),
    logout: vi.fn(),
  }
  render(
    <AuthContext.Provider value={value}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

const guest: User = { id: 1, email: 'g@b.com', name: 'Gwen', demo: false, roles: ['GUEST'] }
const host: User = { id: 2, email: 'h@b.com', name: 'Hank', demo: false, roles: ['GUEST', 'HOST'] }

function linkTo(name: RegExp) {
  return screen.getByRole('link', { name })
}

describe('HomePage', () => {
  it('shows guest entries (browse + my bookings) to a GUEST-only user', () => {
    renderHome(guest)

    expect(linkTo(/browse stays/i)).toHaveAttribute('href', '/search')
    expect(linkTo(/my bookings/i)).toHaveAttribute('href', '/bookings/my')
  })

  it('hides host entries from a GUEST-only user', () => {
    renderHome(guest)

    expect(screen.queryByRole('link', { name: /my listings/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /dashboard/i })).not.toBeInTheDocument()
  })

  it('shows the full set of entries to a HOST', () => {
    renderHome(host)

    expect(linkTo(/browse stays/i)).toHaveAttribute('href', '/search')
    expect(linkTo(/my bookings/i)).toHaveAttribute('href', '/bookings/my')
    expect(linkTo(/my listings/i)).toHaveAttribute('href', '/host/listings')
    expect(linkTo(/dashboard/i)).toHaveAttribute('href', '/host/dashboard')
  })
})
