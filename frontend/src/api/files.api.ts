import { backendClient } from './client'

export interface BackendData {
  name: string;
  type: string;
  length: number;
  modificationDate: string;
  replicas: string[];
  permissions: string;
}
  

  function toStorageUrlPath(path: string): string {
    const normalized = path.trim().replace(/\/{2,}/g, '/')
    const withoutLeadingSlash = normalized.startsWith('/') ? normalized.slice(1) : normalized
    return withoutLeadingSlash
      .split('/')
      .filter(Boolean)
      .map((segment) => encodeURIComponent(segment))
      .join('/')
}

export const filesApi = {

  // Send something like /internal/storage/vip/home?refresh=false
  async listChildren(id: string, refresh = false): Promise<BackendData[]> {
    const response = await backendClient.get<BackendData[]>(`/internal/storage/${toStorageUrlPath(id)}`, {
      params: { refresh },
    })
    return response.data
  },

  async downloadFile(id: string): Promise<Blob> {
    const response = await backendClient.get(`/internal/storage/${toStorageUrlPath(id)}`, {
      params: { download: true },
      responseType: 'blob',
    })
    return response.data
  },

  async uploadFile(destinationPath: string, file: File): Promise<void> {
    await backendClient.put(
      `/internal/storage/${toStorageUrlPath(destinationPath)}`,
      file,
      {
        headers: {
          'Content-Type': file.type || 'application/octet-stream',
        },
      }
    )
  },

  async deleteFile(id: string): Promise<void> {
    await backendClient.delete(`/internal/storage/${toStorageUrlPath(id)}`)
  }
}
