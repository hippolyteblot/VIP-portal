<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import semver from 'semver'
import AppCard from '@/components/ui/AppCard.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useAppVersionsStore } from '@/stores/appversions.stores'
import { useApplicationsStore } from '@/stores/applications.store'
import type { Application } from '@/types/application.types'
import type { AppVersion } from '@/types/appversion.types'
import { parseDescriptorInputs } from '@/utils/boutiquesDescriptor'

const route = useRoute()
const applicationsStore = useApplicationsStore()
const appversionsStore = useAppVersionsStore()

const application = ref<Application | null>(null)
const versions = ref<AppVersion[]>([])
const selectedVersionName = ref('')
const selectedVersion = ref<AppVersion | null>(null)
const isVersionLoading = ref(false)
const versionLoadError = ref('')

const appName = computed(() => route.params.name as string)

const inputParams = computed(() => parseDescriptorInputs(selectedVersion.value?.descriptor ?? null))

const launchRoute = computed(() => ({
  name: 'application-launch',
  params: {
    name: appName.value,
    version: selectedVersionName.value || undefined,
  },
}))

function normalizeVersion(version: string): string {
  if (semver.valid(version)) return version

  const parts = version.split('.')

  if (parts.length === 2 && /^\d+$/.test(parts[0]!) && /^\d+$/.test(parts[1]!)) {
    return `${parts[0]}.${parts[1]}.0`
  }

  if (parts.length === 1 && /^\d+$/.test(parts[0]!)) {
    return `${parts[0]}.0.0`
  }

  return version
}

function sortVersions(versions: AppVersion[]): AppVersion[] {
  return [...versions].sort((a, b) => {
    const va = normalizeVersion(a.version)
    const vb = normalizeVersion(b.version)

    const validA = semver.valid(va)
    const validB = semver.valid(vb)

    if (validA && validB) {
      return semver.compare(validA, validB)
    }

    return a.version.localeCompare(b.version)
  })
}

async function loadSelectedVersion(versionName: string) {
  if (!versionName) {
    selectedVersion.value = null
    versionLoadError.value = ''
    return
  }

  isVersionLoading.value = true
  versionLoadError.value = ''

  try {
    selectedVersion.value = await appversionsStore.fetchAppVersion(appName.value, versionName)
  } catch {
    const fallback = versions.value.find((v) => v.version === versionName) ?? null
    selectedVersion.value = fallback
    if (!fallback) {
      versionLoadError.value = 'Impossible de charger les details de cette version.'
    }
  } finally {
    isVersionLoading.value = false
  }
}

onMounted(async () => {
  application.value = await applicationsStore.getApplication(appName.value)
  versions.value = sortVersions(await appversionsStore.fetchAppVersions(appName.value)).reverse()

  const [firstVersion] = versions.value
  if (firstVersion) {
    selectedVersionName.value = firstVersion.version
    await loadSelectedVersion(selectedVersionName.value)
  }
})

watch(selectedVersionName, async (versionName) => {
  await loadSelectedVersion(versionName)
})
</script>

<template>
  <div class="space-y-6">
    <RouterLink
      :to="{ name: 'applications' }"
      class="inline-flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-primary-600"
    >
      <ArrowLeft class="h-4 w-4" />
      Retour aux applications
    </RouterLink>

    <div v-if="application">
      <div class="flex flex-wrap items-center gap-3">
        <h1 class="text-2xl font-bold text-gray-900">
          {{ application.fullName || application.name }}
        </h1>
        <div class="mt-3 flex flex-wrap gap-1.5">
          <AppBadge
            v-for="version in versions"
            :key="`badge-${version.version}`"
            :variant="version.version === selectedVersionName ? 'primary' : 'gray'"
          >
            {{ version.version }}
          </AppBadge>
        </div>
      </div>

      <div v-if="versions.length" class="mt-4 max-w-sm">
        <label for="version-select" class="block text-sm font-semibold text-gray-700">
          Selected version
        </label>
        <select
          id="version-select"
          v-model="selectedVersionName"
          class="mt-2 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-700 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-0"
        >
          <option
            v-for="version in versions"
            :key="version.version"
            :value="version.version"
          >
            {{ version.version }}
          </option>
        </select>

        
      </div>

      <div v-if="application.groups.length" class="mt-3 flex flex-wrap gap-1.5">
        <h2 class="text-sm font-semibold text-gray-700 w-full">
          Groups
        </h2>
        <AppBadge
          v-for="group in application.groups"
          :key="group.name"
          variant="gray"
        >
          {{ group.name }}
        </AppBadge>
      </div>

      <div v-if="application.note" class="mt-6">
        <h2 class="text-sm font-semibold text-gray-700">
          Description
        </h2>
        <p class="mt-2 text-gray-600">
          {{ application.note }}
        </p>
      </div>

      <div v-if="application.citation" class="mt-6">
        <h2 class="text-sm font-semibold text-gray-700">
          Citation
        </h2>
        <blockquote
          class="mt-2 border-l-4 border-primary-200 bg-primary-50/50 py-2 pl-4 pr-4 text-sm italic text-gray-700"
        >
          {{ application.citation }}
        </blockquote>
      </div>

      <div v-if="selectedVersion" class="mt-6 space-y-2">
        <h2 class="text-sm font-semibold text-gray-700">
          Version details
        </h2>
        <p class="text-sm text-gray-600">
          <span class="font-medium text-gray-700">Version:</span> {{ selectedVersion.version }}
        </p>
        <p v-if="selectedVersion.doi" class="text-sm text-gray-600">
          <span class="font-medium text-gray-700">DOI:</span> {{ selectedVersion.doi }}
        </p>
        <p v-if="selectedVersion.source" class="text-sm text-gray-600">
          <span class="font-medium text-gray-700">Source:</span> {{ selectedVersion.source }}
        </p>
        <p v-if="selectedVersion.parsedDescriptor?.description" class="text-sm text-gray-600">
          <span class="font-medium text-gray-700">Description:</span> {{ selectedVersion.parsedDescriptor?.description }}
        </p>
      </div>

      <p v-if="isVersionLoading" class="mt-4 text-sm text-gray-500">
        Version loading...
      </p>
      <p v-if="versionLoadError" class="mt-4 text-sm text-red-600">
        {{ versionLoadError }}
      </p>

      <div v-if="inputParams.length > 0" class="mt-8">
        <h2 class="text-sm font-semibold text-gray-700">
          Input parameters
        </h2>
        <AppCard :padding="false">
          <div class="overflow-x-auto rounded-xl">
            <table class="min-w-full divide-y divide-gray-200">
              <thead>
                <tr>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Name
                  </th>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Type
                  </th>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Required
                  </th>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Description
                  </th>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Default Value
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200 bg-white">
                <tr v-for="param in inputParams" :key="param.name">
                  <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                    {{ param.name }}
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                    {{ param.type }}
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-600">
                    {{ param.required ? 'Yes' : 'No' }}
                  </td>
                  <td class="px-4 py-3 text-sm text-gray-600">
                    {{ param.description }}
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-500">
                    {{ param.defaultValue ?? '—' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </AppCard>
      </div>

      <div class="mt-8">
        <RouterLink
          :to="launchRoute" class="block">
          <AppButton variant="primary" size="lg">
            Launch app
          </AppButton>
        </RouterLink>
      </div>
    </div>

    <div v-else class="flex justify-center py-16">
    </div>
  </div>
</template>
