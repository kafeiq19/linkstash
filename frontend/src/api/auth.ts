import { http } from './client'
import type { AuthResult, User } from './types'

export function register(username: string, password: string) {
  return http.post<AuthResult>('/auth/register', { username, password }, false)
}

export function login(username: string, password: string) {
  return http.post<AuthResult>('/auth/login', { username, password }, false)
}

export function me() {
  return http.get<{ user: User }>('/auth/me').then((d) => d.user)
}
