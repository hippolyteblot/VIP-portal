
export interface Tag {
  name: string

}

export interface PrecisePage<T> {
  data: T[]
  total: number
}

export interface ApplicationImportPayload {
  jsonFile?: File
  executionResource?: string
  existingTag?: string
  customTag?: string
}
