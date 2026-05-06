import { createContext, useContext } from 'react'

export type AuthStatus =
  | 'disabled'
  | 'loading'
  | 'authenticated'
  | 'unauthenticated'
  | 'error'

export interface AuthContextValue {
  authEnabled: boolean
  status: AuthStatus
  userName?: string
  authorities: string[]
  permissionsLoaded: boolean
  permissions: {
    canViewPersonnel: boolean
    canManageEvents: boolean
  }
  hasAuthority: (requiredAuthorities: string | string[]) => boolean
  signIn: () => Promise<void>
  signOut: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }

  return context
}
