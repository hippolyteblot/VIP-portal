<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { Search, Filter } from 'lucide-vue-next'
import AppBadge from '@/components/ui/AppBadge.vue'
import { useWorkflowsStore } from '@/stores/workflows.store'
import type { WorkflowStatus, WorkflowListParams } from '@/types/workflow.types'
import { useFormatters } from '@/composables/useFormatters'

const workflowsStore = useWorkflowsStore()
const { formatRelativeTime } = useFormatters()

const statusFilter = ref('')
const appFilter = ref('')
const startDateFilter = ref('')
const endDateFilter = ref('')
const showFilters = ref(false)

const statusColors = {
  Running: 'primary',
  Completed: 'success',
  Failed: 'danger',
  Killed: 'warning',
  Cleaned: 'gray',
  Queued: 'info',
  Unknown: 'gray',
} as const

const statusList: WorkflowStatus[] = [
  'Running', 'Queued', 'Completed', 'Failed', 'Killed', 'Cleaned', 'Unknown',
]

const page = ref(0)
const pageSize = 20

function buildParams(): WorkflowListParams {
  const params: WorkflowListParams = {
    offset: page.value * pageSize,
    quantity: pageSize,
  }
  if (statusFilter.value) params.status = statusFilter.value
  if (appFilter.value) params.application = appFilter.value
  if (startDateFilter.value) params.startDate = startDateFilter.value
  if (endDateFilter.value) params.endDate = endDateFilter.value
  return params
}

async function loadWorkflows() {
  await workflowsStore.listWorkflows(buildParams())
}

function onPage(delta: number) {
  page.value = Math.max(0, page.value + delta)
  loadWorkflows()
}

onMounted(loadWorkflows)
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Executions</h1>
        <p class="mt-1 text-sm text-gray-500">
          Browse and manage your workflow executions.
        </p>
      </div>
      <button
        class="inline-flex items-center gap-2 rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        @click="showFilters = !showFilters"
      >
        <Filter class="h-4 w-4" />
        Filters
      </button>
    </div>

    <div
      v-if="showFilters"
      class="flex flex-wrap items-end gap-3 rounded-lg border border-gray-200 bg-gray-50 p-4"
    >
      <div class="min-w-0 flex-1">
        <label class="block text-xs font-medium text-gray-600">Application</label>
        <input
          v-model="appFilter"
          type="text"
          placeholder="Filter by application..."
          class="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder:text-gray-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          @input="loadWorkflows"
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-600">Status</label>
        <select
          v-model="statusFilter"
          class="mt-1 block rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          @change="loadWorkflows"
        >
          <option value="">All</option>
          <option v-for="s in statusList" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-600">From</label>
        <input
          v-model="startDateFilter"
          type="date"
          class="mt-1 block rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          @change="loadWorkflows"
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-600">To</label>
        <input
          v-model="endDateFilter"
          type="date"
          class="mt-1 block rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          @change="loadWorkflows"
        />
      </div>
    </div>

    <div v-if="workflowsStore.isLoading" class="flex justify-center py-16 text-sm text-gray-500">
      Loading executions...
    </div>

    <div v-else-if="workflowsStore.workflows.length === 0" class="py-16 text-center text-sm text-gray-500">
      No executions found.
    </div>

    <div v-else class="overflow-x-auto rounded-lg border border-gray-200">
      <table class="min-w-full divide-y divide-gray-200 text-sm">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-4 py-3 text-left font-semibold text-gray-700">ID</th>
            <th class="px-4 py-3 text-left font-semibold text-gray-700">Name</th>
            <th class="px-4 py-3 text-left font-semibold text-gray-700">Application</th>
            <th class="px-4 py-3 text-left font-semibold text-gray-700">Status</th>
            <th class="px-4 py-3 text-left font-semibold text-gray-700">User</th>
            <th class="px-4 py-3 text-left font-semibold text-gray-700">Started</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr
            v-for="wf in workflowsStore.workflows"
            :key="wf.id"
            class="transition hover:bg-gray-50"
          >
            <td class="px-4 py-3">
              <RouterLink
                :to="{ name: 'workflow-detail', params: { id: wf.id } }"
                class="font-mono text-xs text-primary-600 hover:text-primary-700"
              >
                {{ wf.id }}
              </RouterLink>
            </td>
            <td class="px-4 py-3 font-medium text-gray-900">
              {{ wf.workflowName }}
            </td>
            <td class="px-4 py-3 text-gray-600">
              {{ wf.applicationName }}
              <span class="text-gray-400">v{{ wf.applicationVersion }}</span>
            </td>
            <td class="px-4 py-3">
              <AppBadge :variant="statusColors[wf.status] || 'gray'">
                {{ wf.status }}
              </AppBadge>
            </td>
            <td class="px-4 py-3 text-gray-600">
              {{ wf.userId }}
            </td>
            <td class="px-4 py-3 text-gray-500">
              {{ formatRelativeTime(wf.startDate) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="flex items-center justify-between gap-4 text-sm text-gray-600">
      <span>Total: {{ workflowsStore.total }} execution(s)</span>
      <div class="flex items-center gap-2">
        <button
          :disabled="page === 0"
          class="rounded-lg border border-gray-300 px-3 py-1.5 disabled:opacity-40"
          @click="onPage(-1)"
        >
          Previous
        </button>
        <span class="font-medium">Page {{ page + 1 }}</span>
        <button
          :disabled="(page + 1) * pageSize >= workflowsStore.total"
          class="rounded-lg border border-gray-300 px-3 py-1.5 disabled:opacity-40"
          @click="onPage(1)"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>
