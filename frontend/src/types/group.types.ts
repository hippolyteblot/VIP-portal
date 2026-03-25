export interface Group {
  name: string
  publicGroup: boolean
  type: GroupType
  auto: boolean
}

export type GroupType = 'APPLICATION' | 'RESOURCE'