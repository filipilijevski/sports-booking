import { useEffect, useState } from 'react';
import { Box, Paper, Stack, Typography, Button, TextField, Alert, Divider } from '@mui/material';
import { mfaStatus, mfaSetup, mfaEnable, mfaDisable } from '../../lib/mfa';

export default function MfaCard({ isOauth = false }: { isOauth?: boolean }) {
  const [enabled, setEnabled] = useState<boolean>(false);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string>('');
  const [info, setInfo] = useState<string>('');

  // setup state
  const [password, setPassword] = useState('');
  const [setupUri, setSetupUri] = useState<string | null>(null);
  const [qr, setQr] = useState<string | null>(null);
  const [maskedSecret, setMasked] = useState<string | null>(null);
  const [totp, setTotp] = useState('');

  // recovery codes (after enable)
  const [codes, setCodes] = useState<string[] | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const s = await mfaStatus();
        setEnabled(!!s.enabled);
      } catch {
        /* best-effort; keep silent */
      }
    })();
  }, []);

  const beginSetup = async () => {
    setErr(''); setInfo('');
    if (isOauth) {
      setErr('Two-factor authentication via TOTP is managed by your Google account and cannot be enabled here.');
      return;
    }
    if (!password) { setErr('Enter your current password.'); return; }
    setLoading(true);
    try {
      const r = await mfaSetup(password);
      setSetupUri(r.otpauthUrl);
      setQr(r.qrDataUrl ?? null);
      setMasked(r.maskedSecret);
      setInfo('Scan the QR in your authenticator app, then enter the 6-digit code to confirm.');
    } catch (e: any) {
      setErr(e?.message || 'Unable to start 2FA setup.');
    } finally {
      setLoading(false);
    }
  };

  const confirmEnable = async () => {
    setErr(''); setInfo('');
    if (!/^\d{6}$/.test(totp)) { setErr('Enter the 6-digit code.'); return; }
    setLoading(true);
    try {
      const r = await mfaEnable(totp);
      setCodes(r.recoveryCodes || []);
      setEnabled(true);
      setSetupUri(null); setQr(null); setMasked(null); setTotp('');
      setInfo('Two-factor authentication is now enabled. Save your recovery codes in a safe place.');
    } catch (e: any) {
      setErr(e?.message || 'Invalid code');
    } finally {
      setLoading(false);
    }
  };

  const doDisable = async () => {
    setErr(''); setInfo('');
    if (!password && !totp) { setErr('Enter your current password or a recovery code.'); return; }
    setLoading(true);
    try {
      await mfaDisable({ password: password || undefined, recoveryCode: totp || undefined });
      setEnabled(false);
      setCodes(null);
      setInfo('Two-factor authentication has been disabled.');
      setPassword(''); setTotp('');
    } catch (e: any) {
      setErr(e?.message || 'Could not disable 2FA');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Paper elevation={3} sx={{ p: 2.5, borderRadius: 3 }}>
      <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
        Two-Factor Authentication (TOTP)
      </Typography>
      {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
      {info && <Alert severity="info"  sx={{ mb: 2 }}>{info}</Alert>}

      {!enabled && !setupUri && (
        <>
          {isOauth ? (
            <Alert severity="info">
              Your account is managed via Google OAuth2. TOTP-based 2FA is not enabled here.
              If you want MFA, please turn on 2-Step Verification in your Google account.
            </Alert>
          ) : (
            <Stack spacing={1.5}>
              <Typography variant="body2">
                Add an extra layer of security to your account with a 6-digit code from
                Google/Microsoft Authenticator or any TOTP app.
              </Typography>
              <TextField
                label="Current password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                fullWidth
              />
              <Box>
                <Button variant="contained" onClick={beginSetup} disabled={loading || !password}>
                  Begin setup
                </Button>
              </Box>
            </Stack>
          )}
        </>
      )}

      {!enabled && setupUri && (
        <Stack spacing={2}>
          {qr ? (
            <Box sx={{ textAlign: 'center' }}>
              <img src={qr} alt="Authenticator QR" style={{ width: 220, height: 220 }} />
            </Box>
          ) : (
            <Alert severity="info">
              Scan this URL in your authenticator app: <br />
              <Typography variant="caption" sx={{ wordBreak: 'break-all' }}>{setupUri}</Typography>
            </Alert>
          )}
          <Typography variant="body2">Secret: <code>{maskedSecret}</code></Typography>
          <TextField
            label="6-digit code"
            value={totp}
            onChange={e => setTotp(e.target.value.replace(/[^\d]/g, '').slice(0, 6))}
            inputProps={{ inputMode: 'numeric', pattern: '\\d*', maxLength: 6 }}
            fullWidth
          />
          <Box>
            <Button variant="contained" onClick={confirmEnable} disabled={loading || !/^\d{6}$/.test(totp)}>
              Enable 2FA
            </Button>
          </Box>
        </Stack>
      )}

      {enabled && (
        <>
          <Alert severity="success" sx={{ mb: 2 }}>
            Two-factor authentication is enabled for your account.
          </Alert>

          {codes && (
            <Box sx={{ mb: 2 }}>
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Recovery codes (store securely):</Typography>
              <Box component="pre" sx={{ p: 1.5, background: '#f5f5f5', borderRadius: 1, whiteSpace: 'pre-wrap' }}>
                {codes.join('\n')}
              </Box>
              <Stack direction="row" spacing={1}>
                <Button
                  variant="outlined"
                  onClick={() => {
                    const blob = new Blob([codes.join('\n')], { type: 'text/plain' });
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'ttclub-recovery-codes.txt';
                    a.click();
                    URL.revokeObjectURL(url);
                  }}
                >
                  Download .txt
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => navigator.clipboard.writeText(codes.join('\n')).catch(() => {})}
                >
                  Copy
                </Button>
              </Stack>
              <Divider sx={{ my: 2 }} />
            </Box>
          )}

          <Stack spacing={1.5}>
            <Typography variant="body2">
              To disable 2FA you must confirm your password (or use a recovery code).
            </Typography>
            {!isOauth && (
              <TextField
                label="Current password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                fullWidth
              />
            )}
            <TextField
              label="Recovery code (optional alternative)"
              type="text"
              value={totp}
              onChange={e => setTotp(e.target.value.trim())}
              fullWidth
            />
            <Box>
              <Button variant="outlined" color="error" onClick={doDisable} disabled={loading}>
                Disable 2FA
              </Button>
            </Box>
          </Stack>
        </>
      )}
    </Paper>
  );
}
