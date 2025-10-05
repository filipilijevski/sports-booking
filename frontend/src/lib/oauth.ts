// Single source of truth for Google URL
/**
 * Build the Google OAuth2 authorization URL that points directly at the
 * backend origin. Every component should import and use this constant so
 * we never end up with half‑broken copies that trigger CORS pre‑flights.
 *
 * In dev VITE_API_URL is http://localhost:8080 
 * In prod it may be an empty string (same origin) or the public backend
 *   host.
 */
export const BACKEND_ORIGIN =
  (import.meta.env.VITE_API_URL as string | undefined) ?? '';

export const GOOGLE_OAUTH_URL =
  `${BACKEND_ORIGIN.replace(/\/$/, '')}/oauth2/authorization/google`;