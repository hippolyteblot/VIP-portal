import axios from 'axios'

const BASE_URL = ''

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|;\\s*)${name}=([^;]*)`))
  return match?.[1] ? decodeURIComponent(match[1]) : null
}

/**
 * Spring Security 6 utilise XorCsrfTokenRequestAttributeHandler (protection BREACH).
 * Le token brut du cookie doit être XOR-encodé avant d'être envoyé dans le header.
 */
function encodeXsrfToken(rawToken: string): string {
  const encoder = new TextEncoder()
  const tokenBytes = encoder.encode(rawToken)

  const randomBytes = new Uint8Array(tokenBytes.length)
  crypto.getRandomValues(randomBytes)

  const xored = new Uint8Array(tokenBytes.length)
  for (let i = 0; i < tokenBytes.length; i++) {
    xored[i] = randomBytes[i]! ^ tokenBytes[i]!
  }

  const combined = new Uint8Array(randomBytes.length + xored.length)
  combined.set(randomBytes)
  combined.set(xored, randomBytes.length)
  
  return btoa(String.fromCharCode(...combined))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}


export const backendClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  withCredentials: true,
  withXSRFToken: false,
  headers: {
    'Content-Type': 'application/json',
  },
})

backendClient.interceptors.request.use((config) => {
  const method = (config.method || 'get').toUpperCase()
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
    const raw = readCookie('XSRF-TOKEN')
    if (raw) {
      config.headers['X-XSRF-TOKEN'] = encodeXsrfToken(raw)
    }
  }
  return config
})