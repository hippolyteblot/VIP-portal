import { backendClient } from './client'
import type { RegisterPayload } from '@/types/auth.types'

interface SignUpFormPayload {
  user: {
    id: null
    firstName: string
    lastName: string
    email: string
    password: string
    countryCode: string
    institution: string
    groups: unknown[]
  }
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
}
