export interface Group {
  name: string
  publicGroup: boolean
  type: string
  auto: boolean
  role?: string
}

export type GroupType = 'APPLICATION' | 'RESOURCE'