import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { setAccessToken } from '../api/tokenStore'
import { setAuthFailureHandler } from '../api/client'
import { server } from './mocks/server'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  server.resetHandlers()
  setAccessToken(null)
  setAuthFailureHandler(null)
})

afterAll(() => server.close())
