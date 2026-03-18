<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import AppCard from '@/components/ui/AppCard.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useAppVersionsStore } from '@/stores/appversions.stores'
import { useApplicationsStore } from '@/stores/applications.store'
import type { Application } from '@/types/application.types'
import type { AppVersion } from '@/types/appversion.types'

const route = useRoute()
const applicationsStore = useApplicationsStore()
const appversionsStore = useAppVersionsStore()

const application = ref<Application | null>(null)
const versions = ref<AppVersion[]>([])

const appName = computed(() => route.params.name as string)

onMounted(async () => {
  application.value = await applicationsStore.getApplication(appName.value)
  versions.value = await appversionsStore.fetchAppVersions(appName.value)
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
        <!-- <AppBadge v-if="application.version" variant="primary">
          {{ application.version }}
        </AppBadge> -->
        <!-- Same but foreach application version, loop and display badge for each version -->
        <AppBadge
          v-for="version in versions"
          :key="version.version"
          variant="primary"
        >
          {{ version.version }}
        </AppBadge>
      </div>
      <p v-if="application.owner" class="mt-1 text-sm text-gray-500">
        {{ application.owner }}
      </p>
      <div v-if="application.groups.length" class="mt-3 flex flex-wrap gap-1.5">
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

      <!--
      <div v-if="inputParams.length > 0" class="mt-8">
        <h2 class="text-sm font-semibold text-gray-700">
          Paramètres d'entrée
        </h2>
        <AppCard padding>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead>
                <tr>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Nom
                  </th>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Type
                  </th>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Obligatoire
                  </th>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Description
                  </th>
                  <th class="bg-gray-50 px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                    Valeur par défaut
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
                    {{ param.required ? 'Oui' : 'Non' }}
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
      </div>-->

      <div class="mt-8">
        <RouterLink
          :to="{ name: 'application-detail', params: {  } }" class="block">
          <AppButton variant="primary" size="lg">
            Lancer cette application
          </AppButton>
        </RouterLink>
      </div>
    </div>

    <div v-else class="flex justify-center py-16">
    </div>
  </div>
</template>
