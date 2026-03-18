export type ParsedInput = {
  id: string
  name: string
  type: 'File' | 'String' | 'Number' | 'Boolean' | 'List'
  description: string
  required: boolean
  defaultValue?: string
  possibleValues?: string[]
}

function mapBoutiquesType(rawType: unknown): ParsedInput['type'] {
  const normalized = typeof rawType === 'string' ? rawType.trim().toLowerCase() : ''
  if (normalized === 'file') return 'File'
  if (normalized === 'number' || normalized === 'integer' || normalized === 'float') return 'Number'
  if (normalized === 'flag' || normalized === 'boolean') return 'Boolean'
  if (normalized === 'list') return 'List'
  return 'String'
}

export function parseDescriptorInputs(descriptor: string | null): ParsedInput[] {
  if (!descriptor) return []

  try {
    const parsed = JSON.parse(descriptor) as { inputs?: unknown }
    if (!Array.isArray(parsed.inputs)) return []

    return parsed.inputs
      .filter((item): item is Record<string, unknown> => item !== null && typeof item === 'object')
      .map((input) => {
        const rawName = typeof input.name === 'string' ? input.name.trim() : ''
        const rawId = typeof input.id === 'string' ? input.id.trim() : ''
        const required = input.optional === true ? false : true
        const defaultValue = input['default-value']

        let defaultValueString: string | undefined
        if (defaultValue !== undefined && defaultValue !== null) {
          defaultValueString = typeof defaultValue === 'string' ? defaultValue : JSON.stringify(defaultValue)
        }

        const possibleValues = Array.isArray(input['value-choices'])
          ? input['value-choices']
              .filter((choice): choice is string | number | boolean => ['string', 'number', 'boolean'].includes(typeof choice))
              .map((choice) => String(choice))
          : undefined

        return {
          id: rawId || rawName || 'unnamed-input',
          name: rawName || rawId || 'Unnamed input',
          type: mapBoutiquesType(input.type),
          description:
            typeof input.description === 'string' && input.description.trim().length > 0
              ? input.description.trim()
              : '-',
          required,
          defaultValue: defaultValueString,
          possibleValues,
        }
      })
  } catch {
    return []
  }
}
