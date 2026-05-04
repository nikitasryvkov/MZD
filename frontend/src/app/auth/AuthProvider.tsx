import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren,
} from 'react'
import { UserManager, WebStorageStateStore, type User } from 'oidc-client-ts'
import { useLocation, useNavigate } from 'react-router-dom'
import { AuthContext, type AuthContextValue, type AuthStatus } from './authContext'
import { getAuthConfiguration } from './authConfig'
import { getCurrentUser, type CurrentUserResponse } from './currentUserApi'
import { setCurrentAccessToken } from './authSession'
import styles from './AuthProvider.module.css'

const SKIP_AUTO_SIGN_IN_KEY = 'mzd-dashboard.skip-auto-sign-in'
const EMPTY_AUTH_PERMISSIONS: CurrentUserResponse['permissions'] = {
  canViewPersonnel: false,
}

function getUserDisplayName(user?: User) {
  if (!user) {
    return undefined
  }

  const preferredUsername = user.profile.preferred_username
  if (typeof preferredUsername === 'string' && preferredUsername.trim()) {
    return preferredUsername
  }

  const fullName = user.profile.name
  if (typeof fullName === 'string' && fullName.trim()) {
    return fullName
  }

  return user.profile.sub
}

function describeError(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return 'Ошибка авторизации.'
}

function resolveSafeReturnTo(value: unknown) {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) {
    return '/'
  }

  return value
}

async function loadUser(manager: UserManager) {
  let user = await manager.getUser()
  if (user?.expired) {
    try {
      user = (await manager.signinSilent()) ?? null
    } catch {
      await manager.removeUser()
      user = null
    }
  }

  return user ?? null
}

