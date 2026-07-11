import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../auth/authContext'
import type { AuthContextValue } from '../auth/authContext'
import type { User } from '../api/types'
import { ProtectedRoute } from './ProtectedRoute'

function renderGuard(partial: Partial<AuthContextValue>) {
  const value: AuthContextValue = {
    user: null,
    loading: false,
    login: vi.fn(),
    register: vi.fn(),
    demoLogin: vi.fn(),
    logout: vi.fn(),
    ...partial,
  }
  render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<div>Protected content</div>} />
          </Route>
          <Route path="/login" element={<div>Login page</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

const guest: User = { id: 1, email: 'a@b.com', name: 'A', demo: false, roles: ['GUEST'] }

describe('ProtectedRoute', () => {
  it('shows a loading state while auth is resolving', () => {
    renderGuard({ loading: true })
    expect(screen.getByText(/loading/i)).toBeInTheDocument()
  })

  it('redirects to login when unauthenticated', () => {
    renderGuard({ user: null, loading: false })
    expect(screen.getByText(/login page/i)).toBeInTheDocument()
  })

  it('renders the protected content when authenticated', () => {
    renderGuard({ user: guest, loading: false })
    expect(screen.getByText(/protected content/i)).toBeInTheDocument()
  })
})
