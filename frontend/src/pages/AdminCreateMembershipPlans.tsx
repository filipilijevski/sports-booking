import { useEffect, useState } from 'react';
import {
  Box, Paper, Stack, Typography, Button, TextField, FormControl, InputLabel, Select, MenuItem,
  Divider, Dialog, DialogTitle, DialogContent, DialogActions, Alert, CircularProgress, Table, TableHead, TableRow, TableCell, TableBody, IconButton
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import {
  adminListPlans, adminCreatePlan, adminUpdatePlan, adminDeletePlan,
  adminAddEntitlement, adminUpdateEntitlement, adminDeleteEntitlement,
  type MembershipPlanDto, type EntitlementKind, type PlanType
} from '../lib/booking';

export default function AdminCreateMembershipPlans() {
  return (
    <Box>
      <Header />
      <PlansPanel />
    </Box>
  );
}

function Header() {
  return (
    <Paper sx={{ p:2, borderRadius:3, mb:2, background:'rgba(255,255,255,0.9)' }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center">
        <Typography variant="h5" sx={{ fontWeight:700 }}>Create Membership Plans</Typography>
        <CreatePlanDialog onCreated={() => { /* handled in panel via pull */ }} />
      </Stack>
    </Paper>
  );
}

function PlansPanel() {
  const [items, setItems] = useState<MembershipPlanDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const pull = async () => {
    setLoading(true); setErr(null);
    try { setItems(await adminListPlans() ?? []); }
    catch (e:any) { setErr(e?.message ?? 'Failed to load'); }
    finally { setLoading(false); }
  };

  useEffect(() => { pull(); }, []);

  return (
    <>
      {err && (
        <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
          <Alert severity="error">{err}</Alert>
        </Paper>
      )}

      {loading ? (
        <Box sx={{ textAlign:'center', py:6 }}><CircularProgress /></Box>
      ) : items.length === 0 ? (
        <Paper sx={{ p:3, borderRadius:3, background:'rgba(255,255,255,0.95)' }}>
          <Typography>No plans yet. Create the INITIAL plan first.</Typography>
        </Paper>
      ) : (
        items.map(p => (
          <PlanEditor key={p.id} plan={p} onChanged={pull} onDelete={pull} />
        ))
      )}
    </>
  );
}

function CreatePlanDialog({ onCreated }: { onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [type, setType] = useState<PlanType>('INITIAL');
  const [name, setName] = useState('Initial Annual Club Membership');
  const [price, setPrice] = useState<number>(0);
  const [days, setDays] = useState<number>(365);
  const [desc, setDesc] = useState<string>('');
  const [holderKind, setHolderKind] = useState<'INDIVIDUAL'|'GROUP'>('INDIVIDUAL');
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const save = async () => {
    setSaving(true); setErr(null);
    try {
      if (!name.trim()) throw new Error('Name required');
      if (!Number.isFinite(price) || price < 0) throw new Error('Price must be >= 0');
      if (!Number.isInteger(days) || days <= 0) throw new Error('Duration (days) must be a positive integer');
      await adminCreatePlan({
        type,
        name: name.trim(),
        priceCad: price,
        durationDays: days,
        active: true,
        description: desc.trim() || null,
        holderKind,
      });
      setOpen(false);
      onCreated();
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to create');
    } finally { setSaving(false); }
  };

  return (
    <>

      <Button variant="contained" startIcon={<AddIcon />} onClick={()=>setOpen(true)}>New Plan</Button>
      <Dialog open={open} onClose={()=>setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Membership Plan</DialogTitle>
        <DialogContent dividers>
          {err && <Alert severity="error" sx={{ mb:2 }}>{err}</Alert>}
          <Stack spacing={2}>
            <FormControl fullWidth>
              <InputLabel>Type</InputLabel>
              <Select label="Type" value={type} onChange={e=>setType(e.target.value as PlanType)}>
                <MenuItem value="INITIAL">INITIAL</MenuItem>
                <MenuItem value="SPECIAL">SPECIAL</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Holder Kind</InputLabel>
              <Select label="Holder Kind" value={holderKind} onChange={e=>setHolderKind(e.target.value as 'INDIVIDUAL'|'GROUP')}>
                <MenuItem value="INDIVIDUAL">INDIVIDUAL</MenuItem>
                <MenuItem value="GROUP">GROUP</MenuItem>
              </Select>
            </FormControl>
            <TextField fullWidth label="Name" value={name} onChange={e=>setName(e.target.value)} />
            <TextField fullWidth type="number" label="Price (CAD)" value={price} onChange={e=>setPrice(Number(e.target.value))} />
            <TextField fullWidth type="number" label="Duration (days)" value={days} onChange={e=>setDays(Number(e.target.value))} />
            <TextField fullWidth label="Description (optional)" multiline minRows={3} value={desc} onChange={e=>setDesc(e.target.value)} />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={()=>setOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!name.trim() || saving} onClick={save}>
            {saving ? <CircularProgress size={18}/> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

function PlanEditor({
  plan, onChanged, onDelete,
}: {
  plan: MembershipPlanDto;
  onChanged: () => void;
  onDelete: () => void;
}) {
  const [type, setType] = useState<PlanType>(plan.type);
  const [name, setName] = useState(plan.name);
  const [price, setPrice] = useState<number>(plan.priceCad ?? 0);
  const [days, setDays] = useState<number>(plan.durationDays ?? 365);
  const [active, setActive] = useState<boolean>(plan.active);
  const [holderKind, setHolderKind] = useState<'INDIVIDUAL'|'GROUP'>(
    (plan as any).holderKind ?? 'INDIVIDUAL'
  );
  const [desc, setDesc] = useState<string>(plan.description ?? '');

  const [addEntOpen, setAddEntOpen] = useState(false);

  const save = async () => {
    if (!name.trim()) return alert('Name required');
    if (!Number.isFinite(price) || price < 0) return alert('Price must be >= 0');
    if (!Number.isInteger(days) || days <= 0) return alert('Duration (days) must be a positive integer');

    await adminUpdatePlan(plan.id, {
      type,
      name: name.trim(),
      priceCad: price,
      durationDays: days,
      active,
      description: desc.trim() || null,
      holderKind,
    });
    onChanged();
  };

  const del = async () => {
    if (!confirm(`Delete plan "${plan.name}"?`)) return;
    await adminDeletePlan(plan.id);
    onDelete();
  };

  return (
    <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:3 }}>
      <Stack spacing={2}>
        <Stack direction={{ xs:'column', md:'row' }} spacing={2} alignItems={{ md:'center' }}>
          <FormControl size="small" sx={{ width:160 }}>
            <InputLabel>Type</InputLabel>
            <Select label="Type" value={type} onChange={e=>setType(e.target.value as PlanType)}>
              <MenuItem value="INITIAL">INITIAL</MenuItem>
              <MenuItem value="SPECIAL">SPECIAL</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ width:180 }}>
            <InputLabel>Holder</InputLabel>
            <Select label="Holder" value={holderKind} onChange={e=>setHolderKind(e.target.value as 'INDIVIDUAL'|'GROUP')}>
              <MenuItem value="INDIVIDUAL">INDIVIDUAL</MenuItem>
              <MenuItem value="GROUP">GROUP</MenuItem>
            </Select>
          </FormControl>
          <TextField label="Name" value={name} onChange={e=>setName(e.target.value)} fullWidth />
        </Stack>

        <Stack direction={{ xs:'column', md:'row' }} spacing={2} alignItems={{ md:'center' }}>
          <TextField label="Price (CAD)" type="number" value={price} onChange={e=>setPrice(Number(e.target.value))} sx={{ width:{ xs:'100%', md:180 } }} />
          <TextField label="Duration (days)" type="number" value={days} onChange={e=>setDays(Number(e.target.value))} sx={{ width:{ xs:'100%', md:180 } }} />
          <FormControl size="small" sx={{ width:{ xs:'100%', md:150 } }}>
            <InputLabel>Status</InputLabel>
            <Select label="Status" value={String(active)} onChange={e=>setActive(e.target.value==='true')}>
              <MenuItem value="true">Active</MenuItem>
              <MenuItem value="false">Inactive</MenuItem>
            </Select>
          </FormControl>
          <Stack direction="row" spacing={1} sx={{ ml:{ md:'auto' } }}>
            <Button variant="contained" onClick={save}>Save</Button>
            <Button color="error" onClick={del} startIcon={<DeleteIcon />}>Delete</Button>
          </Stack>
        </Stack>

        <TextField
          label="Description (optional)"
          value={desc}
          onChange={e=>setDesc(e.target.value)}
          fullWidth
          multiline
          minRows={2}
        />

        <Divider />

        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h6">Entitlements</Typography>
          <Button size="small" onClick={()=>setAddEntOpen(true)} startIcon={<AddIcon />}>Add</Button>
        </Stack>

        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Kind</TableCell><TableCell>Amount</TableCell><TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {plan.entitlements.map(e => (
              <EntRow key={e.id} ent={e} onChanged={onChanged} />
            ))}
          </TableBody>
        </Table>

        <AddEntitlementDialog
          open={addEntOpen}
          onClose={() => setAddEntOpen(false)}
          onSave={async (payload) => {
            await adminAddEntitlement(plan.id, payload);
            setAddEntOpen(false);
            onChanged();
          }}
        />
      </Stack>
    </Paper>
  );
}

function AddEntitlementDialog({
  open, onClose, onSave,
}: {
  open: boolean;
  onClose: () => void;
  onSave: (payload: { kind: EntitlementKind; amount: number }) => Promise<void>;
}) {
  const [kind, setKind] = useState<EntitlementKind>('TABLE_HOURS');
  const [amount, setAmount] = useState<number>(1);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setKind('TABLE_HOURS');
      setAmount(1);
      setErr(null);
    }
  }, [open]);

  const save = async () => {
    setSaving(true); setErr(null);
    try {
      if (!['TABLE_HOURS','PROGRAM_CREDITS','TOURNAMENT_ENTRIES'].includes(kind)) throw new Error('Invalid kind');
      if (!Number.isFinite(amount) || amount < 0) throw new Error('Amount must be ≥ 0');
      await onSave({ kind, amount });
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to add entitlement');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Add Entitlement</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb:2 }}>{err}</Alert>}
        <Stack spacing={2}>
          <FormControl fullWidth>
            <InputLabel>Kind</InputLabel>
            <Select label="Kind" value={kind} onChange={e=>setKind(e.target.value as EntitlementKind)}>
              <MenuItem value="TABLE_HOURS">TABLE_HOURS</MenuItem>
              <MenuItem value="PROGRAM_CREDITS">PROGRAM_CREDITS</MenuItem>
              <MenuItem value="TOURNAMENT_ENTRIES">TOURNAMENT_ENTRIES</MenuItem>
            </Select>
          </FormControl>
          <TextField label="Amount" type="number" value={amount} onChange={e=>setAmount(Number(e.target.value))} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={save} disabled={saving}>{saving ? <CircularProgress size={18}/> : 'Add'}</Button>
      </DialogActions>
    </Dialog>
  );
}

function EntRow({ ent, onChanged }: { ent: { id:number; kind:EntitlementKind; amount:number }, onChanged: () => void }) {
  const [kind, setKind] = useState<EntitlementKind>(ent.kind);
  const [amount, setAmount] = useState<number>(ent.amount);

  const save = async () => {
    if (!['TABLE_HOURS','PROGRAM_CREDITS','TOURNAMENT_ENTRIES'].includes(kind)) { alert('Invalid kind'); return; }
    if (!Number.isFinite(amount) || amount < 0) { alert('Amount must be >= 0'); return; }
    await adminUpdateEntitlement(ent.id, { kind, amount });
    onChanged();
  };

  const del = async () => {
    if (!confirm('Delete entitlement?')) return;
    await adminDeleteEntitlement(ent.id);
    onChanged();
  };

  return (
    <TableRow>
      <TableCell>
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <Select value={kind} onChange={e=>setKind(e.target.value as EntitlementKind)}>
            <MenuItem value="TABLE_HOURS">TABLE_HOURS</MenuItem>
            <MenuItem value="PROGRAM_CREDITS">PROGRAM_CREDITS</MenuItem>
            <MenuItem value="TOURNAMENT_ENTRIES">TOURNAMENT_ENTRIES</MenuItem>
          </Select>
        </FormControl>
      </TableCell>
      <TableCell><TextField size="small" type="number" value={amount} onChange={e=>setAmount(Number(e.target.value))} /></TableCell>
      <TableCell align="right" sx={{ whiteSpace:'nowrap' }}>
        <Button size="small" onClick={save}>Save</Button>
        <IconButton size="small" onClick={del}><DeleteIcon fontSize="small" /></IconButton>
      </TableCell>
    </TableRow>
  );
}
