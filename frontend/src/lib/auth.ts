/*  authentication helpers & single-source of truth
 *  Dual-mode: legacy header (localStorage) OR cookie-auth (HttpOnly) */

import { api } from './api';

const USE_COOKIE_AUTH =
  String(import.meta.env.VITE_USE_COOKIE_AUTH ?? '').toLowerCase() === 'true';

/*  local-storage keys (legacy) */
const ACCESS_KEY  = 'accessToken';
const REFRESH_KEY = 'refreshToken';
const NAME_KEY    = 'firstName';

/* storage helpers */
export const getAccessToken  = () => localStorage.getItem(ACCESS_KEY);
export const getRefreshToken = () => localStorage.getItem(REFRESH_KEY);
export const getFirstName    = () => localStorage.getItem(NAME_KEY);

export const setAccessToken  = (tok: string) => localStorage.setItem(ACCESS_KEY,  tok);
export const setRefreshToken = (tok: string) => localStorage.setItem(REFRESH_KEY, tok);

/** Persist the user’s first name and notify same-tab listeners. */
export const setFirstName = (n: string) => {
  localStorage.setItem(NAME_KEY, n);
  broadcastJwtUpdate();
};

/* cross-tab / same-tab sync */
export function broadcastJwtUpdate() {
  window.dispatchEvent(new Event('jwt-updated'));
}

/** Purge everything except page refresh */
export function clearAuth() {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(NAME_KEY);
  broadcastJwtUpdate();
}

/* DTOs sent back by /auth/login */
export interface LoginResponse {
  accessToken:  string;
  refreshToken: string;
  firstName?:   string;
}

export interface RegisterPayload {
  email:             string;
  password:          string;
  firstName:         string;
  lastName:          string;
  verificationCode:  string;
  registrationCode?: string | null;
  requestedRole?:    string | null;
}

/* MFA challenge union */
export interface MfaChallenge {
  status: 'MFA_REQUIRED';
  mfaToken: string;
  methods: string[]; // e.g. ['TOTP']
}

export type LoginResult = LoginResponse | MfaChallenge;

/* helper - fallback to /users/me */
async function fetchFirstNameFromProfile(): Promise<string | null> {
  try {
    const me = await api<any>('/users/me');
    return me?.firstName ?? me?.given_name ?? me?.name ?? null;
  } catch {
    return null;
  }
}

/* public login - may return MFA_REQUIRED */
export async function login(
  email: string,
  password: string,
): Promise<LoginResult> {

  const data = await api<LoginResult>('/auth/login', {
    method: 'POST',
    body:   JSON.stringify({ email, password }),
  });
  if (!data) throw new Error('Empty login response');

  // If MFA required, return challenge as-is (no cookie/token yet)
  if ((data as any).status === 'MFA_REQUIRED') {
    return data as MfaChallenge;
  }

  const tokens = data as LoginResponse;

  if (!USE_COOKIE_AUTH) {
    setAccessToken (tokens.accessToken);
    setRefreshToken(tokens.refreshToken);
  }

  let firstName = tokens.firstName ?? null;
  if (!firstName) firstName = await fetchFirstNameFromProfile();
  if (firstName) setFirstName(firstName);
  if (!firstName) broadcastJwtUpdate();

  return { ...tokens, firstName: firstName ?? tokens.firstName };
}

/* verify MFA step */
export async function verifyMfa(mfaToken: string, code: string): Promise<LoginResponse> {
  const data = await api<LoginResponse>('/auth/mfa/verify', {
    method: 'POST',
    body: JSON.stringify({ mfaToken, code }),
  });
  if (!data) throw new Error('Empty MFA verify response');

  if (!USE_COOKIE_AUTH) {
    setAccessToken (data.accessToken);
    setRefreshToken(data.refreshToken);
  }

  let firstName = data.firstName ?? (await fetchFirstNameFromProfile());
  if (firstName) setFirstName(firstName);
  if (!firstName) broadcastJwtUpdate();

  return data;
}

export async function register(payload: RegisterPayload): Promise<void> {
  await api('/auth/register', { method: 'POST', body: JSON.stringify(payload) });
}

export async function sendVerificationCode(email: string): Promise<void> {
  await api('/auth/send-code', { method: 'POST', body: JSON.stringify({ email }) });
}

export async function requestPasswordReset(email: string): Promise<void> {
  await api('/auth/password-reset/request', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

/* public logout */
export function logout(): void {
  // Cookie mode: best-effort server logout + clear cookies
  if (USE_COOKIE_AUTH) {
    // fire-and-forget; we still clear local and reload regardless
    api('/auth/logout', { method: 'POST' }).catch(() => {});
  }
  clearAuth();
  window.dispatchEvent(new Event('user-logout'));
  window.location.reload();
}
