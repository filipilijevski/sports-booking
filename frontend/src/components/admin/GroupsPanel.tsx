import { useEffect, useState } from 'react';
import {
  Paper, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Box, CircularProgress, Alert, Stack, Typography,
  Dialog, DialogTitle, DialogContent, DialogActions, Divider, Chip
} from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {
  adminListGroups,
  adminGetGroup,
  adminGetGroupAudit,
  type GroupListItem,
  type GroupDetail,
  type GroupAuditEvent,
} from '../../lib/booking';

export default function GroupsPanel() {
  const [rows, setRows] = useState<GroupListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const [selected, setSelected] = useState<GroupDetail | null>(null);
  const [audit, setAudit] = useState<GroupAuditEvent[]>([]);
  const [busySel, setBusySel] = useState(false);

  const pull = async () => {
    setLoading(true); setErr(null);
    try {
      const data = await adminListGroups();
      setRows(data ?? []);
    } catch (e: any) {
      setErr(e?.message ?? 'Failed to load groups.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { pull(); }, []);

  const openDetail = async (id: number) => {
    setBusySel(true); setErr(null);
    try {
      const [dRes, aRes] = await Promise.allSettled([
        adminGetGroup(id),
        adminGetGroupAudit(id),
      ]);
      if (dRes.status === 'fulfilled') setSelected(dRes.value ?? null);
      else throw new Error(dRes.reason?.message ?? 'Failed to load group detail.');

      // audit is optional; fail-open to empty list
      if (aRes.status === 'fulfilled') setAudit(aRes.value ?? []);
      else setAudit([]);
    } catch (e: any) {
      setErr(e?.message ?? 'Failed to load group detail.');
    } finally {
      setBusySel(false);
    }
  };

  return (
    <>
      {err && (
        <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
          <Alert severity="error">{err}</Alert>
        </Paper>
      )}

      {loading ? (
        <Box sx={{ textAlign:'center', py:6 }}><CircularProgress /></Box>
      ) : rows.length === 0 ? (
        <Paper sx={{ p:3, borderRadius:3, background:'rgba(255,255,255,0.95)' }}>
          <Typography>No membership groups yet.</Typography>
        </Paper>
      ) : (
        <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Plan</TableCell>
                <TableCell>Holder</TableCell>
                <TableCell>Owner</TableCell>
                <TableCell>Active</TableCell>
                <TableCell>Members</TableCell>
                <TableCell>Remaining</TableCell>
                <TableCell align="right">Detail</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map(r => {
                const rem = r.entitlements;
                const remStr = [
                  rem?.tableHoursRemaining != null ? `${rem.tableHoursRemaining} hrs` : null,
                  rem?.programCreditsRemaining != null ? `${rem.programCreditsRemaining} credits` : null,
                  rem?.tournamentEntriesRemaining != null ? `${rem.tournamentEntriesRemaining} entries` : null,
                ].filter(Boolean).join(' • ') || '—';

                return (
                  <TableRow key={r.id}>
                    <TableCell>#{r.id}</TableCell>
                    <TableCell>{r.planName} ({r.planType})</TableCell>
                    <TableCell>{r.holderKind}</TableCell>
                    <TableCell>
                      <Stack spacing={0}>
                        <Typography variant="body2" sx={{ fontWeight:700 }}>{r.ownerName}</Typography>
                        <Typography variant="caption" color="text.secondary">{r.ownerEmail}</Typography>
                      </Stack>
                    </TableCell>
                    <TableCell>{r.active ? 'Yes' : 'No'}</TableCell>
                    <TableCell>{r.membersCount}</TableCell>
                    <TableCell>{remStr}</TableCell>
                    <TableCell align="right">
                      <Button size="small" variant="outlined" onClick={() => openDetail(r.id)} disabled={busySel}>
                        View
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </Paper>
      )}

      <Dialog open={!!selected} onClose={() => setSelected(null)} maxWidth="md" fullWidth>
        <DialogTitle>Group Detail</DialogTitle>
        <DialogContent dividers>
          {!selected ? null : (
            <Stack spacing={2}>
              <Stack direction="row" spacing={1} alignItems="center">
                <InfoOutlinedIcon />
                <Typography variant="h6" sx={{ fontWeight:800 }}>
                  {selected.planName} ({selected.planType}) • Holder: {selected.holderKind}
                </Typography>
              </Stack>
              <Typography variant="body2">
                Group #{selected.id} • Owner: {selected.ownerName} ({selected.ownerEmail})
              </Typography>
              <Typography variant="body2">
                Period: {new Date(selected.startTs).toLocaleString()} → {new Date(selected.endTs).toLocaleString()}
              </Typography>

              <Divider />

              <Typography variant="subtitle2">Entitlements remaining</Typography>
              <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                <Chip label={`Table Hours: ${selected.entitlements?.tableHoursRemaining ?? '—'}`} />
                <Chip label={`Program Credits: ${selected.entitlements?.programCreditsRemaining ?? '—'}`} />
                <Chip label={`Tournament Entries: ${selected.entitlements?.tournamentEntriesRemaining ?? '—'}`} />
              </Stack>

              <Divider />

              <Typography variant="subtitle2">Members ({selected.membersCount})</Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>User</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Active</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {selected.members.map(m => (
                    <TableRow key={m.userMembershipId}>
                      <TableCell>{m.name}</TableCell>
                      <TableCell>{m.email}</TableCell>
                      <TableCell>{m.active ? 'Yes' : 'No'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              <Divider />

              <Typography variant="subtitle2">Audit Log (most recent first)</Typography>
              {audit.length === 0 ? (
                <Alert severity="info">No audit events available.</Alert>
              ) : (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>When</TableCell>
                      <TableCell>Kind</TableCell>
                      <TableCell>Delta</TableCell>
                      <TableCell>Remaining</TableCell>
                      <TableCell>Member</TableCell>
                      <TableCell>Actor</TableCell>
                      <TableCell>Note</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {audit
                      .slice()
                      .sort((a,b) => new Date(b.ts).getTime() - new Date(a.ts).getTime())
                      .map((ev, idx) => (
                        <TableRow key={ev.id ?? idx}>
                          <TableCell>{new Date(ev.ts).toLocaleString()}</TableCell>
                          <TableCell>{ev.kind}</TableCell>
                          <TableCell>{ev.delta > 0 ? `+${ev.delta}` : ev.delta}</TableCell>
                          <TableCell>{ev.remaining ?? '—'}</TableCell>
                          <TableCell>{ev.memberName ?? '—'}</TableCell>
                          <TableCell>{ev.actorName ?? '—'}</TableCell>
                          <TableCell>{ev.note ?? '—'}</TableCell>
                        </TableRow>
                      ))}
                  </TableBody>
                </Table>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelected(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
