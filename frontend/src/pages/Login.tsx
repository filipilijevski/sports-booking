import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Container,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Link,
} from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import { login, broadcastJwtUpdate, requestPasswordReset, type LoginResult } from '../lib/auth';
import { verifyMfa } from '../lib/auth';
import { GOOGLE_OAUTH_URL } from '../lib/oauth';

export interface LoginProps { onLoginSuccess: () => void; }
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;

export default function Login({ onLoginSuccess }: LoginProps) {
  const [email,   setEmail]   = useState('');
  const [pw,      setPw]      = useState('');
  const [loading, setLoading] = useState(false);
  const [err,     setErr]     = useState('');
  const [emailErr, setEmailErr] = useState('');
  const [mfaToken, setMfaToken] = useState<string | null>(null);
  const [mfaCode, setMfaCode] = useState('');
  const [forgot, setForgot] = useState(false);
  const [resetMsg, setResetMsg] = useState('');

  const nav = useNavigate();
  const inMfa = !!mfaToken;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErr(''); setEmailErr(''); setResetMsg(''); setLoading(true);

    try {
      if (inMfa) {
        if (!/^\d{6}$/.test(mfaCode)) {
          setErr('Enter the 6-digit code.');
          return;
        }
        await verifyMfa(mfaToken!, mfaCode);
        broadcastJwtUpdate();
        onLoginSuccess();
        nav('/', { replace: true });
        return;
      }

      const eaddr = email.trim();
      if (!emailRegex.test(eaddr)) {
        setEmailErr('Please enter a valid email address.');
        throw new Error('Please enter a valid email address.');
      }

      if (forgot) {
        await requestPasswordReset(eaddr);
        setResetMsg('If that email exists, a temporary password has been sent.');
        return;
      }

      const res = await login(eaddr, pw.trim()) as LoginResult;
      if ((res as any).status === 'MFA_REQUIRED') {
        setMfaToken((res as any).mfaToken);
        return;
      }

      broadcastJwtUpdate();
      onLoginSuccess();
      nav('/', { replace: true });
    } catch (e: any) {
      if (e?.code === 'INVALID_CREDENTIALS') setErr('Incorrect username or password.');
      else if (e?.code === 'INVALID_MFA_CODE') setErr('Incorrect or expired code.');
      else if (e?.code === 'MFA_TOKEN_INVALID') { setErr('Your MFA session expired. Please sign in again.'); setMfaToken(null); setMfaCode(''); }
      else if (e?.code === 'MFA_TOO_MANY_ATTEMPTS') { setErr('Too many invalid attempts. Please sign in again.'); setMfaToken(null); setMfaCode(''); }
      else setErr(e?.message ?? 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Box component="img" src="/logo.svg" alt="Club logo" sx={{ height: 80, mb: 2 }} />

        <Typography component="h1" variant="h4" gutterBottom>
          {inMfa ? 'Two-Factor Authentication' : (forgot ? 'Forgot password' : 'TT Club Login')}
        </Typography>

        {err && <Alert severity="error" sx={{ width: '100%', mb: 2 }}>{err}</Alert>}
        {resetMsg && <Alert severity="info" sx={{ width: '100%', mb: 2 }}>{resetMsg}</Alert>}

        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1, width: '100%' }}>
          {inMfa ? (
            <>
              <TextField
                label="6-digit code"
                type="tel"
                margin="normal"
                required
                fullWidth
                value={mfaCode}
                onChange={e => setMfaCode(e.target.value.replace(/[^\d]/g, '').slice(0, 6))}
                inputProps={{ inputMode: 'numeric', pattern: '\\d*', maxLength: 6 }}
              />
              <Button type="submit" fullWidth variant="contained" sx={{ mt: 3, mb: 2, py: 1.5 }} disabled={loading}>
                {loading ? <CircularProgress size={26} /> : 'Verify'}
              </Button>
              <Button fullWidth variant="text" onClick={() => { setMfaToken(null); setMfaCode(''); }}>
                Back
              </Button>
            </>
          ) : (
            <>
              <TextField
                label="Email address"
                type="email"
                margin="normal"
                required
                fullWidth
                autoComplete="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                error={!!emailErr}
                helperText={emailErr}
              />
              {!forgot && (
                <TextField
                  label="Password"
                  type="password"
                  margin="normal"
                  required
                  fullWidth
                  autoComplete="current-password"
                  value={pw}
                  onChange={e => setPw(e.target.value)}
                />
              )}

              {!forgot && (
                <>
                  <Button type="submit" fullWidth variant="contained" sx={{ mt: 3, mb: 2, py: 1.5 }} disabled={loading}>
                    {loading ? <CircularProgress size={26} /> : 'Sign In'}
                  </Button>

                  <Button fullWidth variant="outlined" startIcon={<GoogleIcon />} component="a" href={GOOGLE_OAUTH_URL} sx={{ mb: 2, py: 1.5 }}>
                    Sign in with Google
                  </Button>

                  <Box sx={{ textAlign: 'right' }}>
                    <Link component="button" type="button" onClick={() => { setForgot(true); setErr(''); setResetMsg(''); }}>
                      Forgot password?
                    </Link>
                  </Box>
                </>
              )}

              {forgot && (
                <>
                  <Button type="submit" fullWidth variant="contained" sx={{ mt: 3, mb: 2, py: 1.5 }} disabled={loading}>
                    {loading ? <CircularProgress size={26} /> : 'Send temporary password'}
                  </Button>
                  <Button fullWidth variant="text" onClick={() => setForgot(false)}>
                    Back
                  </Button>
                </>
              )}
            </>
          )}
        </Box>
      </Box>
    </Container>
  );
}
