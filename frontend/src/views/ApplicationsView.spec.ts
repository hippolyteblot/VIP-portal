import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ApplicationsView from './ApplicationsView.vue'

const mocked = vi.hoisted(() => ({
  applicationsStore: {
    filteredApplications: [] as Array<{
      name: string
      fullName: string | null
      citation: string | null
      owner: string | null
      groups: Array<{ name: string; publicGroup: boolean; type: string; auto: boolean }>
      note: string | null
    }>,
    isLoading: false,
    searchQuery: '',
    fetchApplications: vi.fn(async () => undefined),
  },
  appVersionsStore: {
    appVersions: [] as Array<{
      applicationName: string
      version: string
      descriptor: string | null
      parsedDescriptor: Record<string, unknown> | null
      doi: string | null
      visible: boolean
      resources: Array<{ name: string; status: boolean; configuration: string }>
      tags: string[]
      settings: Array<Record<string, unknown>>
      source: string | null
      note: string | null
    }>,
    isLoading: false,
    fetchAppVersions: vi.fn(async () => undefined),
  },
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a><slot /></a>',
  },
}))

vi.mock('@/stores/applications.store', () => ({
  useApplicationsStore: () => mocked.applicationsStore,
}))

vi.mock('@/stores/appversions.stores', () => ({
  useAppVersionsStore: () => mocked.appVersionsStore,
}))

describe('ApplicationsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocked.applicationsStore.isLoading = false
    mocked.appVersionsStore.isLoading = false
    mocked.applicationsStore.searchQuery = ''

    mocked.applicationsStore.filteredApplications = [
      {
        name: 'freesurfer',
        fullName: 'FreeSurfer',
        citation: null,
        owner: null,
        groups: [{ name: 'Neuro', publicGroup: true, type: 'TEAM', auto: false }],
        note: 'Cortical pipeline',
      },
      {
        name: 'without-version',
        fullName: 'No Version',
        citation: null,
        owner: null,
        groups: [],
        note: null,
      },
    ]

    mocked.appVersionsStore.appVersions = [
      {
        applicationName: 'freesurfer',
        version: '1.0.0',
        descriptor: '{}',
        parsedDescriptor: { description: 'Older description' },
        doi: null,
        visible: true,
        resources: [],
        tags: [],
        settings: [],
        source: null,
        note: null,
      },
      {
        applicationName: 'freesurfer',
        version: '2.0.0',
        descriptor: '{}',
        parsedDescriptor: { description: 'Latest description' },
        doi: null,
        visible: true,
        resources: [],
        tags: [],
        settings: [],
        source: null,
        note: null,
      },
    ]
  })

  it('loads applications and versions on mount', async () => {
    mount(ApplicationsView)
    await flushPromises()

    expect(mocked.applicationsStore.fetchApplications).toHaveBeenCalledTimes(1)
    expect(mocked.appVersionsStore.fetchAppVersions).toHaveBeenCalledTimes(1)
  })

  it('renders only applications that have versions', async () => {
    const wrapper = mount(ApplicationsView)
    await flushPromises()

    expect(wrapper.text()).toContain('freesurfer')
    expect(wrapper.text()).not.toContain('without-version')
    expect(wrapper.text()).toContain('2.0.0')
    expect(wrapper.text()).toContain('1.0.0')
    expect(wrapper.text()).toContain('Latest description')
  })

  it('shows empty state when no matching applications with versions', async () => {
    mocked.applicationsStore.filteredApplications = []

    const wrapper = mount(ApplicationsView)
    await flushPromises()

    expect(wrapper.text()).toContain('No applications found. Try adjusting your search')
  })
})
