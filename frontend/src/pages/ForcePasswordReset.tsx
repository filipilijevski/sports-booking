import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Container,
  Paper,
  Typography,
  TextField,
  Stack,
  Button,
  Alert,
  CircularProgress,
} from '@mui/material';
import { api } from '../lib/api';
import { useRole } from '../context/RoleContext';
import { useAuthDialog } from '../context/AuthDialogContext';
import { clearAuth } from '../lib/auth';

const BG =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

export default function ForcePasswordReset() {
  const nav = useNavigate();
  const { pwdChangeRequired, ready, role } = useRole();
  const { open } = useAuthDialog();

  const [pw1, setPw1] = useState('');
  const [pw2, setPw2] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // If user is not required to change password (e.g., they visited the route manually),
  // bounce to profile (or home) once role is ready.
  useEffect(() => {
    if (!ready) return;
    if (!pwdChangeRequired) {
      nav(role !== 'GUEST' ? '/profile' : '/', { replace: true });
    }
  }, [ready, pwdChangeRequired, role, nav]);

  async function submit() {
    setErr(null);
    setOk(null);

    const p1 = pw1.trim();
    const p2 = pw2.trim();

    if (p1.length < 8) {
      setErr('Password must be at least 8 characters.');
      return;
    }
    if (p1 !== p2) {
      setErr('Passwords do not match.');
      return;
    }

    try {
      setBusy(true);
      // Allowed by PasswordChangeRequiredFilter even when pwd_change_required=true
      await api<void>('/users/me/password', {
        method: 'POST',
        body: JSON.stringify({ newPassword: p1, confirmPassword: p2 }),
      });

      setOk('Password changed successfully. You will now be signed out to log in with your new password.');
      setPw1('');
      setPw2('');

      // Graceful, no-reload logout so we can open the sign-in dialog right after.
      setTimeout(async () => {
        try {
          await api('/auth/logout', { method: 'POST' }).catch(() => {});
        } finally {
          clearAuth();                 // clears localStorage tokens/names + emits 'jwt-updated'
          open();                      // show the login dialog
          nav('/', { replace: true }); // send user to landing page with dialog open
        }
      }, 1500);
    } catch (e: any) {
      setErr(e?.message || 'Failed to change password');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Box component="main" sx={{ minHeight: '100dvh', position: 'relative' }}>
      <Box
        aria-hidden
        sx={{
          position: 'fixed',
          inset: 0,
          zIndex: -1,
          backgroundImage: BG,
          backgroundRepeat: 'no-repeat',
          backgroundAttachment: 'fixed',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      />
      <Container maxWidth="sm" sx={{ py: { xs: 4, md: 6 } }}>
        <Paper
          elevation={3}
          sx={{ p: 3, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.97)' }}
        >
          <Typography variant="h5" sx={{ fontWeight: 800, mb: 1 }}>
            Update Your Password
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            You signed in with a temporary password. Please set a new password to continue.
          </Typography>

          {err && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {err}
            </Alert>
          )}
          {ok && (
            <Alert severity="success" sx={{ mb: 2 }}>
              {ok}
            </Alert>
          )}

          <Stack spacing={2}>
            <TextField
              label="New password"
              type="password"
              fullWidth
              value={pw1}
              onChange={(e) => setPw1(e.target.value)}
              disabled={busy}
              helperText="Minimum 8 characters."
            />
            <TextField
              label="Confirm new password"
              type="password"
              fullWidth
              value={pw2}
              onChange={(e) => setPw2(e.target.value)}
              disabled={busy}
            />
            <Stack direction="row" spacing={2} justifyContent="flex-end">
              <Button
                variant="contained"
                onClick={submit}
                disabled={busy}
              >
                {busy ? <CircularProgress size={20} /> : 'Save Password'}
              </Button>
            </Stack>
          </Stack>
        </Paper>
      </Container>
    </Box>
  );
}
