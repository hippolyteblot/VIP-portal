import { backendClient } from './client'

interface BackendUser {
  id?: string
  email?: string
  firstName?: string
  lastName?: string
}

export interface BackendMessage {
  id: number
  sender?: BackendUser | null
  receivers?: BackendUser[]
  title?: string
  message?: string
  posted?: string
  postedDate?: string | number
  read?: boolean
}

interface SendMessageRequest {
  recipients: string[]
  subject: string
  message: string
}

export const messagesApi = {
  getReceived: (startDate?: number) =>
    backendClient
      .get<BackendMessage[]>('/internal/messages', {
        params: startDate ? { startDate } : undefined,
      })
      .then((r) => r.data),

  getSent: (startDate?: number) =>
    backendClient
      .get<BackendMessage[]>('/internal/messages/send', {
        params: startDate ? { startDate } : undefined,
      })
      .then((r) => r.data),

  send: (payload: SendMessageRequest) =>
    backendClient.post('/internal/messages', payload).then((r) => r.data),

  deleteReceived: (id: number) =>
    backendClient.delete(`/internal/messages/${id}`).then((r) => r.data),

  deleteSent: (id: number) =>
    backendClient.delete(`/internal/messages/send/${id}`).then((r) => r.data),
}
