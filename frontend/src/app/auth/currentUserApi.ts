import { apiRequest } from '@/shared/api/http'

export interface CurrentUserResponse {
  principalId: string
  authorities: string[]
  permissions: {
    canViewPersonnel: boolean
  }
}

export function getCurrentUser() {
  return apiRequest<CurrentUserResponse>('/api/v1/me')
}
