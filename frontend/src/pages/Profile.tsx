import { useEffect, useState, useMemo } from 'react';
import {
  Box, Container, Paper, Stack, TextField, Button, Typography,
  CircularProgress, Alert, Snackbar, Divider, Chip,
  Table, TableHead, TableRow, TableCell, TableBody, LinearProgress,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useRole } from '../context/RoleContext';
import { api } from '../lib/api';
import { fetchMyMemberships, type MyMembershipsPayload } from '../lib/booking';
import { fetchMyTableCreditSummary, type TableCreditSummary } from '../lib/booking';
import MfaCard from '../components/security/MfaCard';

const INFO_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

type MeDto = {
  id: number;
  email: string;
  role: string;
  firstName: string | null;
  lastName: string | null;
  provider?: 'LOCAL' | 'GOOGLE' | string;
};

export default function Profile() {
  const [me, setMe] = useState<MeDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [savingInfo, setSavingInfo] = useState(false);
  const [savingPwd, setSavingPwd] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);

  const [firstName, setFirst] = useState('');
  const [lastName, setLast] = useState('');
  const [email, setEmail] = useState('');
  const [pwd1, setPwd1] = useState('');
  const [pwd2, setPwd2] = useState('');

  const { role, pwdChangeRequired } = useRole();
  const isClient = role === 'CLIENT';
  const [my, setMy] = useState<MyMembershipsPayload | null>(null);
  const [myErr, setMyErr] = useState<string | null>(null);

  // table rental credits summary
  const [credits, setCredits] = useState<TableCreditSummary | null>(null);
  const [creditsErr, setCreditsErr] = useState<string | null>(null);

  const nav = useNavigate();

  const load = async () => {
    try {
      setLoading(true);
      const data = await api<MeDto>('/users/me');
      if (!data) throw new Error('Failed to load profile');
      setMe(data);
      setFirst(data.firstName ?? '');
      setLast(data.lastName ?? '');
      setEmail(data.email ?? '');

      if (isClient) {
        const mine = await fetchMyMemberships().catch((e:any) => { setMyErr(e?.message ?? 'Failed to load memberships'); return null; });
        setMy(mine ?? null);

        const cred = await fetchMyTableCreditSummary().catch((e:any) => { setCreditsErr(e?.message ?? 'Failed to load table rental credits'); return null; });
        setCredits(cred ?? null);
      } else {
        setMy(null);
        setCredits(null);
      }
    } catch (e: any) {
      const msg = e?.message || '';
      setErr(msg.toLowerCase().includes('401')
        ? 'Your session has expired. Please log in again.'
        : (msg || 'Failed to load profile'));
      if ((e?.status ?? 0) === 401 || msg.toLowerCase().includes('401')) {
        nav('/');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [role]);

  const isOauth = me?.provider && me.provider !== 'LOCAL';

  const saveInfo = async () => {
    try {
      setSavingInfo(true);
      setErr(null);
      const body: any = {};
      if (firstName !== (me?.firstName ?? '')) body.firstName = firstName;
      if (lastName !== (me?.lastName ?? '')) body.lastName = lastName;
      if (!isOauth && email !== (me?.email ?? '')) body.email = email;

      const updated = await api<MeDto>('/users/me', {
        method: 'PATCH',
        body: JSON.stringify(body),
      });
      if (!updated) throw new Error('Failed to update profile');

      setMe(updated);
      setFirst(updated.firstName ?? '');
      setLast(updated.lastName ?? '');
      setEmail(updated.email ?? '');

      localStorage.setItem('firstName', updated.firstName ?? '');
      window.dispatchEvent(new Event('jwt-updated'));

      setOk('Profile updated successfully.');
    } catch (e: any) {
      setErr(e?.message || 'Failed to update profile');
    } finally {
      setSavingInfo(false);
    }
  };

  const changePassword = async () => {
    if (pwd1.length < 8) {
      setErr('New password must be at least 8 characters long.');
      return;
    }
    if (pwd1 !== pwd2) {
      setErr('Passwords do not match.');
      return;
    }
    try {
      setSavingPwd(true);
      setErr(null);
      await api<void>('/users/me/password', {
        method: 'POST',
        body: JSON.stringify({ newPassword: pwd1, confirmPassword: pwd2 }),
      });
      setPwd1('');
      setPwd2('');
      setOk('Password changed successfully.');
    } catch (e: any) {
      setErr(e?.message || 'Failed to change password');
    } finally {
      setSavingPwd(false);
    }
  };

  const activeEnrollments = useMemo(
    () => (my?.enrollments ?? []).filter(e => e.status === 'ACTIVE'),
    [my?.enrollments]
  );
  const pastEnrollments = useMemo(
    () => (my?.enrollments ?? []).filter(e => e.status !== 'ACTIVE'),
    [my?.enrollments]
  );

  const pct = (purchased?: number | null, remaining?: number | null) => {
    const P = purchased ?? 0;
    const R = remaining ?? 0;
    if (P <= 0) return 0;
    const used = Math.max(0, P - R);
    const v = Math.min(100, Math.round((used / P) * 100));
    return isFinite(v) ? v : 0;
  };

  return (
    <Box component="main" sx={{ minHeight: '100dvh', position: 'relative' }}>
      <Box
        aria-hidden
        sx={{
          position: 'fixed',
          inset: 0,
          zIndex: -1,
          backgroundImage: INFO_GRADIENT,
          backgroundRepeat: 'no-repeat',
          backgroundAttachment: 'fixed',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      />
      <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
        <Typography variant="h4" sx={{ color: 'common.white', fontWeight: 700, mb: 2 }}>
          My Profile
        </Typography>

        {err && (
          <Paper elevation={3} sx={{ p: 2, mb: 2, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
            <Alert severity="error">{err}</Alert>
          </Paper>
        )}

        {loading ? (
          <Paper elevation={3} sx={{ p: 4, textAlign: 'center', borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
            <CircularProgress />
          </Paper>
        ) : (
          <>
            {/* show forced-change hint if we logged in with a temporary password */}
            {pwdChangeRequired && (
              <Paper elevation={3} sx={{ p: 2, mb: 2, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
                <Alert severity="warning">
                  You signed in with a temporary password. Please change your password below to continue using the site.
                </Alert>
              </Paper>
            )}

            <Paper elevation={3} sx={{ p: 3, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>Profile Information</Typography>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 1}}>
                <TextField label="First Name" fullWidth value={firstName} onChange={e => setFirst(e.target.value)} />
                <TextField label="Last Name"  fullWidth value={lastName}  onChange={e => setLast(e.target.value)} />
              </Stack>
              <TextField
                label="Email"
                fullWidth
                value={email}
                onChange={e => setEmail(e.target.value)}
                disabled={!!isOauth}
                helperText={isOauth ? 'Email is linked to your Google account and cannot be changed here.' : ' '}
              />
              <Stack direction="row" spacing={2} justifyContent="flex-end" sx={{ mt: 2 }}>
                <Button variant="contained" onClick={saveInfo} disabled={savingInfo}>
                  {savingInfo ? <CircularProgress size={18} /> : 'Save Changes'}
                </Button>
              </Stack>
            </Paper>

            <Divider sx={{ my: 3, opacity: 0.5 }} />

            <Paper elevation={3} sx={{ p: 3, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>Change Password</Typography>
              {isOauth ? (
                <Alert severity="info">
                  Your account is managed via Google OAuth2. Password changes are not available here.
                </Alert>
              ) : (
                <>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                    <TextField label="New Password" type="password" fullWidth value={pwd1} onChange={e => setPwd1(e.target.value)} />
                    <TextField label="Confirm New Password" type="password" fullWidth value={pwd2} onChange={e => setPwd2(e.target.value)} />
                  </Stack>
                  <Stack direction="row" spacing={2} justifyContent="flex-end" sx={{ mt: 2 }}>
                    <Button variant="outlined" onClick={changePassword} disabled={savingPwd}>
                      {savingPwd ? <CircularProgress size={18} /> : 'Update Password'}
                    </Button>
                  </Stack>
                </>
              )}
            </Paper>

            {/* MFA section */}
            <Divider sx={{ my: 3, opacity: 0.5 }} />
            <MfaCard isOauth={!!isOauth} />

            {isClient && (
              <>
                {/* Table rental credits summary */}
                <Divider sx={{ my: 3, opacity: 0.5 }} />
                <Paper elevation={3} sx={{ p: 3, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
                  <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                    My Table Rental Credits
                  </Typography>

                  {creditsErr && <Alert severity="warning" sx={{ mb:2 }}>{creditsErr}</Alert>}

                  {!credits ? (
                    <Typography variant="body2">No credit information available.</Typography>
                  ) : (
                    <Stack direction="row" spacing={1.5} useFlexGap flexWrap="wrap">
                      <Chip label={`Total Remaining: ${credits.hoursRemaining.toFixed(2)} hrs`} color="primary" />
                      <Chip
                        label={`Purchased: ${(credits.purchasedHours ?? 0).toFixed(2)} hrs`}
                        variant="outlined"
                      />
                      <Chip
                        label={`Membership: ${(credits.membershipHours ?? 0).toFixed(2)} hrs`}
                        variant="outlined"
                      />
                    </Stack>
                  )}
                </Paper>

                <Divider sx={{ my: 3, opacity: 0.5 }} />

                <Paper elevation={3} sx={{ p: 3, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
                  <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                    My Memberships & Programs
                  </Typography>

                  {myErr && <Alert severity="warning" sx={{ mb:2 }}>{myErr}</Alert>}

                  <Typography variant="subtitle2" sx={{ mb: 1 }}>Memberships</Typography>
                  {!my?.memberships?.length ? (
                    <Typography variant="body2" sx={{ mb: 2 }}>No membership records found.</Typography>
                  ) : (
                    <Table size="small" sx={{ mb: 2 }}>
                      <TableHead>
                        <TableRow>
                          <TableCell>Plan</TableCell>
                          <TableCell>Type</TableCell>
                          <TableCell>Active</TableCell>
                          <TableCell>Start</TableCell>
                          <TableCell>End</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {my.memberships!.map((m, i) => (
                          <TableRow key={`${m.planId}-${i}`}>
                            <TableCell>{m.planName ?? `Plan #${m.planId}`}</TableCell>
                            <TableCell>{m.planType ?? '—'}</TableCell>
                            <TableCell>{m.active ? 'Yes' : 'No'}</TableCell>
                            <TableCell>{m.startTs ? new Date(m.startTs).toLocaleDateString() : '—'}</TableCell>
                            <TableCell>{m.endTs ? new Date(m.endTs).toLocaleDateString() : '—'}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  )}

                  {/* Active Program Enrollments with progress */}
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>Active Program Enrollments</Typography>
                  {!activeEnrollments.length ? (
                    <Typography variant="body2" sx={{ mb: 2 }}>No active program enrollments.</Typography>
                  ) : (
                    <Stack spacing={1.5} sx={{ mb: 2 }}>
                      {activeEnrollments.map((e, idx) => {
                        const percent = pct(e.sessionsPurchased, e.sessionsRemaining);
                        return (
                          <Paper key={`${e.enrollmentId ?? e.programId}-active-${idx}`} variant="outlined" sx={{ p: 1.5 }}>
                            <Stack spacing={0.5}>
                              <Typography variant="body2" sx={{ fontWeight: 700 }}>
                                {e.programTitle ?? `Program #${e.programId}`} {e.packageName ? `• ${e.packageName}` : ''}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                {e.sessionsPurchased ?? 0} purchased • {Math.max(0, (e.sessionsRemaining ?? 0))} remaining
                              </Typography>
                              <LinearProgress variant="determinate" value={percent} />
                            </Stack>
                          </Paper>
                        );
                      })}
                    </Stack>
                  )}

                  {/* Past Programs */}
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>Past Programs</Typography>
                  {!pastEnrollments.length ? (
                    <Typography variant="body2">No past program enrollments.</Typography>
                  ) : (
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Program</TableCell>
                          <TableCell>Status</TableCell>
                          <TableCell>Purchased</TableCell>
                          <TableCell>Remaining</TableCell>
                          <TableCell>Start</TableCell>
                          <TableCell>End</TableCell>
                          <TableCell>Last Attendance</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {pastEnrollments.map((e, i) => (
                          <TableRow key={`${e.enrollmentId ?? e.programId}-past-${i}`}>
                            <TableCell>{e.programTitle ?? `Program #${e.programId}`}</TableCell>
                            <TableCell>
                              <Chip size="small" label={e.status ?? '—'} color={e.status === 'EXHAUSTED' ? 'warning' : 'default'} />
                            </TableCell>
                            <TableCell>{e.sessionsPurchased ?? '—'}</TableCell>
                            <TableCell>{e.sessionsRemaining ?? '—'}</TableCell>
                            <TableCell>{e.startTs ? new Date(e.startTs).toLocaleDateString() : '—'}</TableCell>
                            <TableCell>{e.endTs ? new Date(e.endTs).toLocaleDateString() : '—'}</TableCell>
                            <TableCell>{e.lastAttendedAt ? new Date(e.lastAttendedAt).toLocaleDateString() : '—'}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  )}
                </Paper>
              </>
            )}
          </>
        )}
      </Container>

      <Snackbar
        open={!!ok}
        autoHideDuration={3000}
        onClose={() => setOk(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" onClose={() => setOk(null)} sx={{ width: '100%' }}>
          {ok}
        </Alert>
      </Snackbar>
    </Box>
  );
}
