import { ref } from 'vue'
import { defineStore } from 'pinia'
import { workflowsApi } from '@/api/workflows.api'
import { useNotificationsStore } from '@/stores/notifications.store'
import type { Workflow, WorkflowLaunchPayload } from '@/types/workflow.types'

export const useWorkflowsStore = defineStore('workflows', () => {
  const currentWorkflow = ref<Workflow | null>(null)
  const workflows = ref<Workflow[]>([])
  const isLoading = ref(false)
  const total = ref(0)
  const notifications = useNotificationsStore()

  async function fetchWorkflow(wid: string) {
    isLoading.value = true
    try {
      currentWorkflow.value = await workflowsApi.get(wid)
    } catch {
      notifications.error('Unable to load workflow.')
    } finally {
      isLoading.value = false
    }
  }

  async function listWorkflows(offset = 0, quantity = 10) {
    isLoading.value = true
    try {
      const page = await workflowsApi.list(offset, quantity)
      workflows.value = page.data
      total.value = page.total
    } catch {
      notifications.error('Unable to load workflows.')
    } finally {
      isLoading.value = false
    }
  }

  async function launchWorkflow(payload: WorkflowLaunchPayload): Promise<Workflow | null> {
    try {
      const workflow = await workflowsApi.launch(payload)
      notifications.success('Workflow launched.')
      return workflow
    } catch {
      notifications.error('Unable to launch workflow.')
      return null
    }
  }

  return {
    currentWorkflow,
    workflows,
    isLoading,
    total,
    fetchWorkflow,
    listWorkflows,
    launchWorkflow,
  }
})
