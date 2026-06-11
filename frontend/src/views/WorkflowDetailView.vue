<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Play, Trash2, XCircle } from 'lucide-vue-next'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useWorkflowsStore } from '@/stores/workflows.store'
import { useNotificationsStore } from '@/stores/notifications.store'
import { useFormatters } from '@/composables/useFormatters'
import type { WorkflowStatus } from '@/types/workflow.types'

const route = useRoute()
const router = useRouter()
const workflowsStore = useWorkflowsStore()
const notifications = useNotificationsStore()
const { formatRelativeTime } = useFormatters()

const wid = computed(() => route.params.id as string)
const wf = computed(() => workflowsStore.currentWorkflow)

const statusColors = {
  Running: 'primary',
  Completed: 'success',
  Failed: 'danger',
  Killed: 'warning',
  Cleaned: 'gray',
  Queued: 'info',
  Unknown: 'gray',
} as const

const canKill = computed(() =>
  wf.value != null && ['Running', 'Queued', 'Unknown'].includes(wf.value.status),
)

const canClean = computed(() =>
  wf.value != null && ['Completed', 'Failed', 'Killed'].includes(wf.value.status),
)

async function handleKill() {
  if (!wf.value) return
  const ok = await workflowsStore.killWorkflow(wf.value.id)
  if (ok) notifications.success('Workflow has been killed.')
}

async function handleClean() {
  if (!wf.value) return
  const ok = await workflowsStore.cleanWorkflow(wf.value.id, true)
  if (ok) notifications.success('Workflow has been cleaned.')
}

function goToRelaunch() {
  if (!wf.value) return
  router.push({
    name: 'application-launch',
    params: {
      name: wf.value.applicationName,
      version: wf.value.applicationVersion,
    },
  })
}

onMounted(() => {
  workflowsStore.fetchWorkflow(wid.value)
})
</script>

<template>
  <div class="space-y-6">
    <button
      class="inline-flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-primary-600"
      @click="router.push('/executions')"
    >
      <ArrowLeft class="h-4 w-4" />
      Back to executions
    </button>

    <div v-if="workflowsStore.isLoading" class="flex justify-center py-16 text-sm text-gray-500">
      Loading execution...
    </div>

    <div v-else-if="!wf" class="py-16 text-center text-sm text-gray-500">
      Execution not found.
    </div>

    <template v-else>
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <h1 class="text-2xl font-bold text-gray-900">
            {{ wf.workflowName }}
          </h1>
          <AppBadge :variant="statusColors[wf.status] || 'gray'">
            {{ wf.status }}
          </AppBadge>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <AppButton
            v-if="canKill"
            variant="danger"
            size="sm"
            @click="handleKill"
          >
            <XCircle class="mr-1 h-4 w-4" />
            Kill
          </AppButton>
          <AppButton
            v-if="canClean"
            variant="secondary"
            size="sm"
            @click="handleClean"
          >
            <Trash2 class="mr-1 h-4 w-4" />
            Clean
          </AppButton>
          <AppButton
            variant="primary"
            size="sm"
            @click="goToRelaunch"
          >
            <Play class="mr-1 h-4 w-4" />
            Relaunch
          </AppButton>
        </div>
      </div>

      <div class="grid gap-6 md:grid-cols-2">
        <AppCard padding>
          <h2 class="mb-3 text-sm font-semibold text-gray-700">Details</h2>
          <dl class="space-y-2 text-sm">
            <div class="flex justify-between">
              <dt class="text-gray-500">ID</dt>
              <dd class="font-mono text-s text-gray-900">{{ wf.id }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">Application</dt>
              <dd class="text-gray-900">
                {{ wf.applicationName }}
                <span class="text-gray-400">v{{ wf.applicationVersion }}</span>
              </dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">User</dt>
              <dd class="text-gray-900">{{ wf.userId }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">Started</dt>
              <dd class="text-gray-900">{{ formatRelativeTime(wf.startDate) }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">Engine</dt>
              <dd class="text-gray-900">{{ wf.engineName || '—' }}</dd>
            </div>
            <div class="flex justify-between">
              <dt class="text-gray-500">Tags</dt>
              <dd class="text-gray-900">{{ wf.tags || '—' }}</dd>
            </div>
          </dl>
        </AppCard>

        <AppCard padding>
          <h2 class="mb-3 text-sm font-semibold text-gray-700">Outputs</h2>
          <div v-if="!wf.outputs || Object.keys(wf.outputs).length === 0" class="text-sm text-gray-500">
            No outputs yet.
          </div>
          <dl v-else class="space-y-2 text-sm">
            <div v-for="(val, key) in wf.outputs" :key="key" class="flex justify-between">
              <dt class="text-gray-500">{{ key }}</dt>
              <dd class="max-w-[60%] truncate text-gray-900" :title="val">{{ val }}</dd>
            </div>
          </dl>
        </AppCard>
      </div>

      <AppCard padding>
        <h2 class="mb-3 text-sm font-semibold text-gray-700">Inputs</h2>
        <div v-if="!wf.inputs || Object.keys(wf.inputs).length === 0" class="text-sm text-gray-500">
          No inputs.
        </div>
        <div v-else class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200 text-sm">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-4 py-2 text-left font-semibold text-gray-700">Parameter</th>
                <th class="px-4 py-2 text-left font-semibold text-gray-700">Value</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="(input, name) in wf.inputs" :key="name">
                <td class="px-4 py-2 font-medium text-gray-900">{{ name }}</td>
                <td class="px-4 py-2 font-mono text-xs text-gray-600">
                  <template v-if="input.interval">
                    {{ input.interval.join(' – ') }}
                  </template>
                  <template v-else-if="input.values?.length">
                    {{ input.values.join(', ') }}
                  </template>
                  <span v-else class="text-gray-400">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </AppCard>
    </template>
  </div>
</template>
