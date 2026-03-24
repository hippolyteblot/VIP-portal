import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAppVersionsStore } from './appversions.stores'

const mocked = vi.hoisted(() => ({
  getAll: vi.fn(),
  getAllForApplication: vi.fn(),
  getByVersion: vi.fn(),
}))

vi.mock('@/api/appVersions.api', () => ({
  appVersionsApi: {
    getAll: mocked.getAll,
    getAllForApplication: mocked.getAllForApplication,
    getByVersion: mocked.getByVersion,
  },
}))

describe('useAppVersionsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetches all versions and updates loading + total', async () => {
    mocked.getAll.mockResolvedValue({
      data: [
        {
          applicationName: 'demo-app',
          version: '1.0.0',
          descriptor: '{}',
          parsedDescriptor: {},
          doi: null,
          visible: true,
          resources: [],
          tags: ['stable'],
          settings: [],
          source: null,
          note: null,
        },
      ],
      total: 1,
    })

    const store = useAppVersionsStore()
    const pending = store.fetchAppVersions()

    expect(store.isLoading).toBe(true)
    const versions = await pending

    expect(mocked.getAll).toHaveBeenCalledWith(0, 50)
    expect(store.isLoading).toBe(false)
    expect(store.totalCount).toBe(1)
    expect(versions).toHaveLength(1)
    expect(store.appVersions[0]?.version).toBe('1.0.0')
  })

  it('fetches versions for a specific application', async () => {
    mocked.getAllForApplication.mockResolvedValue({
      data: [
        {
          applicationName: 'demo-app',
          version: '2.0.0',
          descriptor: null,
          parsedDescriptor: null,
          doi: null,
          visible: true,
          resources: [],
          tags: [],
          settings: [],
          source: null,
          note: null,
        },
      ],
      total: 1,
    })

    const store = useAppVersionsStore()
    const versions = await store.fetchAppVersionsForApplication('demo-app')

    expect(mocked.getAllForApplication).toHaveBeenCalledWith('demo-app', 0, 50)
    expect(store.totalCount).toBe(1)
    expect(versions[0]?.version).toBe('2.0.0')
  })

  it('fetches a single version by app/version', async () => {
    mocked.getByVersion.mockResolvedValue({
      applicationName: 'demo-app',
      version: '3.0.0',
      descriptor: null,
      parsedDescriptor: null,
      doi: null,
      visible: true,
      resources: [],
      tags: ['latest'],
      settings: [],
      source: null,
      note: null,
    })

    const store = useAppVersionsStore()
    const version = await store.fetchAppVersion('demo-app', '3.0.0')

    expect(mocked.getByVersion).toHaveBeenCalledWith('demo-app', '3.0.0')
    expect(version.version).toBe('3.0.0')
    expect(version.tags).toEqual(['latest'])
  })
})
