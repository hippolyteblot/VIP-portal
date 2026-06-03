import { backendClient } from './client'
import type { PrecisePage } from '@/types/application.types'
import type { Workflow, WorkflowLaunchPayload, WorkflowUpdatePayload } from '@/types/workflow.types'

export const workflowsApi = {
  list: (offset = 0, quantity = 10) =>
    backendClient
      .get<PrecisePage<Workflow>>('/internal/workflows', { params: { offset, quantity } })
      .then((r) => r.data),

  get: (wid: string) =>
    backendClient
      .get<Workflow>(`/internal/workflows/${encodeURIComponent(wid)}`)
      .then((r) => r.data),

  launch: (payload: WorkflowLaunchPayload) =>
    backendClient
      .post<Workflow>('/internal/workflows', payload)
      .then((r) => r.data),

  updateStatus: (wid: string, payload: WorkflowUpdatePayload) =>
    backendClient
      .put<Workflow>(`/internal/workflows/${encodeURIComponent(wid)}`, payload)
      .then((r) => r.data),
}
