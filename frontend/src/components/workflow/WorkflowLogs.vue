<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import AppCard from '@/components/ui/AppCard.vue'
import AppButton from '@/components/ui/AppButton.vue'
import LogViewer from '@/components/workflow/LogViewer.vue'
import { workflowsApi } from '@/api/workflows.api'
import { JOB_LOG_TYPES, type JobLogType } from '@/types/workflow.types'
import type { Task } from '@/types/workflow.types'

interface Props {
  wid: string
  isRunning: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isRunning: false,
})

const selectedJobInvocationId = ref<string | null>(null)
const jobs = ref<Task[]>([])
const logContent = ref('')
const logLoading = ref(false)
const activeLogType = ref<JobLogType>('app-stdout')

let pollTimer: ReturnType<typeof setInterval> | null = null

const parsedInvocationId = computed(() => {
  if (selectedJobInvocationId.value === null) return null
  const n = Number(selectedJobInvocationId.value)
  return Number.isFinite(n) && n > 0 ? n : null
})

const isExecutionSelected = computed(() => parsedInvocationId.value === null)

const selectedJob = computed(() =>
  parsedInvocationId.value !== null
    ? jobs.value.find((j) => j.invocationID === parsedInvocationId.value) ?? null
    : null
)

const headerLabel = computed(() => {
  if (isExecutionSelected.value) return 'Execution Logs'
  return selectedJob.value
    ? `Job #${selectedJob.value.invocationID} — ${selectedJob.value.fileName}`
    : `Job #${parsedInvocationId.value}`
})

const isAutoRefreshActive = computed(() => props.isRunning)

async function fetchJobs() {
  try {
    jobs.value = await workflowsApi.listJobs(props.wid)
  } catch {
    jobs.value = []
  }
}

async function fetchLog() {
  logLoading.value = true
  try {
    const invocationId = parsedInvocationId.value
    if (invocationId === null) {
      logContent.value = activeLogType.value === 'app-stdout'
        ? await workflowsApi.readStdout(props.wid)
        : await workflowsApi.readStderr(props.wid)
    } else {
      logContent.value = await workflowsApi.readJobLog(props.wid, invocationId, activeLogType.value)
    }
  } catch {
    logContent.value = ''
  } finally {
    logLoading.value = false
  }
}

function switchLogType(type: JobLogType) {
  activeLogType.value = type
  fetchLog()
}

function switchToExecution() {
  selectedJobInvocationId.value = null
  activeLogType.value = 'app-stdout'
  fetchLog()
}

function selectJob(event: Event) {
  const val = (event.target as HTMLSelectElement).value
  selectedJobInvocationId.value = val === '' || val === 'null' ? null : val
  activeLogType.value = 'app-stdout'
  fetchLog()
}

function startPolling() {
  stopPolling()
  if (!props.isRunning) return
  pollTimer = setInterval(() => {
    fetchLog()
  }, 5000)
}

function stopPolling() {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(() => props.wid, () => {
  selectedJobInvocationId.value = null
  activeLogType.value = 'app-stdout'
  logContent.value = ''
  jobs.value = []
  fetchJobs()
  fetchLog()
}, { immediate: true })

watch(isAutoRefreshActive, (active) => {
  if (active) startPolling()
  else stopPolling()
})

onMounted(() => {
  if (isAutoRefreshActive.value) startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <AppCard padding>
    <div class="flex flex-col gap-4">
      <div class="flex flex-wrap items-center gap-2">
        <h2 class="text-sm font-semibold uppercase tracking-wider text-gray-500">Logs</h2>

        <div class="flex-1" />

        <select
          :value="selectedJobInvocationId ?? ''"
          @change="selectJob($event)"
          class="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 shadow-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
        >
          <option value="">Execution</option>
          <option
            v-for="job in jobs"
            :key="job.invocationID"
            :value="String(job.invocationID)"
          >
            Job #{{ job.invocationID }} — {{ job.fileName }}
          </option>
        </select>

        <span v-if="isAutoRefreshActive" class="flex items-center gap-1 text-xs text-blue-600">
          <RefreshCw class="h-3 w-3 animate-spin" />
          Auto
        </span>
      </div>

      <div class="text-xs font-medium text-gray-700">{{ headerLabel }}</div>

      <div v-if="isExecutionSelected" class="flex flex-wrap gap-1">
        <AppButton
          variant="ghost"
          size="sm"
          :class="activeLogType === 'app-stdout' ? 'bg-gray-100 text-gray-900' : ''"
          @click="switchLogType('app-stdout')"
        >
          Stdout
        </AppButton>
        <AppButton
          variant="ghost"
          size="sm"
          :class="activeLogType === 'app-stderr' ? 'bg-gray-100 text-gray-900' : ''"
          @click="switchLogType('app-stderr')"
        >
          Stderr
        </AppButton>
      </div>

      <div v-else class="flex flex-wrap gap-1">
        <AppButton
          v-for="logType in JOB_LOG_TYPES"
          :key="logType.value"
          variant="ghost"
          size="sm"
          :class="activeLogType === logType.value ? 'bg-gray-100 text-gray-900' : ''"
          @click="switchLogType(logType.value)"
        >
          {{ logType.label }}
        </AppButton>
      </div>

      <LogViewer
        :content="logContent"
        :loading="logLoading"
        :file-name="headerLabel"
      />
    </div>
  </AppCard>
</template>
