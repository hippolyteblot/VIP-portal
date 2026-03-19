<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { Search } from 'lucide-vue-next'
import AppCard from '@/components/ui/AppCard.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import { useApplicationsStore } from '@/stores/applications.store'

const applicationsStore = useApplicationsStore()

onMounted(() => {
  applicationsStore.fetchApplications()
})
</script>

<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">
        Applications
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Explore and launch applications available in the VIP Portal. Use the search to quickly find the application you need, or create a new one if you have the necessary permissions.
      </p>
      <router-link
        to="/applications/create"
        class="mt-3 inline-flex items-center gap-1 text-sm font-medium text-primary-600 hover:text-primary-700"
      >
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="h-4 w-4">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5
M4.5 12h15" />
        </svg>
        Create a new application
      </router-link>
    </div>

    <div class="relative">
      <Search
        class="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400"
      />
      <input
        v-model="applicationsStore.searchQuery"
        type="search"
        placeholder="Search for an application..."
        class="block w-full rounded-lg border border-gray-300 py-2.5 pl-10 pr-4 text-sm placeholder:text-gray-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-0"
      />
    </div>

    <div v-if="applicationsStore.isLoading" class="flex justify-center py-16">
      <
    </div>

    <div
      v-else
      class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3"
    >
      <div
        v-for="app in applicationsStore.filteredApplications"
        :key="app.name"
      >
        <AppCard hoverable padding class="h-full">
          <RouterLink :to="{ name: 'application-detail', params: { name: app.name } }" class="block">
            <div class="flex items-start justify-between gap-2">
              <h3 class="text-lg font-bold text-gray-900">
                {{ app.name }}
              </h3>
            </div>
            <p v-if="app.note" class="mt-2 line-clamp-2 text-sm text-gray-600">
              {{ app.note }}
            </p>
            <div v-if="app.groups.length" class="mt-3 flex flex-wrap gap-1.5">
              <AppBadge
                v-for="group in app.groups"
                :key="group.name"
                variant="gray"
              >
                {{ group.name }}
              </AppBadge>
            </div>
            <p v-if="app.note" class="mt-3 text-xs text-gray-500">
              {{ app.note }}
            </p>
            <p class="mt-2 text-sm font-medium text-primary-600 hover:text-primary-700">
              View details &rarr;
            </p>
          </RouterLink>
        </AppCard>
      </div>
    </div>

    <p
      v-if="!applicationsStore.isLoading && applicationsStore.filteredApplications.length === 0"
      class="py-12 text-center text-gray-500"
    >
      No applications found. Try adjusting your search or
      <RouterLink to="/applications/create" class="font-medium text-primary-600 hover:text-primary-700">
        create a new one.
      </RouterLink>
    </p>
  </div>
</template>
