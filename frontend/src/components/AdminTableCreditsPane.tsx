import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TablePagination,
  TextField,
  Tooltip,
  Typography,
  CircularProgress,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import SearchIcon from '@mui/icons-material/Search';
import {
  adminSearchUsersWithCredits,
  adminConsumeTableHours,
  adminManualGrantTableHours,
  type AdminCreditUserRow,
  type Paged,
} from '../lib/booking';

type PageData = Paged<AdminCreditUserRow> | null;

function fmtDate(iso?: string | null) {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '—';
    return new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(d);
  } catch {
    return '—';
  }
}

export default function AdminTableCreditsPane() {
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage] = useState(10); // fixed by requirement
  const [data, setData] = useState<PageData>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [consuming, setConsuming] = useState<number | null>(null); // userId while consuming
  const [granting, setGranting]   = useState<number | null>(null); // userId while granting
  const [amounts, setAmounts] = useState<Record<number, string>>({}); // userId -> input value

  const total = data?.totalElements ?? 0;
  const rows = data?.content ?? [];

  const fetchPage = async (p = page, query = q) => {
    setLoading(true);
    setErr(null);
    try {
      const res = await adminSearchUsersWithCredits(query, p, rowsPerPage);
      setData(res ?? null);
    } catch (e: any) {
      setErr(e?.message ?? 'Failed to load credits.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPage(0, '');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onSearch = () => {
    setPage(0);
    fetchPage(0, q);
  };

  const handleChangePage = (_: any, newPage: number) => {
    setPage(newPage);
    fetchPage(newPage, q);
  };

  const setAmount = (userId: number, v: string) => {
    setAmounts(prev => ({ ...prev, [userId]: v }));
  };

  const getAmount = (userId: number) => {
    const raw = amounts[userId];
    return raw == null || raw === '' ? '1' : raw; // default 1.0 hr
  };

  const validateHalfHour = (hours: number) => {
    if (!isFinite(hours) || hours <= 0) return 'Please enter a valid number of hours (> 0).';
    const scaled = Math.round(hours * 2);
    if (Math.abs(scaled - hours * 2) > 1e-9) return 'Hours must be in 0.5 increments.';
    return null;
  };

  const consume = async (row: AdminCreditUserRow) => {
    const raw = getAmount(row.id);
    const hours = Number(raw);

    const errMsg = validateHalfHour(hours);
    if (errMsg) { alert(errMsg); return; }

    // Backend guard: max 2.5 hours per operation.
    if (hours > 2.5) {
      alert('You can consume at most 2.5 hours per operation.');
      return;
    }
    if (hours > row.tableHoursRemaining) {
      if (!confirm("This exceeds the user's remaining hours. Continue?")) return;
    }
    if (!confirm(`Consume ${hours} hour(s) for ${row.name ?? row.email}?`)) return;

    try {
      setConsuming(row.id);
      setErr(null);
      const resp = await adminConsumeTableHours({ userId: row.id, hours });
      const remaining = (resp as any)?.remaining ?? (resp as any)?.hoursRemaining;
      setData(prev => {
        if (!prev) return prev;
        const content = prev.content.map(u =>
          u.id === row.id
            ? { ...u, tableHoursRemaining: typeof remaining === 'number' ? remaining : u.tableHoursRemaining }
            : u
        );
        return { ...prev, content };
      });
    } catch (e: any) {
      setErr(e?.message ?? 'Failed to consume hours.');
    } finally {
      setConsuming(null);
    }
  };

  const grant = async (row: AdminCreditUserRow) => {
    const raw = getAmount(row.id);
    const hours = Number(raw);

    const errMsg = validateHalfHour(hours);
    if (errMsg) { alert(errMsg); return; }

    if (hours > 24) {
      if (!confirm('Grant more than 24 hours at once?')) return;
    }
    if (!confirm(`Grant ${hours} hour(s) to ${row.name ?? row.email}?`)) return;

    try {
      setGranting(row.id);
      setErr(null);
      await adminManualGrantTableHours({ userId: row.id, hours });
      // optimistic bump
      setData(prev => {
        if (!prev) return prev;
        const content = prev.content.map(u =>
          u.id === row.id ? { ...u, tableHoursRemaining: (u.tableHoursRemaining ?? 0) + hours } : u
        );
        return { ...prev, content };
      });
    } catch (e: any) {
      setErr(e?.message ?? 'Failed to grant hours.');
    } finally {
      setGranting(null);
    }
  };

  const busy = loading && !data;

  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 700, flex: 1 }}>
          Manage Table Rental Credits
        </Typography>
        <TextField
          size="small"
          placeholder="Search by name or email"
          value={q}
          onChange={e => setQ(e.target.value)}
          onKeyDown={e => { if (e.key === 'Enter') onSearch(); }}
          InputProps={{ endAdornment: <IconButton onClick={onSearch}><SearchIcon /></IconButton> }}
          sx={{ width: { xs: '100%', sm: 360 } }}
        />
        <Tooltip title="Reload">
          <span>
            <IconButton onClick={() => fetchPage(page, q)} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </span>
        </Tooltip>
      </Stack>

      {err && <Alert sx={{ mt: 2 }} severity="error">{err}</Alert>}

      {busy ? (
        <Box sx={{ textAlign: 'center', py: 4 }}><CircularProgress /></Box>
      ) : (
        <>
          <Box sx={{ overflowX: 'auto', mt: 2 }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell align="right">Hours Remaining</TableCell>
                  <TableCell>Last Used / Attended</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map(row => (
                  <TableRow key={row.id} hover>
                    <TableCell>{row.name ?? '—'}</TableCell>
                    <TableCell>{row.email}</TableCell>
                    <TableCell align="right">{row.tableHoursRemaining.toFixed(2)}</TableCell>
                    <TableCell>{fmtDate((row as any).lastUsedAt)}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end" alignItems="center">
                        <TextField
                          size="small"
                          type="number"
                          inputProps={{ step: '0.5', min: '0.5', max: '2.5' }}
                          value={getAmount(row.id)}
                          onChange={e => setAmount(row.id, e.target.value)}
                          sx={{ width: 96 }}
                        />
                        <Button
                          variant="outlined"
                          onClick={() => grant(row)}
                          disabled={granting === row.id}
                        >
                          {granting === row.id ? 'Granting…' : 'Grant'}
                        </Button>
                        <Button
                          variant="contained"
                          onClick={() => consume(row)}
                          disabled={consuming === row.id}
                        >
                          {consuming === row.id ? 'Consuming…' : 'Consume'}
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
                {rows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
                        No users found.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Box>

          <TablePagination
            component="div"
            count={total}
            page={page}
            onPageChange={handleChangePage}
            rowsPerPage={rowsPerPage}
            rowsPerPageOptions={[10]}
          />
        </>
      )}
    </Paper>
  );
}
