import { describe, expect, it, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { server } from '../test/mocks/server'
import { api, setAuthFailureHandler } from './client'
import { getMe, login } from './auth'
import { getAccessToken, setAccessToken } from './tokenStore'

const userBody = { id: 1, email: 'a@b.com', name: 'A', demo: false, roles: ['GUEST'] }

describe('api client', () => {
  it('attaches the in-memory access token as a Bearer header', async () => {
    setAccessToken('abc')
    server.use(
      http.get('/api/users/me', ({ request }) =>
        HttpResponse.json({ auth: request.headers.get('Authorization') }),
      ),
    )

    const response = await api.get<{ auth: string }>('/users/me')

    expect(response.data.auth).toBe('Bearer abc')
  })

  it('refreshes the token and replays the original request on a 401', async () => {
    let refreshCalls = 0
    server.use(
      http.get('/api/users/me', ({ request }) => {
        if (request.headers.get('Authorization') !== 'Bearer new-token') {
          return new HttpResponse(null, { status: 401 })
        }
        return HttpResponse.json(userBody)
      }),
      http.post('/api/auth/refresh', () => {
        refreshCalls += 1
        return HttpResponse.json({ accessToken: 'new-token' })
      }),
    )

    const user = await getMe()

    expect(user.email).toBe('a@b.com')
    expect(refreshCalls).toBe(1)
    expect(getAccessToken()).toBe('new-token')
  })

  it('clears the token and notifies the failure handler when refresh fails', async () => {
    const onFailure = vi.fn()
    setAuthFailureHandler(onFailure)
    setAccessToken('stale')
    server.use(
      http.get('/api/users/me', () => new HttpResponse(null, { status: 401 })),
      http.post('/api/auth/refresh', () => new HttpResponse(null, { status: 401 })),
    )

    await expect(getMe()).rejects.toBeDefined()
    expect(onFailure).toHaveBeenCalledOnce()
    expect(getAccessToken()).toBeNull()
  })

  it('retries only once, then rejects if the request still fails after refresh', async () => {
    let refreshCalls = 0
    server.use(
      http.get('/api/users/me', () => new HttpResponse(null, { status: 401 })),
      http.post('/api/auth/refresh', () => {
        refreshCalls += 1
        return HttpResponse.json({ accessToken: 'new-token' })
      }),
    )

    await expect(getMe()).rejects.toBeDefined()
    expect(refreshCalls).toBe(1)
  })

  it('does not attempt a refresh when an auth endpoint itself returns 401', async () => {
    let refreshCalls = 0
    server.use(
      http.post('/api/auth/login', () => new HttpResponse(null, { status: 401 })),
      http.post('/api/auth/refresh', () => {
        refreshCalls += 1
        return HttpResponse.json({ accessToken: 'x' })
      }),
    )

    await expect(login({ email: 'a@b.com', password: 'wrong' })).rejects.toBeDefined()
    expect(refreshCalls).toBe(0)
  })
})
