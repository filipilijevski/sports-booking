/**
 * Single place that talks to the Spring JSON API
 * ----------------------------------------------
 * - Origin  : VITE_API_URL   (default '')
 * - Base    : always “.../api” (auto-added if you omit it)
 * - Adds    : Bearer token (if present & legacy mode)
 * - Handles : refresh-token rotation (401 -> /auth/refresh)
 * - Cookies : credentials: 'include' for all fetches (dual mode)
 * - CSRF    : optional X-CSRF header from XSRF-TOKEN cookie
 */

import {
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
  clearAuth,
  broadcastJwtUpdate,
} from './auth';

const USE_COOKIE_AUTH =
  String(import.meta.env.VITE_USE_COOKIE_AUTH ?? '').toLowerCase() === 'true';

const ENABLE_PROACTIVE_REFRESH =
  String(import.meta.env.VITE_PROACTIVE_REFRESH ?? '').toLowerCase() === 'true';

const ACCESS_TTL_SEC = Number(import.meta.env.VITE_ACCESS_TTL_SEC ?? '900');

const backendOrigin =
  (import.meta.env.VITE_API_URL as string | undefined) ?? '';

const ORIGIN = backendOrigin.replace(/\/$/, '');

const API_BASE = ORIGIN
  ? ORIGIN.endsWith('/api') ? ORIGIN : `${ORIGIN}/api`
  : '/api';

function buildUrl(path: string): string {
  if (/^https?:\/\//i.test(path)) return path;
  let p = path.startsWith('/') ? path : `/${path}`;
  if (API_BASE.endsWith('/api') && p.startsWith('/api')) p = p.slice(4);
  return `${API_BASE}${p}`;
}

function readCookie(name: string): string | null {
  const m = document.cookie
    ?.split(';')
    .map(s => s.trim())
    .find(s => s.toLowerCase().startsWith(`${name.toLowerCase()}=`));
  if (!m) return null;
  const v = m.substring(m.indexOf('=') + 1);
  try { return decodeURIComponent(v); } catch { return v; }
}

function attachCsrfHeader(headers: Record<string, string>) {
  const token = readCookie('XSRF-TOKEN');
  if (token) headers['X-CSRF'] = token;
}

async function refreshAccessTokenLegacy(): Promise<boolean> {
  try {
    const rt = getRefreshToken();
    if (!rt) return false;

    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    attachCsrfHeader(headers);

    const res = await fetch(buildUrl('/auth/refresh'), {
      method: 'POST',
      headers,
      credentials: 'include',
      body: JSON.stringify({ refreshToken: rt }),
    });
    if (!res.ok) return false;

    const { accessToken, refreshToken } = (await res.json()) as { accessToken: string, refreshToken?: string };
    if (!accessToken) return false;

    setAccessToken(accessToken);
    if (refreshToken) setRefreshToken(refreshToken); // <- critical fix
    broadcastJwtUpdate();
    return true;
  } catch {
    clearAuth();
    return false;
  }
}

async function refreshAccessTokenCookieMode(): Promise<boolean> {
  try {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    attachCsrfHeader(headers);

    const res = await fetch(buildUrl('/auth/refresh'), {
      method: 'POST',
      headers,
      credentials: 'include',
    });
    if (!res.ok) return false;

    try { await res.json(); } catch { /* ignore */ }
    broadcastJwtUpdate();
    return true;
  } catch {
    return false;
  }
}

async function refreshSession(): Promise<boolean> {
  return USE_COOKIE_AUTH
    ? refreshAccessTokenCookieMode()
    : refreshAccessTokenLegacy();
}

let refreshTimer: number | undefined;
function scheduleProactiveRefresh() {
  if (!ENABLE_PROACTIVE_REFRESH) return;
  if (refreshTimer) { clearTimeout(refreshTimer); refreshTimer = undefined; }
  const ttl = Number.isFinite(ACCESS_TTL_SEC) ? ACCESS_TTL_SEC : 900;
  const delayMs = Math.max(60, ttl - 120) * 1000;
  refreshTimer = window.setTimeout(async () => {
    await refreshSession();
    scheduleProactiveRefresh();
  }, delayMs);
}
window.addEventListener('jwt-updated', scheduleProactiveRefresh);
window.addEventListener('user-logout', () => { if (refreshTimer) clearTimeout(refreshTimer); refreshTimer = undefined; });

export async function api<T = unknown>(
  path: string,
  options: RequestInit = {},
): Promise<T | undefined> {

  const method = (options.method ?? 'GET').toString().toUpperCase();
  const hasBody = options.body != null && !(options.body instanceof FormData);

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
    ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
  };

  if (!USE_COOKIE_AUTH) {
    const jwt = getAccessToken();
    if (jwt) headers.Authorization = `Bearer ${jwt}`;
  }

  attachCsrfHeader(headers);

  const res = await fetch(buildUrl(path), {
    ...options,
    method,
    headers,
    credentials: 'include',
  });

  if (res.status === 401) {
    const ok = await refreshSession();
    if (ok) {
      const retryHeaders: Record<string, string> = {
        ...(options.headers as Record<string, string>),
        ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
      };
      if (!USE_COOKIE_AUTH) {
        const fresh = getAccessToken();
        if (fresh) retryHeaders.Authorization = `Bearer ${fresh}`;
      }
      attachCsrfHeader(retryHeaders);

      const retry = await fetch(buildUrl(path), {
        ...options,
        method,
        headers: retryHeaders,
        credentials: 'include',
      });
      if (!retry.ok) await throwHttpError(retry);
      return await parseBody<T>(retry);
    }
  }

  if (!res.ok) await throwHttpError(res);
  return await parseBody<T>(res);
}

export async function apiPublic<T = unknown>(
  path: string,
  options: RequestInit = {},
): Promise<T | undefined> {

  const hasBody = options.body != null && !(options.body instanceof FormData);
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
    ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
  };

  attachCsrfHeader(headers);

  const res = await fetch(buildUrl(path), {
    ...options,
    headers,
    credentials: 'include',
  });

  if (!res.ok) await throwHttpError(res);
  return await parseBody<T>(res);
}

async function throwHttpError(res: Response): Promise<never> {
  const ct = res.headers.get('Content-Type') || '';
  const text = await res.text();

  const tryParse = (): any | null => { try { return JSON.parse(text); } catch { return null; } };
  const parsed = ct.toLowerCase().includes('application/json') ? tryParse() : tryParse();

  if (parsed && typeof parsed === 'object') {
    const j = parsed as { message?: string; error?: string; code?: string; fieldErrors?: Record<string, string> };
    const err: any = new Error(j.message || j.error || 'Request failed');
    if (j.code)        err.code = j.code;
    if (j.fieldErrors) err.fieldErrors = j.fieldErrors;
    err.status = res.status;
    throw err;
  }

  const fallback = text && text.trim().startsWith('{') ? 'Request failed' : (text || 'Request failed');
  const err: any = new Error(fallback);
  err.status = res.status;
  throw err;
}

async function parseBody<T>(res: Response): Promise<T | undefined> {
  if (res.status === 204) return undefined;
  const txt = await res.text();
  if (!txt) return undefined;
  try { return JSON.parse(txt) as T; }
  catch { return txt as unknown as T; }
}
