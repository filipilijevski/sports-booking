import { api } from './api';

export interface MfaSetupResponse {
  otpauthUrl: string;
  maskedSecret: string;
  qrDataUrl?: string | null;
}

export async function mfaStatus(): Promise<{ enabled: boolean }> {
  return await api('/mfa/status') as { enabled: boolean };
}

export async function mfaSetup(password: string): Promise<MfaSetupResponse> {
  return await api('/mfa/setup', {
    method: 'POST',
    body: JSON.stringify({ password }),
  }) as MfaSetupResponse;
}

export async function mfaEnable(code: string): Promise<{ recoveryCodes: string[] }> {
  return await api('/mfa/enable', {
    method: 'POST',
    body: JSON.stringify({ code }),
  }) as { recoveryCodes: string[] };
}

export async function mfaDisable(args: { password?: string; recoveryCode?: string }): Promise<void> {
  await api('/mfa/disable', {
    method: 'POST',
    body: JSON.stringify(args),
  });
}
