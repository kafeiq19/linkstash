import type { ApiEnvelope } from './types'

const TOKEN_KEY = 'linkstash_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null): void {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export class ApiError extends Error {
  readonly code: number
  readonly status: number

  constructor(message: string, code: number, status: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

type QueryValue = string | number | boolean | undefined | null

function buildQuery(params?: Record<string, QueryValue>): string {
  if (!params) return ''
  const usp = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v === undefined || v === null || v === '') continue
    usp.set(k, String(v))
  }
  const s = usp.toString()
  return s ? `?${s}` : ''
}

async function request<T>(
  method: string,
  path: string,
  options: { body?: unknown; query?: Record<string, QueryValue>; auth?: boolean } = {},
): Promise<T> {
  const { body, query, auth = true } = options
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }

  let res: Response
  try {
    res = await fetch(`/api${path}${buildQuery(query)}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError('网络错误，请检查连接', -1, 0)
  }

  let payload: ApiEnvelope<T> | null = null
  try {
    payload = (await res.json()) as ApiEnvelope<T>
  } catch {
    payload = null
  }

  if (!res.ok || (payload && payload.code !== 0)) {
    const message =
      payload?.message || (res.status === 401 ? '登录已过期，请重新登录' : `请求失败 (${res.status})`)
    if (res.status === 401) {
      setToken(null)
      window.dispatchEvent(new CustomEvent('linkstash:unauthorized'))
    }
    throw new ApiError(message, payload?.code ?? res.status, res.status)
  }

  if (!payload) throw new ApiError('响应格式错误', -1, res.status)
  return payload.data
}

export const http = {
  get: <T>(path: string, query?: Record<string, QueryValue>, auth = true) =>
    request<T>('GET', path, { query, auth }),
  post: <T>(path: string, body?: unknown, auth = true) =>
    request<T>('POST', path, { body, auth }),
  patch: <T>(path: string, body?: unknown, auth = true) =>
    request<T>('PATCH', path, { body, auth }),
  delete: <T>(path: string, auth = true) => request<T>('DELETE', path, { auth }),
}
