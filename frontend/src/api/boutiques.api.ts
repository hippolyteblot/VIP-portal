<<<<<<< HEAD
import { backendClient } from './client'

=======
>>>>>>> dd9e68302137a90ccbad13273653de8f65dd9aef
export interface BoutiquesCheckResult {
  valid: boolean
  errors: string[]
}

/**
 * Mocked API call used before backend endpoint is implemented.
 * It validates minimal Boutiques structure constraints.
 */
export const boutiquesApi = {
  checkDescriptor: async (file: File): Promise<BoutiquesCheckResult> => {
    const text = await file.text()

    try {
      const parsed = JSON.parse(text) as Record<string, unknown>
      const name = typeof parsed.name === 'string' ? parsed.name.trim() : ''
      const version =
        typeof parsed['tool-version'] === 'string'
          ? parsed['tool-version'].trim()
          : typeof parsed.version === 'string'
            ? parsed.version.trim()
            : ''

      const errors: string[] = []

      if (!name) errors.push("'name' is missing or empty in the Boutiques descriptor.")
      if (!version)
        errors.push("'tool-version' or 'version' is missing or empty in the Boutiques descriptor.")

      await backendClient.post(`/internal/boutiques/check`, {
        descriptor: text,
      }).catch((error) =>
        errors.push(
          error.response.data.errorMessage || 'An error occurred while validating the descriptor on the server.'
        )
      )
      return {
        valid: errors.length === 0,
        errors,
      }
    } catch {
      return {
        valid: false,
        errors: ['The JSON file is invalid.'],
      }
    }
  },
}
