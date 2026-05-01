export interface AuthConfiguration {
  authEnabled: boolean
  isValid: boolean
  authority?: string
  clientId?: string
  scope: string
  audience?: string
  redirectUri: string
  silentRedirectUri: string
  postLogoutRedirectUri: string
  callbackPath: string
  silentCallbackPath: string
  validationErrors: string[]
}

function resolveCallbackPath(value: string, origin: string, fallbackPath: string) {
  if (!value) {
    return fallbackPath
  }

  if (value.startsWith(origin)) {
    return value.slice(origin.length) || '/'
  }

  return fallbackPath
}

export function getAuthConfiguration(): AuthConfiguration {
  const authEnabled = import.meta.env.VITE_AUTH_ENABLED === 'true'
  const origin =
    typeof window === 'undefined' ? 'http://localhost:5173' : window.location.origin

  const authority = import.meta.env.VITE_OIDC_AUTHORITY?.trim()
  const clientId = import.meta.env.VITE_OIDC_CLIENT_ID?.trim()
  const scope = import.meta.env.VITE_OIDC_SCOPE?.trim() || 'openid profile email'
  const audience = import.meta.env.VITE_OIDC_AUDIENCE?.trim()
  const redirectUri =
    import.meta.env.VITE_OIDC_REDIRECT_URI?.trim() || `${origin}/auth/callback`
  const silentRedirectUri =
    import.meta.env.VITE_OIDC_SILENT_REDIRECT_URI?.trim() ||
    `${origin}/auth/silent-callback`
  const postLogoutRedirectUri =
    import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI?.trim() || origin

  const validationErrors: string[] = []
  if (authEnabled && !authority) {
    validationErrors.push('При включённой OIDC-аутентификации требуется VITE_OIDC_AUTHORITY.')
  }
  if (authEnabled && !clientId) {
    validationErrors.push('При включённой OIDC-аутентификации требуется VITE_OIDC_CLIENT_ID.')
  }

  return {
    authEnabled,
    isValid: !authEnabled || validationErrors.length === 0,
    authority,
    clientId,
    scope,
    audience,
    redirectUri,
    silentRedirectUri,
    postLogoutRedirectUri,
    callbackPath: resolveCallbackPath(redirectUri, origin, '/auth/callback'),
    silentCallbackPath: resolveCallbackPath(
      silentRedirectUri,
      origin,
      '/auth/silent-callback',
    ),
    validationErrors,
  }
}
