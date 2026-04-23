import { backendClient } from './client'
import type { RegisterPayload } from '@/types/auth.types'
import type { Group } from '@/types/group.types'
import type { ProfileUpdatePayload, ProfileUser } from '@/types/profile.types'

interface SignUpUserPayload {
  id: null
  firstName: string
  lastName: string
  email: string
  password: string
  countryCode: string
  institution: string
  groups: Group[]
}

interface SignUpFormPayload {
  user: SignUpUserPayload
  comment: string
}

function toSignUpFormPayload(payload: RegisterPayload): SignUpFormPayload {
  return {
    user: {
      id: null,
      firstName: payload.firstName,
      lastName: payload.lastName,
      email: payload.email,
      password: payload.password,
      countryCode: payload.countryCode.toLowerCase(),
      institution: payload.institution,
      // Backend signup expects a non-null groups collection.
      groups: [],
    },
    comment: payload.comments,
  }
}

export const usersApi = {
  register: (payload: RegisterPayload) =>
    backendClient.post('/internal/users', toSignUpFormPayload(payload)).then((r) => r.data),

  getById: (id: string) =>
    backendClient.get<ProfileUser>(`/internal/users/${encodeURIComponent(id)}`).then((r) => r.data),

  update: (id: string, payload: ProfileUpdatePayload) =>
    backendClient.put<ProfileUser>(`/internal/users/${encodeURIComponent(id)}`, payload).then((r) => r.data),

  remove: (id: string) =>
    backendClient.delete<void>(`/internal/users/${encodeURIComponent(id)}`).then((r) => r.data),
}
