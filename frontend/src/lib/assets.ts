/**
 * Resolves a product-image path returned by the backend.
 *
 * absolute URLs are passed through untouched;
 * “/uploads/...” paths are returned **relative** in prod so the browser uses the
 * same origin & protocol (avoids https to http mixed-content);
 * during local dev (vite to :5173, spring to :8080) we prepend
 * “http://localhost:8080”.
 */
export function resolveImgUrl(path?: string | null): string {
  if (!path) return '/placeholder.png';

  /* absolute already? */
  if (/^https?:\/\//i.test(path)) return path;

  /* dev-helper: use 8080 only when we’re on localhost */
  let origin = (import.meta.env.VITE_API_URL as string | undefined) ?? '';

  if (!origin) {
    const { protocol, hostname } = window.location;
    const isLocal = hostname === 'localhost' || hostname.startsWith('127.');
    origin = isLocal ? `${protocol}//${hostname}:8080` : '';
  }

  const clean = path.startsWith('/') ? path : `/${path}`;
  return origin ? `${origin}${clean}` : clean;   // '' -> relative (prod)
}
