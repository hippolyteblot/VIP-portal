import type { Group } from './group.types'

export interface ProfileUser {
  id: string
  firstName: string
  lastName: string
  email: string
  institution: string
  countryCode: string
  maxRunningSimulations: number
  level: string
  termsOfUse: string | null
  lastUpdatePublications: string | null
  groups: Group[]
  apiKey: string | null
}

export interface ProfileUpdatePayload {
  id: string
  firstName: string
  lastName: string
  email: string
  institution: string
  countryCode: string
  maxRunningSimulations: number
  level: string
  termsOfUse: string | null
  lastUpdatePublications: string | null
  groups: Group[]
}
