import {
  Box, Paper, Stack, Typography, Button, IconButton, Chip, Tooltip,
  Table, TableHead, TableRow, TableCell, TableBody, Divider, Dialog,
  DialogTitle, DialogContent, DialogActions, TextField, Switch,
  FormControlLabel, InputAdornment,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { useEffect, useMemo, useState } from 'react';
import {
  type Coupon, type CouponType, fetchCoupons, createCoupon, updateCoupon, deleteCoupon,
  formatCouponValue, isEffectivelyActive,
} from '../lib/coupons';

/* helpers */
function toLocalInput(iso?: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  const tzOffset = d.getTimezoneOffset() * 60000;
  return new Date(d.getTime() - tzOffset).toISOString().slice(0, 16);
}
function fromLocalInput(local: string): string | null {
  if (!local) return null;
  // treat as local time, convert to ISO
  const d = new Date(local);
  return d.toISOString();
}

function DetailsDialog({ c, open, onClose }: { c: Coupon | null; open: boolean; onClose: () => void }) {
  if (!c) return null;
  const nowActive = isEffectivelyActive(c);
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Coupon: {c.code}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={1.2}>
          <Typography variant="body2"><b>Value:</b> {formatCouponValue(c)}</Typography>
          <Typography variant="body2"><b>Status:</b> {nowActive ? 'Active' : 'Inactive / Not in window'}</Typography>
          <Typography variant="body2"><b>Start:</b> {c.startAt ? new Date(c.startAt).toLocaleString() : '—'}</Typography>
          <Typography variant="body2"><b>End:</b> {c.endAt ? new Date(c.endAt).toLocaleString() : '—'}</Typography>
          <Typography variant="body2"><b>Min spend:</b> {c.minSpend != null ? `$${c.minSpend.toFixed(2)}` : '—'}</Typography>
          <Divider sx={{ my: 1 }} />
          <Typography variant="body2"><b>Created:</b> {c.createdAt ? new Date(c.createdAt).toLocaleString() : '—'}</Typography>
          <Typography variant="body2"><b>Last updated:</b> {c.updatedAt ? new Date(c.updatedAt).toLocaleString() : '—'}</Typography>
        </Stack>
      </DialogContent>
      <DialogActions><Button onClick={onClose}>Close</Button></DialogActions>
    </Dialog>
  );
}

