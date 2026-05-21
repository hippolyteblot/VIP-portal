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

export interface BackendGroupMessage {
  id: number
  sender?: BackendUser | null
  groupName?: string
  title?: string
  message?: string
  posted?: string
  postedDate?: string | number
}

interface SendMessageRequest {
  recipients: string[]
  subject: string
  message: string
  isGroupMessage?: boolean
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

  getGroupMessages: (groupName: string, startDate?: number) =>
    backendClient
      .get<BackendGroupMessage[]>('/internal/messages/groups', {
        params: startDate ? { groupName, startDate } : { groupName },
      })
      .then((r) => r.data),

  send: (payload: SendMessageRequest) =>
    backendClient.post('/internal/messages', payload).then((r) => r.data),

  deleteReceived: (id: number) =>
    backendClient.delete(`/internal/messages/${id}`).then((r) => r.data),

  deleteSent: (id: number) =>
    backendClient.delete(`/internal/messages/send/${id}`).then((r) => r.data),
}
