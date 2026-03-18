import { backendClient } from './client'
import type { PrecisePage } from '@/types/tag.types'


export interface BackendTag {
  name: string
  fullName: string | null
  citation: string | null
  owner: string | null
  groups: { name: string; publicGroup: boolean; type: string; auto: boolean }[]
  note: string | null
}

export interface TagCreatePayload {
  name: string
  fullName?: string | null
  citation?: string | null
  owner?: string | null
  note?: string | null
}

export const tagsApi = {
  getAll: (offset = 0, quantity = 50, group?: string) => {
    const params: Record<string, string | number> = { offset, quantity }
    if (group) params.group = group
    return backendClient
      .get<PrecisePage<BackendTag>>('/internal/tags', { params })
      .then((r) => r.data)
  },

}
