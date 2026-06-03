export type WorkflowStatus =
  | 'Running'
  | 'Completed'
  | 'Failed'
  | 'Killed'
  | 'Cleaned'
  | 'Waiting'
  | 'Error'

export interface Workflow {
  id: string
  applicationName: string
  applicationVersion: string
  workflowName: string
  status: WorkflowStatus
  inputs: Record<string, unknown>
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
  inputs: Record<string, unknown>
}

export interface WorkflowUpdatePayload {
  status: WorkflowStatus
}
