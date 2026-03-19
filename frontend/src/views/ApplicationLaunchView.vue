<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import { useApplicationsStore } from '@/stores/applications.store'
import { useAppVersionsStore } from '@/stores/appversions.stores'
import type { Application } from '@/types/application.types'
import type { AppVersion } from '@/types/appversion.types'
import { parseDescriptorInputs } from '@/utils/boutiquesDescriptor'

const route = useRoute()
const applicationsStore = useApplicationsStore()
const appversionsStore = useAppVersionsStore()

const application = ref<Application | null>(null)
const selectedVersion = ref<AppVersion | null>(null)

const isLoading = ref(false)
const loadError = ref('')

const formValues = ref<Record<string, string | number | boolean | File | null>>({})
const executionName = ref('')
const resultsDirectory = ref('')

const appName = computed(() => route.params.name as string)
const routeVersion = computed(() => (route.params.version as string | undefined) ?? '')
const inputParams = computed(() => parseDescriptorInputs(selectedVersion.value?.descriptor ?? null))

function initializeFormValues() {
  const initialValues: Record<string, string | number | boolean | File | null> = {}

  for (const input of inputParams.value) {
    if (input.type === 'Boolean') {
      initialValues[input.id] = input.defaultValue === 'true'
      continue
    }

    if (input.type === 'Number') {
      if (input.defaultValue === undefined || input.defaultValue === '') {
        initialValues[input.id] = ''
      } else {
        const num = Number(input.defaultValue)
        initialValues[input.id] = Number.isNaN(num) ? '' : num
      }
      continue
    }

    if (input.type === 'File') {
      initialValues[input.id] = null
      continue
    }

    initialValues[input.id] = input.defaultValue ?? ''
  }

  formValues.value = initialValues
}

async function loadVersion(versionName: string) {
  if (!versionName) {
    selectedVersion.value = null
    loadError.value = 'No version provided in the URL.'
    formValues.value = {}
    return
  }

  try {
    loadError.value = ''
    selectedVersion.value = await appversionsStore.fetchAppVersion(appName.value, versionName)
    initializeFormValues()
  } catch {
    selectedVersion.value = null
    formValues.value = {}
    loadError.value = 'Unable to load the requested version.'
  }
}

function onFileChange(inputId: string, event: Event) {
  const target = event.target as HTMLInputElement
  formValues.value[inputId] = target.files?.[0] ?? null
}

onMounted(async () => {
  isLoading.value = true
  loadError.value = ''

  try {
    application.value = await applicationsStore.getApplication(appName.value)
    await loadVersion(routeVersion.value)
  } catch {
    loadError.value = 'Unable to load the application.'
  } finally {
    isLoading.value = false
  }
})

</script>

<template>
  <div class="space-y-6">
    <RouterLink
      :to="{ name: 'application-detail', params: { name: appName } }"
      class="inline-flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-primary-600"
    >
      <ArrowLeft class="h-4 w-4" />
      Back to details
    </RouterLink>

    <div v-if="isLoading" class="py-10 text-sm text-gray-500">
      Loading...
    </div>

    <div v-else-if="loadError" class="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
      {{ loadError }}
    </div>

    <div v-else-if="application && selectedVersion" class="space-y-6">
      <div class="flex flex-wrap items-center gap-3">
        <h1 class="text-2xl font-bold text-gray-900">
          Launch {{ application.fullName || application.name }}
        </h1>

        <AppBadge variant="gray">
          {{ selectedVersion.version }}
        </AppBadge>
      </div>

      <AppCard v-if="selectedVersion" padding>
        <form class="space-y-4" @submit.prevent>
          <h2 class="text-sm font-semibold text-gray-700">
            Execution form
          </h2>

          <div class="grid gap-4 md:grid-cols-2">
            <AppInput
              v-model="executionName"
              label="Execution name"
              placeholder="e.g. freesurfer-run-001"
              :required="true"
            />
            <AppInput
              v-model="resultsDirectory"
              label="Results directory"
              placeholder="e.g. /vip/results/run-001"
              :required="true"
            />
          </div>

          <div
            v-for="input in inputParams"
            :key="input.id"
            class="space-y-2 rounded-lg pt-2"
          >
            <div class="flex items-center gap-2">
              <p class="text-sm font-medium text-gray-800">
                {{ input.name }}
                <span v-if="input.required" class="text-red-500">*</span>
              </p>
              <AppBadge v-if="!input.required" :variant="'gray'">
                Optional
              </AppBadge>
              <AppBadge variant="info">
                {{ input.type }}
              </AppBadge>
            </div>

            <p class="text-sm text-gray-500">
              {{ input.description }}
            </p>

            <div v-if="input.type === 'Boolean'" class="pt-1">
              <label class="inline-flex items-center gap-2 text-sm text-gray-700">
                <input
                  :checked="Boolean(formValues[input.id])"
                  type="checkbox"
                  class="h-4 w-4 rounded border-gray-300 text-primary-600"
                  @change="formValues[input.id] = ($event.target as HTMLInputElement).checked"
                />
                Active
              </label>
            </div>

            <div v-else-if="input.type === 'File'" class="pt-1">
              <input
                type="file"
                class="block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
                @change="onFileChange(input.id, $event)"
              />
            </div>

            <div v-else-if="input.type === 'List'" class="pt-1">
              <AppInput
                :model-value="String(formValues[input.id] ?? '')"
                :label="`Values (comma-separated)`"
                :required="input.required"
                placeholder="value1, value2"
                @update:model-value="formValues[input.id] = String($event)"
              />
            </div>

            <div v-else>
              <AppInput
                :model-value="formValues[input.id] as string | number"
                :type="input.type === 'Number' ? 'number' : 'text'"
                :required="input.required"
                :placeholder="input.defaultValue ? `Default: ${input.defaultValue}` : ''"
                @update:model-value="formValues[input.id] = $event"
              />
            </div>
          </div>

        <AppButton type="submit" variant="primary" size="lg">
            Launch app
        </AppButton>
        </form>
      </AppCard>
    </div>
  </div>
</template>
