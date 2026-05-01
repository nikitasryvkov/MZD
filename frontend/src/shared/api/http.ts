import { getCurrentAccessToken } from '@/app/auth/authSession'

export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiErrorPayload {
  type?: string
  message?: string
  requestId?: string
  traceId?: string
  fieldErrors?: ApiFieldError[]
  currentStatus?: string
  requestedStatus?: string
  allowedTransitions?: string[]
}

const FALLBACK_API_BASE_PATH = '/api'
export class ApiError extends Error {
  status: number
  payload?: ApiErrorPayload

  constructor(status: number, payload?: ApiErrorPayload, message?: string) {
    super(message ?? payload?.message ?? 'Запрос к серверу завершился ошибкой.')
    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

function trimTrailingSlash(value: string) {
  return value.endsWith('/') ? value.slice(0, -1) : value
}

function resolveApiRequestUrl(path: string) {
  if (/^https?:\/\//i.test(path)) {
    return path
  }

  const baseUrl = getApiBaseUrl()
  let normalizedPath = path.startsWith('/') ? path : `/${path}`

  if (baseUrl.endsWith('/api') && normalizedPath.startsWith('/api/')) {
    normalizedPath = normalizedPath.slice(4)
  }

  return `${baseUrl}${normalizedPath}`
}

export function getApiBaseUrl() {
  const explicitUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  if (explicitUrl) {
    return trimTrailingSlash(explicitUrl)
  }

  if (typeof window === 'undefined') {
    return 'http://localhost:8080/api'
  }

  return trimTrailingSlash(FALLBACK_API_BASE_PATH)
}

export function getWebSocketUrl() {
  const explicitUrl = import.meta.env.VITE_WS_URL?.trim()
  if (explicitUrl) {
    return explicitUrl
  }

  if (typeof window === 'undefined') {
    return 'ws://localhost:8080/ws'
  }

  return `${window.location.origin.replace(/^http/i, 'ws')}/ws`
}

export function getAuthToken() {
  return getCurrentAccessToken()
}

async function parseResponsePayload(response: Response) {
  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) {
    return undefined
  }

  return (await response.json()) as ApiErrorPayload
}

export async function apiRequest<T>(path: string, init: RequestInit = {}) {
  const token = getAuthToken()
  const headers = new Headers(init.headers)

  headers.set('Accept', 'application/json')

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  try {
    const response = await fetch(resolveApiRequestUrl(path), {
      ...init,
      headers,
    })

    if (!response.ok) {
      throw new ApiError(response.status, await parseResponsePayload(response))
    }

    if (response.status === 204) {
      return undefined as T
    }

    return (await response.json()) as T
  } catch (error) {
    if (error instanceof ApiError || error instanceof DOMException) {
      throw error
    }

    throw new ApiError(0, undefined, 'Не удалось подключиться к серверу.')
  }
}
