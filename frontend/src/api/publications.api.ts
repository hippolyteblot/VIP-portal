import { backendClient } from './client'
import type { Publication, PublicationInput } from '@/types/publication.types'

export const publicationsApi = {
  getAll: () =>
    backendClient.get<Publication[]>('/internal/publications').then((r) => r.data),

  getById: (id: number) =>
    backendClient.get<Publication>(`/internal/publications/${id}`).then((r) => r.data),

  create: (payload: PublicationInput) =>
    backendClient.post<void>('/internal/publications', payload).then((r) => r.data),

  update: (id: number, payload: PublicationInput) =>
    backendClient.put<void>(`/internal/publications/${id}`, payload).then((r) => r.data),

  remove: (id: number) =>
    backendClient.delete<void>(`/internal/publications/${id}`).then((r) => r.data),
}
