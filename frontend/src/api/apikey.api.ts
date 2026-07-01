import { backendClient } from './client'

export const apikeyApi = {
  get(): Promise<string> {
    return backendClient.get<string>('/internal/apikey').then((r) => r.data)
  },

  generateNew(): Promise<string> {
    return backendClient.get<string>('/internal/apikey', { params: { new: true } }).then((r) => r.data)
  },

  delete(): Promise<void> {
    return backendClient.delete<void>('/internal/apikey').then((r) => r.data)
  },
}
