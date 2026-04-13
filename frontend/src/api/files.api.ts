import { backendClient } from './client'

export interface BackendData {
  name: string;
  type: string;
  length: number;
  modificationDate: string;
  replicas: string[];
  permissions: string;
}

interface StorageMetadata extends BackendData {}

interface StorageOperationResponse {
  operationId: string
  status: string
}

const TERMINAL_SUCCESS_STATUS = 'Done'
const TERMINAL_ERROR_STATUSES = new Set(['Failed', 'Rescheduled'])
const DEFAULT_POLL_INTERVAL_MS = 1000
const DEFAULT_POLL_TIMEOUT_MS = 5 * 60 * 1000
  

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

  // Send something like /internal/storage/directories/vip/home?refresh=false
  async listChildren(id: string, refresh = false): Promise<BackendData[]> {
    const normalizedPath = toStorageUrlPath(id)
    const endpoint = normalizedPath.length > 0
      ? `/internal/storage/directories/${normalizedPath}`
      : '/internal/storage/directories'

    const response = await backendClient.get<BackendData[]>(endpoint, {
      params: { refresh },
    })
    return response.data
  },

  async getMetadata(id: string): Promise<StorageMetadata> {
    const normalizedPath = toStorageUrlPath(id)
    const endpoint = normalizedPath.length > 0
      ? `/internal/storage/${normalizedPath}`
      : '/internal/storage'

    const response = await backendClient.get<StorageMetadata>(endpoint)
    return response.data
  },

  async downloadFile(id: string): Promise<Blob> {
    const submitResponse = await backendClient.post<StorageOperationResponse>(
      '/internal/storage/downloads',
      { path: id },
    )

    const operationId = submitResponse.data.operationId
    await waitForOperationCompletion(operationId)

    const contentResponse = await backendClient.get(`/internal/storage/downloads/${encodeURIComponent(operationId)}/content`, {
      responseType: 'blob',
      validateStatus: (status) => status >= 200 && status < 300,
    })
    return contentResponse.data
  },

  async uploadFile(destinationPath: string, file: File): Promise<void> {
    const formData = new FormData()
    formData.append('file', file)

    const normalizedDirectoryPath = destinationPath.endsWith('/')
      ? destinationPath
      : `${destinationPath}/`

    const submitResponse = await backendClient.post<StorageOperationResponse>(
      '/internal/storage/uploads',
      formData,
      {
        params: {
          path: normalizedDirectoryPath,
        },
      },
    )

    await waitForOperationCompletion(submitResponse.data.operationId)
  },

  async deleteFile(id: string): Promise<void> {
    const normalizedPath = toStorageUrlPath(id)
    const endpoint = normalizedPath.length > 0
      ? `/internal/storage/${normalizedPath}`
      : '/internal/storage'

    await backendClient.delete(endpoint)
  },

  async deleteDirectory(id: string): Promise<void> {
    const normalizedPath = toStorageUrlPath(id)
    const endpoint = normalizedPath.length > 0
      ? `/internal/storage/directories/${normalizedPath}`
      : '/internal/storage/directories'

    await backendClient.delete(endpoint)
  },

  async createDirectory(path: string, name: string): Promise<void> {
    await backendClient.post('/internal/storage/directories', { path, name })
  }
}

async function waitForOperationCompletion(
  operationId: string,
  pollIntervalMs = DEFAULT_POLL_INTERVAL_MS,
  timeoutMs = DEFAULT_POLL_TIMEOUT_MS,
): Promise<void> {
  const startedAt = Date.now()

  while (Date.now() - startedAt < timeoutMs) {
    const response = await backendClient.get<StorageOperationResponse>(
      `/internal/storage/operations/${encodeURIComponent(operationId)}`,
    )
    const status = response.data.status

    if (status === TERMINAL_SUCCESS_STATUS) {
      return
    }

    if (TERMINAL_ERROR_STATUSES.has(status)) {
      throw new Error(`Storage operation failed with status ${status}`)
    }

    await delay(pollIntervalMs)
  }

  throw new Error(`Storage operation timed out after ${timeoutMs} ms`)
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms)
  })
}