function UpsertDialog({
  open, initial, onClose, onSaved,
}: {
  open: boolean;
  initial: Coupon | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [code, setCode]        = useState('');
  const [type, setType]        = useState<CouponType>('AMOUNT');
  const [value, setValue]      = useState<string>('0');
  const [active, setActive]    = useState(true);
  const [minSpend, setMin]     = useState<string>('');
  const [startAt, setStartAt]  = useState<string>('');   // datetime-local
  const [endAt, setEndAt]      = useState<string>('');   // datetime-local
  const [saving, setSaving]    = useState(false);
  const [err, setErr]          = useState<string | null>(null);

  useEffect(() => {
    setErr(null);
    if (initial) {
      setCode(initial.code || '');
      setType(initial.type || 'AMOUNT');
      setValue((initial.value ?? 0).toString());
      setActive(Boolean(initial.active));
      setMin(initial.minSpend != null ? String(initial.minSpend) : '');
      setStartAt(toLocalInput(initial.startAt));
      setEndAt(toLocalInput(initial.endAt));
    } else {
      // defaults for a brand-new coupon
      setCode('');
      setType('AMOUNT');
      setValue('0');
      setActive(true);
      setMin('');
      const nowLocal = toLocalInput(new Date().toISOString());
      setStartAt(nowLocal);     // start defaults to "now"
      setEndAt('');
    }
  }, [initial, open]);

  const willBeEffective = useMemo(() => {
    const serverStart = fromLocalInput(startAt ?? '');
    const serverEnd   = fromLocalInput(endAt ?? '');
    const c: Coupon = {
      id: initial?.id ?? 0,
      code, type, value: parseFloat(value || '0'), active,
      startAt: serverStart ?? undefined,
      endAt: serverEnd ?? undefined,
      minSpend: minSpend ? parseFloat(minSpend) : null,
    };
    return isEffectivelyActive(c);
  }, [code, type, value, active, minSpend, startAt, endAt, initial?.id]);

  const onSave = async () => {
    setSaving(true); setErr(null);
    try {
      const payload: Partial<Coupon> = {
        code: code.trim().toUpperCase(),
        type,
        value: parseFloat(value || '0') || 0,
        active,
        minSpend: minSpend ? parseFloat(minSpend) : null,
        startAt: fromLocalInput(startAt) ?? null,
        endAt: fromLocalInput(endAt) ?? null,
      };

      // Hard client guard: end date in the past => force inactive
      const now = new Date();
      const e = payload.endAt ? new Date(payload.endAt) : null;
      if (e && e < now) payload.active = false;

      if (initial) await updateCoupon(initial.id, payload);
      else         await createCoupon(payload);

      onSaved();
      onClose();
    } catch (e: any) {
      setErr(e?.message || 'Save failed');
    } finally { setSaving(false); }
  };

  const amountAdornment = type === 'AMOUNT'
    ? { startAdornment: <InputAdornment position="start">$</InputAdornment> }
    : { endAdornment: <InputAdornment position="end">%</InputAdornment> };

  return (
    <Dialog open={open} onClose={saving ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>{initial ? 'Edit Coupon' : 'Add Coupon'}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Coupon Code"
            value={code}
            onChange={e => setCode(e.target.value.toUpperCase())}
            inputProps={{ style: { textTransform: 'uppercase' } }}
            helperText="Suggested format: HOLIDAY10, SUMMER25, etc."
            fullWidth
          />
          <Stack direction="row" spacing={2}>
            <TextField
              select
              fullWidth
              label="Type"
              value={type}
              onChange={e => setType(e.target.value as CouponType)}
              SelectProps={{ native: true }}
            >
              <option value="AMOUNT">Fixed amount</option>
              <option value="PERCENT">Percent</option>
            </TextField>
            <TextField
              fullWidth
              label={type === 'AMOUNT' ? 'Amount' : 'Percent'}
              type="number"
              value={value}
              onChange={e => setValue(e.target.value)}
              InputProps={amountAdornment}
            />
          </Stack>

          <FormControlLabel
            control={<Switch checked={active} onChange={e => setActive(e.target.checked)} />}
            label={willBeEffective ? 'Active (effective now)' : 'Active (will not be effective yet)'}
          />

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              label="Start"
              type="datetime-local"
              value={startAt}
              onChange={e => setStartAt(e.target.value)}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <TextField
              label="End (optional)"
              type="datetime-local"
              value={endAt}
              onChange={e => setEndAt(e.target.value)}
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
          </Stack>

          <TextField
            label="Minimum Spend (optional)"
            type="number"
            value={minSpend}
            onChange={e => setMin(e.target.value)}
            InputProps={{ startAdornment: <InputAdornment position="start">$</InputAdornment> }}
            fullWidth
          />

          <Stack direction="row" spacing={1} alignItems="center">
            <InfoOutlinedIcon fontSize="small" color="action" />
            <Typography variant="caption" color="text.secondary">
              If you set an end date already in the past, the coupon will be saved as inactive.
            </Typography>
          </Stack>

          {err && <Typography color="error">{err}</Typography>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={saving}>Cancel</Button>
        <Button variant="contained" onClick={onSave} disabled={saving || !code.trim()}>
          {saving ? 'Saving…' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function AdminCouponsPanel() {
  const [coupons, setCoupons]   = useState<Coupon[]>([]);
  const [loading, setLoading]   = useState(false);
  const [viewing, setViewing]   = useState<Coupon | null>(null);
  const [editing, setEditing]   = useState<Coupon | null>(null);
  const [upsertOpen, setUpsert] = useState(false);
  const [error, setError]       = useState<string | null>(null);

  const refresh = async () => {
    try {
      setLoading(true); setError(null);
      setCoupons(await fetchCoupons());
    } catch (e: any) {
      setError(e?.message || 'Failed to load coupons');
    } finally { setLoading(false); }
  };

  useEffect(() => { refresh(); }, []);

  const onDelete = async (id: number) => {
    if (!confirm('Delete this coupon?')) return;
    await deleteCoupon(id);
    await refresh();
  };

  const effective = (c: Coupon) => isEffectivelyActive(c);

  const now = new Date();
  const rows = useMemo(() => coupons.slice().sort(
    (a, b) => +(b.updatedAt ? new Date(b.updatedAt) : new Date(0)) - +(a.updatedAt ? new Date(a.updatedAt) : new Date(0))
  ), [coupons]);

  return (
    <Paper elevation={3} sx={{ p: 2.5, borderRadius: 3 }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
        <Typography variant="h6">Coupons</Typography>
        <Button startIcon={<AddIcon />} variant="contained" onClick={() => { setEditing(null); setUpsert(true); }}>
          Add Coupon
        </Button>
      </Stack>
      <Divider sx={{ mb: 2 }} />

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      <Box sx={{ overflowX: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Code</TableCell>
              <TableCell>Value</TableCell>
              <TableCell>Active</TableCell>
              <TableCell>Start</TableCell>
              <TableCell>End</TableCell>
              <TableCell>Min Spend</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map(c => (
              <TableRow key={c.id} hover>
                <TableCell onClick={() => setViewing(c)} sx={{ cursor: 'pointer', fontWeight: 600 }}>
                  {c.code}
                </TableCell>
                <TableCell>{formatCouponValue(c)}</TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    label={effective(c) ? 'Active' : 'Inactive'}
                    color={effective(c) ? 'success' : 'default'}
                  />
                </TableCell>
                <TableCell>{c.startAt ? new Date(c.startAt).toLocaleString() : '—'}</TableCell>
                <TableCell
                  sx={{ color: c.endAt && new Date(c.endAt) < now ? 'error.main' : 'inherit' }}
                >
                  {c.endAt ? new Date(c.endAt).toLocaleString() : '—'}
                </TableCell>
                <TableCell>{c.minSpend != null ? `$${c.minSpend.toFixed(2)}` : '—'}</TableCell>
                <TableCell>
                  <Tooltip title="Edit">
                    <span>
                      <IconButton size="small" onClick={() => { setEditing(c); setUpsert(true); }}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title="Delete">
                    <span>
                      <IconButton size="small" onClick={() => onDelete(c.id)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
            {rows.length === 0 && !loading && (
              <TableRow><TableCell colSpan={7}>No coupons yet.</TableCell></TableRow>
            )}
          </TableBody>
        </Table>
      </Box>

      <DetailsDialog c={viewing} open={!!viewing} onClose={() => setViewing(null)} />
      <UpsertDialog
        open={upsertOpen}
        initial={editing}
        onClose={() => setUpsert(false)}
        onSaved={refresh}
      />
    </Paper>
  );
}
