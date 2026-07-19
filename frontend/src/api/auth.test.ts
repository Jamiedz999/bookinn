import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import { becomeHost, demoLogin, getMe, login, logout, register } from './auth'
import { getAccessToken, setAccessToken } from './tokenStore'

const user = { id: 7, email: 'demo@bookinn.app', name: 'Demo', demo: true, roles: ['GUEST'] }

describe('auth api', () => {
  it('register returns the created user', async () => {
    server.use(http.post('/api/auth/register', () => HttpResponse.json(user)))

    const created = await register({ email: user.email, password: 'password123', name: 'Demo' })

    expect(created.id).toBe(7)
  })

  it('login stores the access token', async () => {
    server.use(
      http.post('/api/auth/login', () =>
        HttpResponse.json({ accessToken: 'tok-login', user }),
      ),
    )

    const result = await login({ email: user.email, password: 'password123' })

    expect(result.user.email).toBe(user.email)
    expect(getAccessToken()).toBe('tok-login')
  })

  it('demoLogin stores the access token', async () => {
    server.use(
      http.post('/api/auth/demo-login', () =>
        HttpResponse.json({ accessToken: 'tok-demo', user }),
      ),
    )

    await demoLogin('HOST')

    expect(getAccessToken()).toBe('tok-demo')
  })

  it('logout clears the access token', async () => {
    setAccessToken('tok')
    server.use(http.post('/api/auth/logout', () => new HttpResponse(null, { status: 204 })))

    await logout()

    expect(getAccessToken()).toBeNull()
  })

  it('getMe returns the current user', async () => {
    setAccessToken('tok')
    server.use(http.get('/api/users/me', () => HttpResponse.json(user)))

    const me = await getMe()

    expect(me.email).toBe(user.email)
  })

  it('becomeHost grants HOST then refreshes to a token that carries it', async () => {
    setAccessToken('guest-token')
    const hostUser = { ...user, roles: ['GUEST', 'HOST'] }
    server.use(
      http.post('/api/users/me/become-host', () => HttpResponse.json(hostUser)),
      http.post('/api/auth/refresh', () =>
        HttpResponse.json({ accessToken: 'host-token', user: hostUser }),
      ),
    )

    const result = await becomeHost()

    expect(result.roles).toContain('HOST')
    expect(getAccessToken()).toBe('host-token')
  })
})
