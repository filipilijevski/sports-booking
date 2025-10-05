import { useEffect, useMemo, useState } from 'react';
import {
  Box, Container, Paper, Stack, Typography, TextField, Button, Table, TableHead,
  TableRow, TableCell, TableBody, Pagination, Dialog, DialogTitle, DialogContent,
  DialogActions, CircularProgress, Alert, Tabs, Tab, FormControl, InputLabel, Select,
  MenuItem, Chip, Divider, TableContainer
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';

import {
  adminSearchUsers,
  adminListPrograms,
  adminListPlans,
  adminManualMembership,
  adminManualEnrollment,
  type AdminUserPage,
  type ProgramDto,
  type MembershipPlanDto,
  type AdminUserRow,
} from '../lib/booking';
import { useRole } from '../context/RoleContext';

const BG = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

export default function AdminEnrollUsers() {
  const { role } = useRole();
  const authedAdmin = role === 'OWNER' || role === 'ADMIN';

  const [q, setQ] = useState('');
  const [page, setPage] = useState(0); // zero-based
  const [data, setData] = useState<AdminUserPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const pull = async (opts?: { q?: string; page?: number }) => {
    setLoading(true); setErr(null);
    try {
      const res = await adminSearchUsers(opts?.q ?? q, opts?.page ?? page, 10);
      setData(res ?? { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
    } catch (e: any) {
      setErr(e?.message ?? 'Failed to load users');
      // Keep previous data so pagination remains usable if we have it
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { pull(); /* eslint-disable-line react-hooks/exhaustive-deps */ }, []);

  const doSearch = () => { setPage(0); pull({ q, page: 0 }); };

  // If not authed, short-circuit with a friendly gate
  if (!authedAdmin) {
    return (
      <Box sx={{ minHeight: '100dvh', background: BG, display: 'flex', alignItems: 'center' }}>
        <Container>
          <Paper sx={{ p: 4, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
            <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>Access denied</Typography>
            <Typography>You must be signed in with an ADMIN or OWNER account to view this page.</Typography>
          </Paper>
        </Container>
      </Box>
    );
  }

  const hasAnyResults = !!data && data.totalElements > 0;
  const pageEmpty = !!data && data.totalElements > 0 && data.content.length === 0;

  return (
    <Box sx={{ width:'100%', minHeight:'100vh', background:BG, py:6 }}>
      <Container maxWidth="lg">
        {/* Header / search */}
        <Typography variant="h3" align="center" sx={{ fontWeight: 700, color: 'common.white', py: 3 }}>
          Program and Membership Enrollment
        </Typography>
        <Paper sx={{ p:2, borderRadius:3, mb:2, background:'rgba(255,255,255,0.9)' }}>
          <Stack direction={{ xs:'column', sm:'row' }} spacing={2} alignItems={{ sm:'center' }}>
            <Typography variant="h5" sx={{ fontWeight:700, flex:1 }}>Enroll Users</Typography>
            <TextField
              size="small"
              placeholder="Search by name or email…"
              value={q}
              onChange={e=>setQ(e.target.value)}
              sx={{ flex: 2 }}
              onKeyDown={(e) => { if (e.key === 'Enter') doSearch(); }}
            />
            <Button variant="contained" onClick={doSearch}>Search</Button>
          </Stack>
        </Paper>

        {/* Error banner (kept separate from table so pagination still visible if we have data) */}
        {err && (
          <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
            <Alert severity="error">{err}</Alert>
          </Paper>
        )}

        {/* Main list */}
        {loading ? (
          <Box sx={{ textAlign:'center', py:6 }}><CircularProgress /></Box>
        ) : (
          <>
            {/* Table or Empty state (but we still show pagination below) */}
            {(!data || (!hasAnyResults && data.content.length === 0)) ? (
              <Paper sx={{ p:3, borderRadius:3, background:'rgba(255,255,255,0.95)' }}>
                <Typography>No users match your search.</Typography>
              </Paper>
            ) : pageEmpty ? (
              <Paper sx={{ p:3, borderRadius:3, background:'rgba(255,255,255,0.95)' }}>
                <Typography>This page has no users. Use the pagination below to navigate.</Typography>
              </Paper>
            ) : (
              <Paper
                sx={{
                  p:2,
                  borderRadius:3,
                  background:'rgba(255,255,255,0.95)',
                  overflow: 'hidden',              // prevent spill outside the card
                }}
              >
                <TableContainer sx={{ maxWidth: '100%', overflowX: 'auto' }}>
                  <Table
                    size="small"
                    stickyHeader
                    sx={{
                      tableLayout: 'fixed',       
                      minWidth: 920,               
                    }}
                  >
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight:700, width: 180 }}>User</TableCell>
                        <TableCell sx={{ fontWeight:700, width: 240 }}>Email</TableCell>
                        <TableCell align="center" sx={{ fontWeight:700, width: 140 }}>Active Initial</TableCell>
                        <TableCell align="center" sx={{ fontWeight:700, width: 160 }}>Active Memberships</TableCell>
                        <TableCell align="center" sx={{ fontWeight:700, width: 180 }}>Active Enrollments</TableCell>
                        <TableCell align="center" sx={{ fontWeight:700, width: 160 }}>Lessons Remaining</TableCell>
                        <TableCell align="center" sx={{ fontWeight:700, width: 140 }}>Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {data!.content.map((u: AdminUserRow) => (
                        <EnrollRow key={u.id} row={u} onEnrolled={() => pull()} />
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Paper>
            )}

            {/* Always show pagination if we have page metadata */}
            {data && (
              <Box sx={{ display:'flex', justifyContent:'center', mt:3 }}>
                <Pagination
                  page={page + 1}
                  count={Math.max(1, data.totalPages || 1)}
                  onChange={(_, p1) => { setPage(p1 - 1); pull({ page: p1 - 1 }); }}
                  color="primary"
                />
              </Box>
            )}
          </>
        )}
      </Container>
    </Box>
  );
}

function EnrollRow({ row, onEnrolled }: { row: AdminUserRow, onEnrolled: () => void }) {
  const [openEnroll, setOpenEnroll] = useState(false);
  const [openDetails, setOpenDetails] = useState(false);

  const displayName = row.name || row.email || `User #${row.id}`;

  return (
    <>
      <TableRow
        hover
        role="button"
        tabIndex={0}
        onClick={() => setOpenDetails(true)}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setOpenDetails(true); }}
        sx={{ cursor: 'pointer' }}
      >
        <TableCell sx={{ verticalAlign: 'top', whiteSpace: 'normal' }}>
          <Typography sx={{ fontWeight: 600 }}>{displayName}</Typography>
        </TableCell>

        <TableCell
          sx={{
            verticalAlign: 'top',
            whiteSpace: 'normal',
            wordBreak: 'break-all', // long emails never burst the layout
          }}
        >
          {row.email}
        </TableCell>

        <TableCell align="center" sx={{ verticalAlign: 'top' }}>
          {row.hasActiveInitial ? <Chip label="Yes" color="success" size="small" /> : <Chip label="No" size="small" />}
        </TableCell>

        {/* Totals only (no lists) */}
        <TableCell align="center" sx={{ verticalAlign: 'top' }}>
          {row.activeMemberships > 0
            ? <Chip size="small" variant="outlined" label={row.activeMemberships} />
            : <Typography variant="body2" color="text.secondary"><em>None</em></Typography>}
        </TableCell>

        <TableCell align="center" sx={{ verticalAlign: 'top' }}>
          {row.activeEnrollments > 0
            ? <Chip size="small" variant="outlined" label={row.activeEnrollments} />
            : <Typography variant="body2" color="text.secondary"><em>None</em></Typography>}
        </TableCell>

        <TableCell align="center" sx={{ verticalAlign: 'top' }}>
          {typeof row.lessonsRemaining === 'number' ? row.lessonsRemaining : 0}
        </TableCell>

        <TableCell align="right" sx={{ verticalAlign: 'top' }}>
          <Button
            size="small"
            variant="contained"
            startIcon={<AddIcon />}
            onClick={(e) => { e.stopPropagation(); setOpenEnroll(true); }}
          >
            Enroll
          </Button>
        </TableCell>
      </TableRow>

      {openEnroll && (
        <EnrollDialog
          userId={row.id}
          userLabel={displayName}
          onClose={() => setOpenEnroll(false)}
          onSuccess={() => { setOpenEnroll(false); onEnrolled(); }}
        />
      )}

      {openDetails && (
        <UserDetailsDialog
          row={row}
          onClose={() => setOpenDetails(false)}
          onRequestEnroll={() => { setOpenDetails(false); setOpenEnroll(true); }}
        />
      )}
    </>
  );
}

function EnrollDialog({
  userId, userLabel, onClose, onSuccess,
}: {
  userId: number; userLabel: string; onClose: () => void; onSuccess: () => void;
}) {
  const [tab, setTab] = useState<'membership'|'program'>('membership');

  const [plans, setPlans] = useState<MembershipPlanDto[]>([]);
  const [programs, setPrograms] = useState<ProgramDto[]>([]);
  const [planId, setPlanId] = useState<number | ''>('');
  const [programId, setProgramId] = useState<number | ''>('');
  const [packageId, setPackageId] = useState<number | ''>('');

  const [paymentRef, setPaymentRef] = useState('');
  const [paidAt, setPaidAt] = useState<string>(() => new Date().toISOString().slice(0,16)); // yyyy-MM-ddTHH:mm (local UI)
  const [notes, setNotes] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    (async () => {
      setLoading(true); setErr(null);
      try {
        const [p, g] = await Promise.all([adminListPlans(), adminListPrograms()]);
        if (!alive) return;
        setPlans((p ?? []).filter(x => x.active));
        setPrograms((g ?? []).filter(x => x.active));
      } catch (e: any) {
        setErr(e?.message ?? 'Failed to load options');
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => { alive = false; };
  }, []);

  const activePackages = useMemo(() => {
    if (!programId || !programs.length) return [];
    const pr = programs.find(p => p.id === programId);
    return (pr?.packages ?? []).filter(pk => pk.active);
  }, [programId, programs]);

  const friendlyError = (e: any): string => {
    const code = e?.code?.toString() ?? '';
    const msg  = (e?.message ?? '').toString();

    if (code === 'DUPLICATE_ENROLLMENT' || /Active enrollment already exists/i.test(msg)) {
      return 'User already has an active enrollment for this program.';
    }
    if (/Initial annual club membership is required/i.test(msg)) {
      return 'An active initial (annual) membership is required for special plans or new enrollments.';
    }
    if (/already have an active initial membership/i.test(msg)) {
      return 'User already has an active initial membership.';
    }
    if (/active membership for this plan already exists/i.test(msg)) {
      return 'User already has this membership active.';
    }
    if (/Only clients can be enrolled/i.test(msg)) {
      return 'Only client accounts can be enrolled via this tool.';
    }
    return msg || 'Operation failed.';
  };

  const submit = async () => {
    setSaving(true); setErr(null); setOk(null);
    try {
      const paidIso = (paidAt && paidAt.length >= 16)
        ? new Date(paidAt).toISOString()
        : undefined;

      if (tab === 'membership') {
        if (!planId) throw new Error('Select a membership plan.');
        const res = await adminManualMembership({
          userId, planId: Number(planId),
          paymentRef: paymentRef.trim() || undefined,
          paidAt: paidIso,
          notes: notes.trim() || undefined,
        });
        if (!res?.ok) throw new Error('Server declined to create membership.');
        setOk('Membership created and marked as paid.');
      } else {
        if (!programId || !packageId) throw new Error('Select a program and a package.');
        const res = await adminManualEnrollment({
          userId,
          programPackageId: Number(packageId),
          paymentRef: paymentRef.trim() || undefined,
          paidAt: paidIso,
          notes: notes.trim() || undefined,
        });
        if (!res?.ok) throw new Error('Server declined to create enrollment.');
        setOk('Program enrollment created and marked as paid.');
      }
      onSuccess();
    } catch (e: any) {
      setErr(friendlyError(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ fontWeight: 700 }}>Manually Enroll {userLabel}</DialogTitle>
      <DialogContent dividers>
        {loading ? (
          <Box sx={{ textAlign: 'center', py: 4 }}><CircularProgress /></Box>
        ) : (
          <Stack spacing={2}>
            {err && <Alert severity="error">{err}</Alert>}
            {ok  && <Alert severity="success">{ok}</Alert>}

            <Tabs value={tab} onChange={(_,v)=>setTab(v)}>
              <Tab value="membership" label="Membership" />
              <Tab value="program"    label="Program + Package" />
            </Tabs>

            {tab === 'membership' ? (
              <FormControl fullWidth>
                <InputLabel>Membership Plan</InputLabel>
                <Select
                  label="Membership Plan"
                  value={planId}
                  onChange={e=>setPlanId(e.target.value as number | '')}
                >
                  {plans.length === 0
                    ? <MenuItem value=""><em>No active plans</em></MenuItem>
                    : plans.map(p => (
                        <MenuItem key={p.id} value={p.id}>
                          {p.name} — ${p.priceCad?.toFixed(2)} / {p.durationDays} days
                        </MenuItem>
                      ))}
                </Select>
              </FormControl>
            ) : (
              <Stack direction={{ xs:'column', md:'row' }} spacing={2}>
                <FormControl fullWidth>
                  <InputLabel>Program</InputLabel>
                  <Select
                    label="Program"
                    value={programId}
                    onChange={e => { setProgramId(e.target.value as number | ''); setPackageId(''); }}
                  >
                    {programs.length === 0
                      ? <MenuItem value=""><em>No active programs</em></MenuItem>
                      : programs.map(p => (
                          <MenuItem key={p.id} value={p.id}>
                            {p.title}
                          </MenuItem>
                        ))}
                  </Select>
                </FormControl>

                <FormControl fullWidth>
                  <InputLabel>Package</InputLabel>
                  <Select
                    label="Package"
                    value={packageId}
                    onChange={e => setPackageId(e.target.value as number | '')}
                    disabled={!programId}
                  >
                    {!programId
                      ? <MenuItem value=""><em>Select a program</em></MenuItem>
                      : (activePackages.length === 0
                          ? <MenuItem value=""><em>No active packages</em></MenuItem>
                          : activePackages.map(pk => (
                              <MenuItem key={pk.id} value={pk.id}>
                                {pk.name} — {pk.sessionsCount} sessions — ${pk.priceCad?.toFixed(2)}
                              </MenuItem>
                            )))}
                  </Select>
                </FormControl>
              </Stack>
            )}

            <Stack direction={{ xs:'column', md:'row' }} spacing={2}>
              <TextField
                label="Payment Reference (optional)"
                value={paymentRef}
                onChange={e=>setPaymentRef(e.target.value)}
                fullWidth
              />
              <TextField
                label="Paid At (local)"
                type="datetime-local"
                value={paidAt}
                onChange={e=>setPaidAt(e.target.value)}
                fullWidth
              />
            </Stack>

            <TextField
              label="Admin notes (optional)"
              value={notes}
              onChange={e=>setNotes(e.target.value)}
              fullWidth
              multiline
              minRows={2}
            />
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        <Button variant="contained" disabled={saving || loading} onClick={submit}>
          {saving ? <CircularProgress size={18}/> : 'Enroll'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function UserDetailsDialog({
  row,
  onClose,
  onRequestEnroll,
}: {
  row: AdminUserRow;
  onClose: () => void;
  onRequestEnroll: () => void;
}) {
  const enrollments = (row.enrollments ?? (row as any).enrollmentsList ?? []) as any[];
  const displayName = row.name || row.email || `User #${row.id}`;

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle fontWeight={600}>{displayName}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2}>
          <Stack spacing={1}>
            <Typography variant="subtitle1" fontWeight={600}>Active memberships: </Typography>
            {row.memberships?.length ? (
              <Stack spacing={1}>
                {row.memberships.map(m => (
                  <Typography key={m.userMembershipId} variant="body2">
                    {m.planName ?? `Plan #${m.planId}`}
                    {m.endTs ? ` — ends ${new Date(m.endTs).toLocaleDateString()}` : ''}
                  </Typography>
                ))}
              </Stack>
            ) : (
              <Typography variant="body2" color="text.secondary"><em>None</em></Typography>
            )}
          </Stack>

          <Stack spacing={0.5}>
            <Typography variant="subtitle1" fontWeight={600}>Active enrollments: </Typography>
            {enrollments?.length ? (
              <Stack spacing={1}>
                {enrollments.map((e: any) => (
                  <Typography key={e.enrollmentId} variant="body2">
                    {e.programTitle ?? 'Program'}
                    {e.packageName ? ` — ${e.packageName}` : ''}
                    {typeof e.sessionsRemaining === 'number' ? ` (${e.sessionsRemaining} sessions remaining)` : ''}
                  </Typography>
                ))}
              </Stack>
            ) : (
              <Typography variant="body2" color="text.secondary"><em>None</em></Typography>
            )}
          </Stack>

          <Divider />
          <Stack direction="row" justifyContent="start">
            <Typography variant="body2" fontWeight={600}>Total Lessons Remaining : &nbsp;</Typography>
            <Typography variant="body2" fontWeight={600}>
              {typeof row.lessonsRemaining === 'number' ? row.lessonsRemaining : 0}
            </Typography>
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={onRequestEnroll}
        >
          Enroll
        </Button>
      </DialogActions>
    </Dialog>
  );
}
