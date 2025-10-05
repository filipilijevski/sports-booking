import { useEffect, useMemo, useState } from 'react';
import {
  Box, Paper, Stack, Typography, Button, IconButton, Alert, CircularProgress, Table, TableHead, TableRow, TableCell, TableBody,
  TextField, Select, MenuItem, Dialog, DialogTitle, DialogContent, DialogActions, Divider
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';

import {
  adminListTableCreditPackages, adminCreateTableCreditPackage, adminUpdateTableCreditPackage, adminDeleteTableCreditPackage,
  type TableRentalPackageDto
} from '../lib/booking';

import AdminTableCreditsPane from '../components/AdminTableCreditsPane';
import GroupsPanel from '../components/admin/GroupsPanel';

export default function AdminTableRentalCredits() {
  return (
    <Box>
      <Header />
      <TableCreditPackagesPanel />
      <Divider sx={{ my: 3 }} />
      <AdminTableCreditsPane />
      <Divider sx={{ my: 3 }} />
      <GroupsPanel />
    </Box>
  );
}

function Header() {
  return (
    <Typography variant="h3" align="center" sx={{ fontWeight: 700, color: 'common.white', py: 3 }}>Table Rental Credits</Typography>
  );
}

function TableCreditPackagesPanel() {
  const [items, setItems] = useState<TableRentalPackageDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);

  const pull = async () => {
    setLoading(true); setErr(null);
    try {
      const res = await adminListTableCreditPackages();
      setItems(res ?? []);
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to load table rental packages.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { pull(); }, []);

  const ordered = useMemo(
    () => [...items].sort((a,b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id),
    [items],
  );

  const swapOrder = async (a: TableRentalPackageDto, b: TableRentalPackageDto) => {
    const ao = a.sortOrder ?? 0;
    const bo = b.sortOrder ?? 0;
    await Promise.all([
      adminUpdateTableCreditPackage(a.id, { sortOrder: bo }),
      adminUpdateTableCreditPackage(b.id, { sortOrder: ao }),
    ]);
    await pull();
  };

  return (
    <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)' }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="h6" sx={{ fontWeight: 700 }}>Table Rental Packages</Typography>
        <Button size="small" variant="contained" startIcon={<AddIcon />} onClick={() => setAddOpen(true)}>
          New Package
        </Button>
      </Stack>

      {err && <Alert severity="error" sx={{ mb:2 }}>{err}</Alert>}
      {loading ? (
        <Box sx={{ textAlign:'center', py: 3 }}><CircularProgress /></Box>
      ) : !items.length ? (
        <Alert severity="info">No packages yet. Create your first table rental package.</Alert>
      ) : (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Order</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Hours</TableCell>
              <TableCell>Price (CAD)</TableCell>
              <TableCell>Active</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {ordered.map((pk, idx) => {
              const prev = idx > 0 ? ordered[idx-1] : null;
              const next = idx < ordered.length - 1 ? ordered[idx+1] : null;
              return (
                <TableCreditPackageRow
                  key={pk.id}
                  pack={pk}
                  canMoveUp={!!prev}
                  canMoveDown={!!next}
                  onMoveUp={() => prev && swapOrder(pk, prev)}
                  onMoveDown={() => next && swapOrder(pk, next)}
                  onChanged={pull}
                />
              );
            })}
          </TableBody>
        </Table>
      )}

      <AddTableCreditPackageDialog
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onSave={async payload => {
          await adminCreateTableCreditPackage(payload);
          setAddOpen(false);
          await pull();
        }}
      />
    </Paper>
  );
}

function TableCreditPackageRow({
  pack, canMoveUp, canMoveDown, onMoveUp, onMoveDown, onChanged,
}: {
  pack: TableRentalPackageDto;
  canMoveUp: boolean;
  canMoveDown: boolean;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onChanged: () => void;
}) {
  const [name, setName] = useState(pack.name);
  const [hours, setHours] = useState(pack.hours);
  const [price, setPrice] = useState(pack.priceCad);
  const [active, setActive] = useState(pack.active);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const save = async () => {
    try {
      setSaving(true); setErr(null);
      if (!name.trim()) throw new Error('Name is required');
      if (!Number.isFinite(hours) || hours <= 0) throw new Error('Hours must be > 0');
      if (!Number.isFinite(price) || price < 0) throw new Error('Price must be ≥ 0');

      await adminUpdateTableCreditPackage(pack.id, {
        name: name.trim(),
        hours,
        priceCad: price,
        active,
      });
      await onChanged();
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const del = async () => {
    if (!confirm('Delete this package?')) return;
    await adminDeleteTableCreditPackage(pack.id);
    await onChanged();
  };

  return (
    <TableRow>
      <TableCell sx={{ whiteSpace: 'nowrap' }}>
        <IconButton size="small" disabled={!canMoveUp} onClick={onMoveUp} aria-label="Move up">
          <ArrowUpwardIcon fontSize="small" />
        </IconButton>
        <IconButton size="small" disabled={!canMoveDown} onClick={onMoveDown} aria-label="Move down">
          <ArrowDownwardIcon fontSize="small" />
        </IconButton>
      </TableCell>
      <TableCell><TextField size="small" value={name} onChange={e=>setName(e.target.value)} /></TableCell>
      <TableCell><TextField size="small" type="number" inputProps={{ step:'0.5', min:'0.5' }} value={hours} onChange={e=>setHours(Number(e.target.value))} /></TableCell>
      <TableCell><TextField size="small" type="number" value={price} onChange={e=>setPrice(Number(e.target.value))} /></TableCell>
      <TableCell>
        <Select size="small" value={String(active)} onChange={e=>setActive(e.target.value==='true')}>
          <MenuItem value="true">Active</MenuItem>
          <MenuItem value="false">Inactive</MenuItem>
        </Select>
      </TableCell>
      <TableCell align="right" sx={{ whiteSpace:'nowrap' }}>
        <Button size="small" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Save'}</Button>
        <IconButton size="small" onClick={del}><DeleteIcon fontSize="small" /></IconButton>
        {err && (
          <Typography variant="caption" color="error" sx={{ display:'block', mt: 0.5 }}>{err}</Typography>
        )}
      </TableCell>
    </TableRow>
  );
}

function AddTableCreditPackageDialog({
  open, onClose, onSave,
}: {
  open: boolean;
  onClose: () => void;
  onSave: (payload: { name: string; hours: number; priceCad: number; active?: boolean; sortOrder?: number | null }) => Promise<void>;
}) {
  const [name, setName] = useState('');
  const [hours, setHours] = useState<number>(1);
  const [price, setPrice] = useState<number>(20);
  const [active, setActive] = useState<boolean>(true);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setName('');
      setHours(1);
      setPrice(20);
      setActive(true);
      setErr(null);
    }
  }, [open]);

  const save = async () => {
    setSaving(true); setErr(null);
    try {
      if (!name.trim()) throw new Error('Name is required');
      if (!Number.isFinite(hours) || hours <= 0) throw new Error('Hours must be > 0');
      if (!Number.isFinite(price) || price < 0) throw new Error('Price must be ≥ 0');
      await onSave({ name: name.trim(), hours, priceCad: price, active });
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to add package');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Create Table Rental Package</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb:2 }}>{err}</Alert>}
        <Stack spacing={2}>
          <TextField label="Name" fullWidth value={name} onChange={e=>setName(e.target.value)} />
          <TextField label="Hours" type="number" inputProps={{ step:'0.5', min:'0.5' }} value={hours} onChange={e=>setHours(Number(e.target.value))} />
          <TextField label="Price (CAD)" type="number" value={price} onChange={e=>setPrice(Number(e.target.value))} />
          <Stack direction="row" spacing={2}>
            <Button onClick={() => setActive(true)} variant={active ? 'contained' : 'outlined'}>Active</Button>
            <Button onClick={() => setActive(false)} variant={!active ? 'contained' : 'outlined'}>Inactive</Button>
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={save} disabled={saving}>{saving ? <CircularProgress size={18}/> : 'Create'}</Button>
      </DialogActions>
    </Dialog>
  );
}
