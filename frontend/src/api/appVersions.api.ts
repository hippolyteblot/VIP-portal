import { backendClient } from './client'
import type { AppVersion } from '@/types/appversion.types'
import type { PrecisePage } from '@/types/application.types'

export interface BackendAppVersion {
  applicationName: string
  version: string
  descriptor: string | null
  doi: string | null
  visible: boolean
  resources: { name: string; status: boolean; configuration: string }[]
  tags: string[]
  settings: {}[]
  source: string | null
  note: string | null
}

export interface CreateAppVersionPayload {
  applicationName: string
  version: string
  descriptor: string | null
  doi?: string | null
  visible: boolean
  source?: string | null
  note?: string | null
  resources?: { name: string; status: boolean; configuration: string }[]
  tags?: string[]
  settings?: Record<string, string>
}

export const appVersionsApi = {
  getAll: (applicationId: string, offset = 0, quantity = 50) => {
    return backendClient
      .get<PrecisePage<BackendAppVersion>>(
        `/internal/applications/${encodeURIComponent(applicationId)}/versions`
      )
      .then((r) => r.data)
  },

  getByVersion: (applicationId: string, version: string) =>
    backendClient
      .get<BackendAppVersion>(
        `/internal/applications/${encodeURIComponent(applicationId)}/versions/${encodeURIComponent(version)}`,
      )
      .then((r) => r.data),

  exists: async (applicationName: string, version: string) => {
    const page = await appVersionsApi.getAll(applicationName)
    console.log(`Checking existence of version '${version}' for application '${applicationName}' among ${page.total} versions.`)
    console.log('Versions retrieved:', page.data.map(v => v.version))
    return page.data.some((v) => v.version === version)
  },
  
  create: (payload: CreateAppVersionPayload) =>
    backendClient
      .post<AppVersion>(
        `/internal/applications/${encodeURIComponent(payload.applicationName)}/versions`,
        {
          applicationName: payload.applicationName,
          version: payload.version,
          descriptor: payload.descriptor,
          doi: payload.doi ?? null,
          visible: payload.visible,
          source: payload.source ?? null,
          note: payload.note ?? null,
          resources: payload.resources ?? [],
          tags: payload.tags ?? [],
          settings: payload.settings ?? {},
        },
      )
      .then((r) => r.data),

  createOrUpdate: (payload: CreateAppVersionPayload) =>
    backendClient
      .put<AppVersion>(
        `/internal/applications/${encodeURIComponent(payload.applicationName)}/versions/${encodeURIComponent(payload.version)}`,
        {
          applicationName: payload.applicationName,
          version: payload.version,
          descriptor: payload.descriptor,
          doi: payload.doi ?? null,
          visible: payload.visible,
          source: payload.source ?? null,
          note: payload.note ?? null,
          resources: payload.resources ?? [],
          tags: payload.tags ?? [],
          settings: payload.settings ?? {},
        },
      )
      .then((r) => r.data),
}