export interface AppVersion {
    applicationName: string
    version: string
    descriptor: string | null
    doi: string | null
    visible: boolean
    resources: { name: string; status: boolean; configuration: string }[]
    tags: string[]
    settings: {}[]
    source: string | null
    note: string | null
}

export interface ApplicationInput {
  name: string
  type: 'File' | 'String' | 'Number' | 'Boolean' | 'List'
  description: string
  required: boolean
  defaultValue?: string
  possibleValues?: string[]
}

export interface PrecisePage<T> {
  data: T[]
  total: number
}