export function AuthProvider({ children }: PropsWithChildren) {
  const configuration = useMemo(() => getAuthConfiguration(), [])
  const location = useLocation()
  const navigate = useNavigate()
  const redirectAttemptedRef = useRef(false)
  const authoritiesRequestIdRef = useRef(0)
  const [status, setStatus] = useState<AuthStatus>(
    configuration.authEnabled ? 'loading' : 'disabled',
  )
  const [user, setUser] = useState<User>()
  const [authorities, setAuthorities] = useState<string[]>([])
  const [permissions, setPermissions] = useState<CurrentUserResponse['permissions']>(
    EMPTY_AUTH_PERMISSIONS,
  )
  const [errorMessage, setErrorMessage] = useState<string>()

  const userManager = useMemo(() => {
    if (!configuration.authEnabled || !configuration.isValid) {
      return null
    }

    return new UserManager({
      authority: configuration.authority!,
      client_id: configuration.clientId!,
      redirect_uri: configuration.redirectUri,
      silent_redirect_uri: configuration.silentRedirectUri,
      post_logout_redirect_uri: configuration.postLogoutRedirectUri,
      response_type: 'code',
      scope: configuration.scope,
      automaticSilentRenew: true,
      userStore: new WebStorageStateStore({
        store: window.sessionStorage,
      }),
      extraQueryParams: configuration.audience
        ? { audience: configuration.audience }
        : undefined,
    })
  }, [configuration])

  const authMisconfigured =
    configuration.authEnabled && (!configuration.isValid || !userManager)
  const effectiveStatus: AuthStatus = !configuration.authEnabled
    ? 'disabled'
    : authMisconfigured
      ? 'error'
      : status
  const effectiveErrorMessage = !configuration.authEnabled
    ? undefined
    : authMisconfigured
      ? configuration.validationErrors.join(' ')
      : errorMessage
  const effectiveUser = effectiveStatus === 'authenticated' ? user : undefined
  const effectiveAuthorities = useMemo(
    () => (effectiveStatus === 'authenticated' ? authorities : []),
    [authorities, effectiveStatus],
  )
  const effectivePermissions = useMemo(
    () => (effectiveStatus === 'authenticated' ? permissions : EMPTY_AUTH_PERMISSIONS),
    [effectiveStatus, permissions],
  )

  const clearAuthorities = useCallback(() => {
    authoritiesRequestIdRef.current += 1
    setAuthorities([])
    setPermissions(EMPTY_AUTH_PERMISSIONS)
  }, [])

  const loadCurrentUserAuthorities = useCallback(async () => {
    const requestId = authoritiesRequestIdRef.current + 1
    authoritiesRequestIdRef.current = requestId

    try {
      const currentUser = await getCurrentUser()
      if (authoritiesRequestIdRef.current === requestId) {
        setAuthorities(currentUser.authorities ?? [])
        setPermissions(currentUser.permissions ?? EMPTY_AUTH_PERMISSIONS)
      }
    } catch {
      if (authoritiesRequestIdRef.current === requestId) {
        setAuthorities([])
        setPermissions(EMPTY_AUTH_PERMISSIONS)
      }
    }
  }, [])

  const hasAuthority = useCallback(
    (requiredAuthorities: string | string[]) => {
      const required = Array.isArray(requiredAuthorities)
        ? requiredAuthorities
        : [requiredAuthorities]

      return required.some((authority) => effectiveAuthorities.includes(authority))
    },
    [effectiveAuthorities],
  )

  const signIn = useCallback(async () => {
    const manager = userManager
    if (!manager) {
      return
    }

    window.sessionStorage.removeItem(SKIP_AUTO_SIGN_IN_KEY)
    redirectAttemptedRef.current = true
    await manager.signinRedirect({
      state: {
        returnTo: `${location.pathname}${location.search}${location.hash}`,
      },
    })
  }, [location.hash, location.pathname, location.search, userManager])

  const signOut = useCallback(async () => {
    const manager = userManager
    if (!manager) {
      return
    }

    window.sessionStorage.setItem(SKIP_AUTO_SIGN_IN_KEY, 'true')
    redirectAttemptedRef.current = false
    await manager.signoutRedirect()
  }, [userManager])

  useEffect(() => {
    if (configuration.authEnabled && configuration.isValid && userManager) {
      return
    }

    setCurrentAccessToken(undefined)
  }, [configuration.authEnabled, configuration.isValid, userManager])

  useEffect(() => {
    const manager = userManager
    if (!manager) {
      return
    }
    const activeUserManager: UserManager = manager

    function handleUserLoaded(loadedUser: User) {
      setCurrentAccessToken(loadedUser.access_token)
      setUser(loadedUser)
      setStatus('authenticated')
      setErrorMessage(undefined)
      window.sessionStorage.removeItem(SKIP_AUTO_SIGN_IN_KEY)
      redirectAttemptedRef.current = false
      void loadCurrentUserAuthorities()
    }

    function handleUserUnloaded() {
      setCurrentAccessToken(undefined)
      setUser(undefined)
      clearAuthorities()
      setStatus('unauthenticated')
      setErrorMessage(undefined)
    }

    async function handleAccessTokenExpired() {
      try {
        const renewedUser = await activeUserManager.signinSilent()
        if (!renewedUser) {
          await activeUserManager.removeUser()
          handleUserUnloaded()
          return
        }

        handleUserLoaded(renewedUser)
      } catch {
        await activeUserManager.removeUser()
        handleUserUnloaded()
      }
    }

    function handleSilentRenewError() {
      setCurrentAccessToken(undefined)
      setUser(undefined)
      clearAuthorities()
      setStatus('unauthenticated')
      setErrorMessage('Сеанс истёк. Выполните вход повторно.')
    }

    activeUserManager.events.addUserLoaded(handleUserLoaded)
    activeUserManager.events.addUserUnloaded(handleUserUnloaded)
    activeUserManager.events.addAccessTokenExpired(handleAccessTokenExpired)
    activeUserManager.events.addSilentRenewError(handleSilentRenewError)

    return () => {
      activeUserManager.events.removeUserLoaded(handleUserLoaded)
      activeUserManager.events.removeUserUnloaded(handleUserUnloaded)
      activeUserManager.events.removeAccessTokenExpired(handleAccessTokenExpired)
      activeUserManager.events.removeSilentRenewError(handleSilentRenewError)
    }
  }, [clearAuthorities, loadCurrentUserAuthorities, userManager])

  useEffect(() => {
    const manager = userManager
    if (!configuration.authEnabled || !configuration.isValid || !manager) {
      return
    }
    const activeUserManager: UserManager = manager

    let isActive = true

    async function initializeAuth() {
      setStatus('loading')
      setErrorMessage(undefined)

      try {
        if (location.pathname === configuration.callbackPath) {
          const callbackUser = await activeUserManager.signinCallback()
          if (!isActive) {
            return
          }
          if (!callbackUser) {
            throw new Error('Не удалось завершить вход.')
          }

          const returnTo =
            typeof callbackUser.state === 'object' &&
            callbackUser.state !== null &&
            'returnTo' in callbackUser.state
              ? resolveSafeReturnTo(callbackUser.state.returnTo)
              : '/'

          setCurrentAccessToken(callbackUser.access_token)
          setUser(callbackUser)
          setStatus('authenticated')
          void loadCurrentUserAuthorities()
          navigate(returnTo, { replace: true })
          return
        }

        if (location.pathname === configuration.silentCallbackPath) {
          await activeUserManager.signinSilentCallback()
          if (isActive && window.parent === window) {
            navigate('/', { replace: true })
          }
          return
        }

        const currentUser = await loadUser(activeUserManager)
        if (!isActive) {
          return
        }

        if (currentUser) {
          setCurrentAccessToken(currentUser.access_token)
          setUser(currentUser)
          setStatus('authenticated')
          window.sessionStorage.removeItem(SKIP_AUTO_SIGN_IN_KEY)
          redirectAttemptedRef.current = false
          void loadCurrentUserAuthorities()
          return
        }

        setCurrentAccessToken(undefined)
        setUser(undefined)
        clearAuthorities()
        setStatus('unauthenticated')
      } catch (error) {
        if (!isActive) {
          return
        }

        setCurrentAccessToken(undefined)
        setUser(undefined)
        clearAuthorities()
        setStatus('error')
        setErrorMessage(describeError(error))
      }
    }

    void initializeAuth()

    return () => {
      isActive = false
    }
  }, [clearAuthorities, configuration, loadCurrentUserAuthorities, location.pathname, navigate, userManager])

  useEffect(() => {
    if (
      effectiveStatus !== 'unauthenticated' ||
      !configuration.authEnabled ||
      !configuration.isValid ||
      !userManager ||
      location.pathname === configuration.callbackPath ||
      location.pathname === configuration.silentCallbackPath ||
      window.sessionStorage.getItem(SKIP_AUTO_SIGN_IN_KEY) === 'true' ||
      redirectAttemptedRef.current
    ) {
      return
    }

    void signIn()
  }, [
    configuration.authEnabled,
    configuration.callbackPath,
    configuration.isValid,
    configuration.silentCallbackPath,
    effectiveStatus,
    location.pathname,
    signIn,
    userManager,
  ])

  const contextValue = useMemo<AuthContextValue>(
    () => ({
      authEnabled: configuration.authEnabled,
      status: effectiveStatus,
      userName: getUserDisplayName(effectiveUser),
      authorities: effectiveAuthorities,
      permissions: effectivePermissions,
      hasAuthority,
      signIn,
      signOut,
    }),
    [
      configuration.authEnabled,
      effectiveAuthorities,
      effectivePermissions,
      effectiveStatus,
      effectiveUser,
      hasAuthority,
      signIn,
      signOut,
    ],
  )

  if (
    effectiveStatus === 'loading' ||
    location.pathname === configuration.callbackPath ||
    location.pathname === configuration.silentCallbackPath
  ) {
    return (
      <AuthContext.Provider value={contextValue}>
        <div className={styles.shell}>
          <div className={styles.card}>
            <p className={styles.eyebrow}>Идентификация</p>
            <h1>Выполняется вход</h1>
            <p>Подождите, идёт проверка данных.</p>
          </div>
        </div>
      </AuthContext.Provider>
    )
  }

  if (effectiveStatus === 'error' || effectiveStatus === 'unauthenticated') {
    return (
      <AuthContext.Provider value={contextValue}>
        <div className={styles.shell}>
          <div className={styles.card}>
            <p className={styles.eyebrow}>Идентификация</p>
            <h1>Требуется аутентификация</h1>
            <p>
              Для продолжения работы выполните вход через корпоративную учётную запись.
            </p>
            {effectiveErrorMessage ? (
              <div className={styles.error}>{effectiveErrorMessage}</div>
            ) : null}
            <div className={styles.actions}>
              <button
                className={styles.primaryButton}
                type="button"
                onClick={() => void signIn()}
              >
                Вход
              </button>
              {effectiveStatus === 'error' ? (
                <button
                  className={styles.secondaryButton}
                  type="button"
                  onClick={() => window.location.reload()}
                >
                  Повторить
                </button>
              ) : null}
            </div>
          </div>
        </div>
      </AuthContext.Provider>
    )
  }

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
}
