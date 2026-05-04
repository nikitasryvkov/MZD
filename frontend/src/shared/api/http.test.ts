import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from './http'

function mockFetch(response: Response) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
}

describe('apiRequest', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('returns undefined for 204 responses', async () => {
    mockFetch(new Response(null, { status: 204 }))

    await expect(apiRequest<void>('/api/v1/test')).resolves.toBeUndefined()
  })

  it('preserves HTTP status and payload for API errors', async () => {
    const payload = {
      message: 'Validation failed',
      fieldErrors: [{ field: 'timeRange.from', message: 'Required' }],
    }
    mockFetch(
      new Response(JSON.stringify(payload), {
        status: 400,
        headers: { 'content-type': 'application/json' },
      }),
    )

    await expect(apiRequest('/api/v1/test')).rejects.toMatchObject({
      status: 400,
      payload,
    })
  })

  it('does not convert malformed JSON error payloads into network errors', async () => {
    mockFetch(
      new Response('{', {
        status: 500,
        headers: { 'content-type': 'application/json' },
      }),
    )

    await expect(apiRequest('/api/v1/test')).rejects.toMatchObject({
      status: 500,
      message: 'Сервер вернул некорректный JSON-ответ.',
    })
  })

  it('fails explicitly on empty successful JSON responses', async () => {
    mockFetch(
      new Response('', {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    )

    await expect(apiRequest('/api/v1/test')).rejects.toMatchObject({
      status: 200,
      message: 'Сервер вернул пустой ответ.',
    })
  })
})
