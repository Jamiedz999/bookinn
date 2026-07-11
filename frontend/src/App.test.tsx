import { describe, expect, it } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { server } from './test/mocks/server'
import App from './App'

describe('App', () => {
  it('redirects an unauthenticated visitor to the login page', async () => {
    // No session: the bootstrap getMe 401s and the cookie refresh also fails.
    server.use(
      http.get('/api/users/me', () => new HttpResponse(null, { status: 401 })),
      http.post('/api/auth/refresh', () => new HttpResponse(null, { status: 401 })),
    )

    render(<App />)

    expect(await screen.findByText(/sign in to bookinn/i)).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByText(/loading/i)).not.toBeInTheDocument())
  })
})
