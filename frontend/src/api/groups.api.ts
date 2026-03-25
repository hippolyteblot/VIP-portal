import { backendClient } from './client'
import type { PrecisePage } from '@/types/application.types'
import type { Group } from '@/types/group.types'

export type BackendGroup = Group

export const groupsApi = {
  getAll: (offset = 0, quantity = 50) => {
    const params: Record<string, number> = { offset, quantity }

    return backendClient
      .get<PrecisePage<BackendGroup>>('/internal/groups', { params })
      .then((r) => r.data)
  },
}
