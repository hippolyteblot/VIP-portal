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
  inputs: Record<string, {
    values?: string[]
    interval?: number[]
  }>
  outputs: Record<string, string>
  userId: string
  userFullName?: string
  startDate: string
  endDate?: string
}

export interface WorkflowLaunchPayload {
  applicationName: string
  applicationVersion: string
  workflowName: string
  inputs: Record<string, { type: string; values: string[] }>
}

export interface Task {
  id: string
  invocationID: number
  creationDate: string
  status: string
  exitCode: number
  siteName: string
  nodeName: string
  command: string
  fileName: string
  jobID: number
}

export type JobLogType = 'app-stdout' | 'app-stderr' | 'stdout' | 'stderr' | 'script'

export const JOB_LOG_TYPES: { value: JobLogType; label: string }[] = [
  { value: 'app-stdout', label: 'Application Output' },
  { value: 'app-stderr', label: 'Application Error' },
  { value: 'stdout', label: 'Output' },
  { value: 'stderr', label: 'Error' },
  { value: 'script', label: 'Script' },
]

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
