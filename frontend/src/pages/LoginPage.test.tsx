import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext } from '../auth/authContext'
import type { AuthContextValue } from '../auth/authContext'
import { LoginPage } from './LoginPage'

function renderLogin(overrides: Partial<AuthContextValue> = {}) {
  const value: AuthContextValue = {
    user: null,
    loading: false,
    login: vi.fn().mockResolvedValue(undefined),
    register: vi.fn().mockResolvedValue(undefined),
    demoLogin: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
  render(
    <AuthContext.Provider value={value}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
  return value
}

describe('LoginPage', () => {
  it('submits email and password to login', async () => {
    const value = renderLogin()

    await userEvent.type(screen.getByLabelText(/email/i), 'user@bookinn.app')
    await userEvent.type(screen.getByLabelText(/password/i), 'password123')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))

    expect(value.login).toHaveBeenCalledWith('user@bookinn.app', 'password123')
  })

  it('signs in with one click via the demo button', async () => {
    const value = renderLogin()

    await userEvent.click(screen.getByRole('button', { name: /try as guest/i }))

    expect(value.demoLogin).toHaveBeenCalledWith('GUEST')
  })

  it('shows an error when login fails', async () => {
    renderLogin({ login: vi.fn().mockRejectedValue(new Error('bad')) })

    await userEvent.type(screen.getByLabelText(/email/i), 'user@bookinn.app')
    await userEvent.type(screen.getByLabelText(/password/i), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText(/invalid email or password/i)).toBeInTheDocument()
  })
})
