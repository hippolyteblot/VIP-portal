export type WorkflowStatus =
  | 'Running'
  | 'Completed'
  | 'Failed'
  | 'Killed'
  | 'Cleaned'
  | 'Queued'
  | 'Unknown'

export interface Workflow {
  id: string
  applicationName: string
  applicationVersion: string
  workflowName: string
  status: WorkflowStatus
  inputs: Record<string, { type: string; value: string }>
  outputs: Record<string, string>
  userId: string
  startDate: string
  engineName?: string
  tags?: string
}

export interface WorkflowLaunchPayload {
  applicationName: string
  applicationVersion: string
  workflowName: string
  inputs: Record<string, { value: string }>
}

export interface WorkflowListParams {
  offset?: number
  quantity?: number
  search?: string
  application?: string
  status?: string
  startDate?: string
  endDate?: string
  tag?: string
}
