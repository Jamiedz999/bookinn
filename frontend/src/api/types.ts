export type Role = 'GUEST' | 'HOST'

export interface User {
  id: number
  email: string
  name: string
  demo: boolean
  roles: Role[]
}

export interface AuthResponse {
  accessToken: string
  user: User
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  name: string
}
