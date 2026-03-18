import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { appVersionsApi, type BackendAppVersion } from '@/api/appVersions.api'
import type { AppVersion } from '@/types/appversion.types'

function toAppVersion(backend: BackendAppVersion): AppVersion {
  return {
    applicationName: backend.applicationName,
    version: backend.version,
    descriptor: backend.descriptor,
    doi: backend.doi,
    visible: backend.visible,
    resources: backend.resources,
    tags: backend.tags,
    settings: backend.settings,
    source: backend.source,
    note: backend.note
  }
}

export const useAppVersionsStore = defineStore('appversions', () => {
  const appversions = ref<AppVersion[]>([])
  const totalCount = ref(0)
  const isLoading = ref(false)
  const searchQuery = ref('')


  async function fetchAppVersions(appName: string): Promise<AppVersion[]> {
    isLoading.value = true
    try {
      const page = await appVersionsApi.getAll(appName, 0, 50)
      appversions.value = page.data.map(toAppVersion)
      totalCount.value = page.total
    } finally {
      isLoading.value = false
    }
    return appversions.value
  }

  async function fetchAppVersion(appName: string, version: string): Promise<AppVersion> {
    const backend = await appVersionsApi.getByVersion(appName, version)
    return toAppVersion(backend)
  }

  return {
    appversions,
    totalCount,
    isLoading,
    searchQuery,
    fetchAppVersions,
    fetchAppVersion,
  }
})
