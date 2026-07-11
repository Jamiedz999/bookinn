import axios, { AxiosError } from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { getAccessToken, setAccessToken } from './tokenStore'

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

// Called when a refresh attempt fails, so the auth layer can drop the user and redirect to login.
let onAuthFailure: (() => void) | null = null

export function setAuthFailureHandler(handler: (() => void) | null): void {
  onAuthFailure = handler
}

export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
})

// Attach the in-memory access token to every outgoing request.
api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// A single in-flight refresh is shared by all requests that race on a 401.
let refreshPromise: Promise<string> | null = null

async function requestRefresh(): Promise<string> {
  const response = await axios.post<{ accessToken: string }>('/api/auth/refresh', null, {
    withCredentials: true,
  })
  setAccessToken(response.data.accessToken)
  return response.data.accessToken
}

function isAuthEndpoint(url: string | undefined): boolean {
  return url?.includes('/auth/') ?? false
}

// On a 401 (once per request), silently refresh the access token and replay the original request.
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined
    const isUnauthorized = error.response?.status === 401

    if (!isUnauthorized || !original || original._retry || isAuthEndpoint(original.url)) {
      return Promise.reject(error)
    }

    original._retry = true
    try {
      if (!refreshPromise) {
        refreshPromise = requestRefresh().finally(() => {
          refreshPromise = null
        })
      }
      const newToken = await refreshPromise
      original.headers.Authorization = `Bearer ${newToken}`
      return api(original)
    } catch (refreshError) {
      setAccessToken(null)
      onAuthFailure?.()
      return Promise.reject(refreshError)
    }
  },
)
