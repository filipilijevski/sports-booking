import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Link,
  Stack,
} from '@mui/material';
import GoogleIcon from '@mui/icons-material/Google';
import { useState, useEffect, type FormEvent } from 'react';
import {
  login,
  register,
  sendVerificationCode,
  broadcastJwtUpdate,
  requestPasswordReset,
  type LoginResult,
} from '../lib/auth';
import { verifyMfa } from '../lib/auth';
import { GOOGLE_OAUTH_URL } from '../lib/oauth';

interface Props {
  open: boolean;
  onClose: () => void;
  onAuthSuccess: () => void;
}

type FieldErrors = Partial<{
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  verificationCode: string;
  code: string; // MFA
}>;

const RESEND_COOLDOWN_SEC = 60;
const MIN_PW_LEN = 8;

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/i;
const isValidEmail = (e: string) => emailRegex.test(e.trim());

const storeRedirect = () => sessionStorage.setItem('postLoginRedirect', window.location.pathname);

export default function AuthDialog({ open, onClose, onAuthSuccess }: Props) {
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [loading, setLoading] = useState(false);

  const [err, setErr] = useState('');
  const [success, setSuccess] = useState('');
  const [fields, setFields] = useState<FieldErrors>({});

  const [email, setEmail] = useState('');
  const [pw, setPw] = useState('');

  const [confirmPw, setConfirmPw] = useState('');
  const [first, setFirst] = useState('');
  const [last, setLast] = useState('');
  const [code, setCode] = useState('');

  const [cooldown, setCooldown] = useState(0);

  // MFA
  const [mfaToken, setMfaToken] = useState<string | null>(null);
  const inMfa = !!mfaToken;
  const [mfaCode, setMfaCode] = useState('');

  // Forgot password
  const [forgot, setForgot] = useState(false);
  const [resetMsg, setResetMsg] = useState('');

  useEffect(() => {
    if (cooldown === 0) return;
    const id = setInterval(() => setCooldown(sec => sec - 1), 1_000);
    return () => clearInterval(id);
  }, [cooldown]);

  function reset() {
    setErr('');
    setSuccess('');
    setFields({});
    setLoading(false);
    setEmail('');
    setPw('');
    setConfirmPw('');
    setFirst('');
    setLast('');
    setCode('');
    setMfaToken(null);
    setMfaCode('');
    setCooldown(0);
    setResetMsg('');
    setForgot(false);
    setTab('login');
  }

  function handleClose() {
    reset();
    onClose();
  }

  const canSendCode =
    !!first.trim() &&
    !!last.trim() &&
    isValidEmail(email) &&
    pw.trim().length >= MIN_PW_LEN &&
    pw.trim() === confirmPw.trim();

  async function handleSendCode() {
    if (loading || cooldown > 0) return;
    setErr(''); setSuccess(''); setFields({});

    const eaddr = email.trim();
    const fErrs: FieldErrors = {};
    if (!first.trim())                    fErrs.firstName = 'First name is required.';
    if (!last.trim())                     fErrs.lastName = 'Last name is required.';
    if (!isValidEmail(eaddr))             fErrs.email = 'Please enter a valid email address.';
    if (pw.trim().length < MIN_PW_LEN)    fErrs.password = `Password must be at least ${MIN_PW_LEN} characters.`;
    if (pw.trim() !== confirmPw.trim())   fErrs.confirmPassword = 'Passwords do not match.';
    if (Object.keys(fErrs).length > 0) { setFields(fErrs); return; }

    try {
      setLoading(true);
      await sendVerificationCode(eaddr);
      setSuccess('Code sent! Check your inbox.');
      setCooldown(RESEND_COOLDOWN_SEC);
    } catch (ex: any) {
      const fe: FieldErrors = ex?.fieldErrors || {};
      setFields(fe);
      setErr(ex?.message || 'Could not send code');
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErr(''); setSuccess(''); setFields({}); setLoading(true);

    try {
      if (inMfa) {
        if (!/^\d{6}$/.test(mfaCode)) {
          setFields({ code: 'Enter the 6-digit code.' });
          throw new Error('Enter the 6-digit code.');
        }
        const tokens = await verifyMfa(mfaToken!, mfaCode);
        broadcastJwtUpdate();
        onAuthSuccess();
        handleClose();
        return;
      }

      if (forgot) {
        const eaddr = email.trim();
        if (!isValidEmail(eaddr)) {
          setFields({ email: 'Please enter a valid email address.' });
          throw new Error('Please enter a valid email address.');
        }
        await requestPasswordReset(eaddr);
        setResetMsg('If that email exists, a temporary password has been sent.');
        return;
      }

      const eaddr = email.trim();
      if (!isValidEmail(eaddr)) {
        setFields({ email: 'Please enter a valid email address.' });
        throw new Error('Please enter a valid email address.');
      }

      if (tab === 'login') {
        const res = await login(eaddr, pw.trim());
        if ((res as any).status === 'MFA_REQUIRED') {
          setMfaToken((res as any).mfaToken);
          return;
        }
        broadcastJwtUpdate();
        onAuthSuccess();
        handleClose();
        return;
      }

      if (!first.trim())   { setFields(f => ({ ...f, firstName: 'First name is required.' })); }
      if (!last.trim())    { setFields(f => ({ ...f, lastName: 'Last name is required.' })); }
      if (pw.trim().length < MIN_PW_LEN) {
        setFields(f => ({ ...f, password: `Password must be at least ${MIN_PW_LEN} characters.` }));
        throw new Error(`Password must be at least ${MIN_PW_LEN} characters.`);
      }
      if (pw !== confirmPw) {
        setFields(f => ({ ...f, confirmPassword: 'Passwords do not match.' }));
        throw new Error('Passwords do not match');
      }
      if (code.trim().length !== 6 || !/^\d{6}$/.test(code.trim())) {
        setFields(f => ({ ...f, verificationCode: 'Enter the 6-digit code.' }));
        throw new Error('Enter the 6-digit code');
      }

      await register({
        email:            eaddr,
        password:         pw,
        firstName:        first.trim(),
        lastName:         last.trim(),
        verificationCode: code.trim(),
      });

      setSuccess('Registration successful — now sign in.');
      setTab('login');
      setPw(''); setConfirmPw(''); setCode('');
    } catch (ex: any) {
      if (ex?.code === 'INVALID_MFA_CODE') setErr('Incorrect or expired code.');
      else if (ex?.code === 'MFA_TOKEN_INVALID') setErr('Your MFA session expired. Please sign in again.');
      else if (ex?.code === 'MFA_TOO_MANY_ATTEMPTS') setErr('Too many invalid attempts. Please sign in again.');
      else if (ex?.fieldErrors) setFields(ex.fieldErrors as FieldErrors);
      else setErr(ex?.message || 'Request failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onClose={handleClose}>
      <DialogTitle>
        {inMfa ? 'Two-Factor Authentication' : (tab === 'login' ? (forgot ? 'Forgot password' : 'Sign in') : 'Create account')}
      </DialogTitle>

      {!inMfa && !forgot && (
        <Tabs
          value={tab}
          onChange={(_, v) => { setTab(v); setErr(''); setSuccess(''); setFields({}); }}
          centered
          sx={{ px: 3 }}
        >
          <Tab value="login"    label="Login"    />
          <Tab value="register" label="Register" />
        </Tabs>
      )}

      <Box component="form" onSubmit={handleSubmit}>
        <DialogContent dividers sx={{ width: 420, maxWidth: '90vw' }}>
          {err     && <Alert severity="error"   sx={{ mb: 2 }}>{err}</Alert>}
          {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
          {resetMsg && <Alert severity="info" sx={{ mb: 2 }}>{resetMsg}</Alert>}

          {inMfa ? (
            <>
              <Alert severity="info" sx={{ mb: 2 }}>
                Enter the 6-digit code from your authenticator app.
              </Alert>
              <TextField
                label="6-digit code"
                fullWidth
                value={mfaCode}
                onChange={e => setMfaCode(e.target.value.replace(/[^\d]/g, '').slice(0, 6))}
                inputProps={{ inputMode: 'numeric', pattern: '\\d*', maxLength: 6 }}
                error={!!fields.code}
                helperText={fields.code}
              />
            </>
          ) : forgot ? (
            <>
              <Alert severity="info" sx={{ mb: 2 }}>
                Enter your email and we’ll send a <b>temporary password</b> (valid for 45 minutes).
              </Alert>
              <TextField
                label="Email address"
                type="email"
                fullWidth
                margin="dense"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                error={!!fields.email}
                helperText={fields.email}
              />
            </>
          ) : (
            <>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<GoogleIcon />}
                sx={{ mb: 3, py: 1.2 }}
                component="a"
                href={GOOGLE_OAUTH_URL}
                onClick={storeRedirect}
              >
                Continue with Google
              </Button>

              {tab === 'register' && (
                <>
                  <TextField label="First name" fullWidth margin="dense" value={first} onChange={e => setFirst(e.target.value)} required error={!!fields.firstName} helperText={fields.firstName} />
                  <TextField label="Last name"  fullWidth margin="dense" value={last}  onChange={e => setLast(e.target.value)} required error={!!fields.lastName}  helperText={fields.lastName} />
                </>
              )}

              <TextField
                label="Email address"
                type="email"
                fullWidth
                margin="dense"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                error={!!fields.email}
                helperText={fields.email}
              />
              <TextField
                label="Password"
                type="password"
                fullWidth
                margin="dense"
                value={pw}
                onChange={e => setPw(e.target.value)}
                required
                error={!!fields.password}
                helperText={fields.password || (tab === 'register' ? `Minimum ${MIN_PW_LEN} characters.` : '')}
              />

              {tab === 'register' && (
                <>
                  <TextField
                    label="Confirm password"
                    type="password"
                    fullWidth
                    margin="dense"
                    value={confirmPw}
                    onChange={e => setConfirmPw(e.target.value)}
                    required
                    error={!!fields.confirmPassword}
                    helperText={fields.confirmPassword}
                  />

                  <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                    <TextField
                      label="6-digit code"
                      fullWidth
                      value={code}
                      onChange={e => {
                        const v = e.target.value.replace(/[^\d]/g, '').slice(0, 6);
                        setCode(v);
                      }}
                      inputProps={{ inputMode: 'numeric', pattern: '\\d*', maxLength: 6 }}
                      error={!!fields.verificationCode}
                      helperText={fields.verificationCode}
                    />
                    <Button onClick={handleSendCode} disabled={loading || cooldown > 0 || !canSendCode}>
                      {cooldown > 0 ? `Resend (${cooldown})` : 'Send code'}
                    </Button>
                  </Box>
                </>
              )}

              {tab === 'login' && (
                <Stack direction="row" justifyContent="flex-end" sx={{ mt: 1 }}>
                  <Link component="button" type="button" onClick={() => { setForgot(true); setErr(''); setSuccess(''); setFields({}); }}>
                    Forgot password?
                  </Link>
                </Stack>
              )}
            </>
          )}
        </DialogContent>

        <DialogActions sx={{ px: 3, pb: 3 }}>
          {!inMfa && <Button onClick={handleClose}>Cancel</Button>}
          {inMfa && (
            <Button onClick={() => { setMfaToken(null); setMfaCode(''); setErr(''); setFields({}); }} disabled={loading}>
              Back
            </Button>
          )}
          {forgot && (
            <Button onClick={() => { setForgot(false); setResetMsg(''); }} disabled={loading}>
              Back
            </Button>
          )}
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? <CircularProgress size={22} /> : (inMfa ? 'Verify' : (forgot ? 'Send' : (tab === 'login' ? 'Login' : 'Register')))}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
