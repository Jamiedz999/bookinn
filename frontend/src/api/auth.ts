import { api } from './client'
import { setAccessToken } from './tokenStore'
import type { AuthResponse, LoginRequest, RegisterRequest, Role, User } from './types'

export async function register(body: RegisterRequest): Promise<User> {
  const response = await api.post<User>('/auth/register', body)
  return response.data
}

export async function login(body: LoginRequest): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>('/auth/login', body)
  setAccessToken(response.data.accessToken)
  return response.data
}

export async function demoLogin(role: Role): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>('/auth/demo-login', { role })
  setAccessToken(response.data.accessToken)
  return response.data
}

export async function logout(): Promise<void> {
  await api.post('/auth/logout')
  setAccessToken(null)
}

export async function getMe(): Promise<User> {
  const response = await api.get<User>('/users/me')
  return response.data
}
