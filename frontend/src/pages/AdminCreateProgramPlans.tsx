import { useEffect, useMemo, useState } from 'react';
import {
  Box, Paper, Stack, Typography, Button, IconButton, Divider, TextField,
  FormControl, InputLabel, Select, MenuItem, Chip, Table, TableHead, TableRow, TableCell, TableBody,
  Dialog, DialogTitle, DialogContent, DialogActions, Alert, CircularProgress
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import {
  adminListPrograms, adminCreateProgram, adminUpdateProgram, adminDeleteProgram,
  adminAddPackage, adminUpdatePackage, adminDeletePackage,
  adminAddSlot, adminUpdateSlot, adminDeleteSlot,
  adminListCoaches,
  type ProgramDto, type ProgramPackageDto, type ProgramSlotDto,
  type DayOfWeek, type EnrollmentMode,
} from '../lib/booking';

const DOW: DayOfWeek[] = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'];
const toHms = (hhmm: string) => (hhmm.length === 5 ? `${hhmm}:00` : hhmm);
const fromHms = (hms: string) => (hms?.length >= 5 ? hms.slice(0,5) : hms);
function validTimeRange(start: string, end: string): boolean { if (!start || !end) return false; return start < end; }

export default function AdminCreateProgramPlans() {
  return (
    <Box>
      <Header />
      <ProgramsPanel />
    </Box>
  );
}

function Header() {
  return (
        <Typography variant="h3" align="center" sx={{ fontWeight: 700, color: 'common.white', py: 3 }}>Create Program Plans</Typography>
  );
}

function ProgramsPanel() {
  const [items, setItems] = useState<ProgramDto[]>([]);
  const [coaches, setCoaches] = useState<{id:number; name:string; email:string}[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const pull = async () => {
    setLoading(true); setErr(null);
    try {
      const [programs, coachList] = await Promise.allSettled([
        adminListPrograms(),
        adminListCoaches(),
      ]);
      if (programs.status === 'fulfilled') setItems(programs.value ?? []);
      else setErr((programs as any).reason?.message ?? 'Failed to load programs');

      if (coachList.status === 'fulfilled') setCoaches(coachList.value ?? []);
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to load');
    } finally { setLoading(false); }
  };

  useEffect(() => { pull(); }, []);

  const patchProgram = (id: number, patch: Partial<ProgramDto>) => {
    setItems(prev => prev.map(p => (p.id === id ? { ...p, ...patch } : p)));
  };

  return (
    <>
      <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.9)', mb:2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h6" sx={{ fontWeight: 700 }}>Programs</Typography>
          <CreateProgramDialog onCreated={pull} />
        </Stack>
      </Paper>

      {err && (
        <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
          <Alert severity="error">{err}</Alert>
        </Paper>
      )}

      {loading ? (
        <Box sx={{ textAlign:'center', py:6 }}><CircularProgress /></Box>
      ) : items.length === 0 ? (
        <Paper sx={{ p:3, borderRadius:3, background:'rgba(255,255,255,0.95)' }}>
          <Typography>No programs yet. Create your first one.</Typography>
        </Paper>
      ) : (
        items.map(p => (
          <ProgramEditor
            key={p.id}
            program={p}
            coaches={coaches}
            onPatched={(patch) => patchProgram(p.id, patch)}
            onChanged={pull}
            onDelete={pull}
          />
        ))
      )}
    </>
  );
}

function CreateProgramDialog({ onCreated }: { onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [mode, setMode] = useState<EnrollmentMode>('OPEN');
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const save = async () => {
    setSaving(true); setErr(null);
    try {
      const trimmed = title.trim();
      if (!trimmed) throw new Error('Title is required');
      await adminCreateProgram({
        title: trimmed,
        description: desc.trim(),
        active: true,
        enrollmentMode: mode,
      });
      setOpen(false);
      setTitle(''); setDesc(''); setMode('OPEN');
      onCreated();
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to create');
    } finally { setSaving(false); }
  };

  return (
    <>
      <Button variant="contained" startIcon={<AddIcon />} onClick={()=>setOpen(true)}>New Program</Button>
      <Dialog open={open} onClose={()=>setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Program</DialogTitle>
        <DialogContent dividers>
          {err && <Alert severity="error" sx={{ mb:2 }}>{err}</Alert>}
          <Stack spacing={2}>
            <TextField fullWidth label="Title" value={title} onChange={e=>setTitle(e.target.value)} />
            <TextField fullWidth label="Description" multiline minRows={3} value={desc} onChange={e=>setDesc(e.target.value)} />
            <FormControl fullWidth>
              <InputLabel>Enrollment</InputLabel>
              <Select
                label="Enrollment"
                value={mode}
                onChange={e=>setMode(e.target.value as EnrollmentMode)}
              >
                <MenuItem value="OPEN">Open (public)</MenuItem>
                <MenuItem value="ADMIN_ONLY">Admin-only (invite)</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={()=>setOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!title.trim() || saving} onClick={save}>
            {saving ? <CircularProgress size={18}/> : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

function ProgramEditor({
  program, coaches, onPatched, onChanged, onDelete,
}: {
  program: ProgramDto;
  coaches: {id:number; name:string}[];
  onPatched: (patch: Partial<ProgramDto>) => void;
  onChanged: () => void;
  onDelete: () => void;
}) {
  const [title, setTitle] = useState(program.title);
  const [desc, setDesc] = useState(program.description ?? '');
  const [active, setActive] = useState(program.active);
  const [mode, setMode] = useState<EnrollmentMode>(program.enrollmentMode ?? 'OPEN');

  const [addPkgOpen, setAddPkgOpen] = useState(false);

  const saveHeader = async () => {
    await adminUpdateProgram(program.id, { title, description: desc, active, enrollmentMode: mode });
    onPatched({ title, description: desc, active, enrollmentMode: mode });
  };

  const del = async () => {
    if (!confirm(`Delete program "${program.title}"?`)) return;
    await adminDeleteProgram(program.id);
    onDelete();
  };

  const coachesById = useMemo(() => {
    const m = new Map<number,string>();
    coaches.forEach(c => m.set(c.id, c.name));
    return m;
  }, [coaches]);

  const ordered = useMemo(
    () => [...program.packages].sort((a,b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id),
    [program.packages],
  );

  const swapOrder = async (a: ProgramPackageDto, b: ProgramPackageDto) => {
    const ao = a.sortOrder ?? 0;
    const bo = b.sortOrder ?? 0;
    await Promise.all([
      adminUpdatePackage(a.id, { sortOrder: bo }),
      adminUpdatePackage(b.id, { sortOrder: ao }),
    ]);
    onChanged();
  };

  return (
    <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:3 }}>
      <Stack spacing={2}>
        <Stack direction={{ xs:'column', md:'row' }} spacing={2} alignItems={{ md:'center' }}>
          <TextField label="Title" value={title} onChange={e=>setTitle(e.target.value)} fullWidth />
          <FormControl size="small" sx={{ width: 180 }}>
            <InputLabel>Active</InputLabel>
            <Select label="Active" value={String(active)} onChange={e=>setActive(e.target.value === 'true')}>
              <MenuItem value="true">Active</MenuItem>
              <MenuItem value="false">Inactive</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ width: 220 }}>
            <InputLabel>Enrollment</InputLabel>
            <Select label="Enrollment" value={mode} onChange={e=>setMode(e.target.value as EnrollmentMode)}>
              <MenuItem value="OPEN">Open (public)</MenuItem>
              <MenuItem value="ADMIN_ONLY">Admin-only (invite)</MenuItem>
            </Select>
          </FormControl>
          <Button variant="contained" onClick={saveHeader}>Save</Button>
          <Button color="error" onClick={del} startIcon={<DeleteIcon />}>Delete</Button>
        </Stack>

        <TextField label="Description" value={desc} onChange={e=>setDesc(e.target.value)} fullWidth multiline minRows={2} />

        <Divider />

        {/* packages */}
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Typography variant="h6">Packages</Typography>
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip label="Reorder with arrows" variant="outlined" size="small" />
            <Button size="small" onClick={() => setAddPkgOpen(true)} startIcon={<AddIcon />}>Add Package</Button>
          </Stack>
        </Stack>

        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Order</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Sessions</TableCell>
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
                <PackageRow
                  key={pk.id}
                  pack={pk}
                  onChanged={onChanged}
                  canMoveUp={!!prev}
                  canMoveDown={!!next}
                  onMoveUp={() => prev && swapOrder(pk, prev)}
                  onMoveDown={() => next && swapOrder(pk, next)}
                />
              );
            })}
          </TableBody>
        </Table>

        <AddPackageDialog
          open={addPkgOpen}
          onClose={() => setAddPkgOpen(false)}
          onSave={async (payload) => {
            await adminAddPackage(program.id, payload);
            setAddPkgOpen(false);
            onChanged();
          }}
        />

        <Divider />

        {/* slots */}
        <Typography variant="h6">Weekly Slots</Typography>
        <WeeklySlotsGrid
          programId={program.id}
          slots={program.slots}
          coaches={coaches}
          onChanged={onChanged}
          coachNameById={(id) => coachesById.get(id) ?? ''}
        />

        {!!coaches.length && (
          <>
            <Divider />
            <Typography variant="body2" color="text.secondary">
              <b>Coaches:</b> {coaches.map(c => `${c.id} - ${c.name}`).join(' • ')}
            </Typography>
          </>
        )}
      </Stack>
    </Paper>
  );
}

function AddPackageDialog({
  open, onClose, onSave,
}: {
  open: boolean;
  onClose: () => void;
  onSave: (payload: { name: string; sessionsCount: number; priceCad: number; active?: boolean; sortOrder?: number }) => Promise<void>;
}) {
  const [name, setName] = useState('');
  const [sessions, setSessions] = useState<number>(6);
  const [price, setPrice] = useState<number>(100);
  const [active, setActive] = useState<boolean>(true);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setName('');
      setSessions(6);
      setPrice(100);
      setActive(true);
      setErr(null);
    }
  }, [open]);

  const save = async () => {
    setSaving(true); setErr(null);
    try {
      if (!name.trim()) throw new Error('Name is required');
      if (!Number.isFinite(sessions) || sessions <= 0) throw new Error('Sessions must be > 0');
      if (!Number.isFinite(price) || price < 0) throw new Error('Price must be ≥ 0');
      await onSave({ name: name.trim(), sessionsCount: sessions, priceCad: price, active });
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to add package');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add Package</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb:2 }}>{err}</Alert>}
        <TextField label="Name" fullWidth sx={{ mb:2 }} value={name} onChange={e=>setName(e.target.value)} />
        <Stack direction={{ xs:'column', sm:'row' }} spacing={2}>
          <TextField label="Sessions" type="number" sx={{ flex:1 }} value={sessions} onChange={e=>setSessions(Number(e.target.value))} />
          <TextField label="Price (CAD)" type="number" sx={{ flex:1 }} value={price} onChange={e=>setPrice(Number(e.target.value))} />
        </Stack>
        <Stack direction="row" spacing={2} sx={{ mt:2 }}>
          <Button onClick={() => setActive(true)} variant={active ? 'contained' : 'outlined'}>Active</Button>
          <Button onClick={() => setActive(false)} variant={!active ? 'contained' : 'outlined'}>Inactive</Button>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={save} disabled={saving}>{saving ? <CircularProgress size={18}/> : 'Add'}</Button>
      </DialogActions>
    </Dialog>
  );
}

function PackageRow({
  pack, onChanged, canMoveUp, canMoveDown, onMoveUp, onMoveDown,
}: {
  pack: ProgramPackageDto;
  onChanged: () => void;
  canMoveUp: boolean;
  canMoveDown: boolean;
  onMoveUp: () => void;
  onMoveDown: () => void;
}) {
  const [name, setName] = useState(pack.name);
  const [sc, setSc]     = useState(pack.sessionsCount);
  const [price, setPrice] = useState(pack.priceCad);
  const [active, setActive] = useState(pack.active);

  const save = async () => {
    if (!name.trim()) return alert('Name is required');
    if (!Number.isFinite(sc) || sc <= 0) return alert('Sessions must be > 0');
    if (!Number.isFinite(price) || price < 0) return alert('Price must be >= 0');

    await adminUpdatePackage(pack.id, { name: name.trim(), sessionsCount: sc, priceCad: price, active });
    onChanged();
  };

  const del = async () => {
    if (!confirm('Delete this package?')) return;
    await adminDeletePackage(pack.id);
    onChanged();
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
      <TableCell><TextField size="small" type="number" value={sc} onChange={e=>setSc(Number(e.target.value))} /></TableCell>
      <TableCell><TextField size="small" type="number" value={price} onChange={e=>setPrice(Number(e.target.value))} /></TableCell>
      <TableCell>
        <Select size="small" value={String(active)} onChange={e=>setActive(e.target.value==='true')}>
          <MenuItem value="true">Active</MenuItem>
          <MenuItem value="false">Inactive</MenuItem>
        </Select>
      </TableCell>
      <TableCell align="right" sx={{ whiteSpace:'nowrap' }}>
        <Button size="small" onClick={save}>Save</Button>
        <IconButton size="small" onClick={del}><DeleteIcon fontSize="small" /></IconButton>
      </TableCell>
    </TableRow>
  );
}

function WeeklySlotsGrid({
  programId, slots, coaches, onChanged, coachNameById,
}: {
  programId: number;
  slots: ProgramSlotDto[];
  coaches: {id:number; name:string}[];
  onChanged: () => void;
  coachNameById: (id: number) => string;
}) {
  const [addOpen, setAddOpen] = useState<null | { weekday: DayOfWeek }>(null);

  const byDay = useMemo(() => {
    const map = new Map<DayOfWeek, ProgramSlotDto[]>();
    DOW.forEach(d => map.set(d, []));
    slots.forEach(s => map.get(s.weekday)!.push(s));
    DOW.forEach(d => map.get(d)!.sort((a,b) => fromHms(a.startTime).localeCompare(fromHms(b.startTime))));
    return map;
  }, [slots]);

  return (
    <>
      <Box sx={{ display:'grid', gridTemplateColumns: '1fr', gap:1, minWidth: 0, overflowX: 'hidden' }}>
        {DOW.map(d => (
          <Box key={d} sx={{ border:'1px solid rgba(0,0,0,0.12)', borderRadius: 2, p: 1.2, background:'rgba(255,255,255,0.6)', minWidth:0 }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 0.5 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>{d.slice(0,1) + d.slice(1).toLowerCase()}</Typography>
              <IconButton size="small" onClick={() => setAddOpen({ weekday: d })} aria-label={`Add slot for ${d}`}><AddIcon fontSize="small" /></IconButton>
            </Stack>

            {byDay.get(d)!.length === 0 ? (
              <Typography variant="caption" sx={{ opacity: 0.6 }}>—</Typography>
            ) : (
              <Stack spacing={1}>
                {byDay.get(d)!.map(s => (
                  <SlotEditorRow key={s.id} slot={s} coaches={coaches} onChanged={onChanged} coachNameById={coachNameById} />
                ))}
              </Stack>
            )}
          </Box>
        ))}
      </Box>

      <AddSlotDialog
        open={!!addOpen}
        weekday={addOpen?.weekday ?? 'MONDAY'}
        coaches={coaches}
        onClose={() => setAddOpen(null)}
        onSave={async (payload) => {
          await adminAddSlot(programId, payload);
          setAddOpen(null);
          onChanged();
        }}
      />
    </>
  );
}

function AddSlotDialog({
  open, weekday, coaches, onClose, onSave,
}: {
  open: boolean;
  weekday: DayOfWeek;
  coaches: {id:number; name:string}[];
  onClose: () => void;
  onSave: (payload: { weekday: DayOfWeek; startTime: string; endTime: string; coachId: number }) => Promise<void>;
}) {
  const [start, setStart] = useState('17:00');
  const [end, setEnd] = useState('18:00');
  const [coachId, setCoachId] = useState<number | ''>('');

  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setStart('17:00'); setEnd('18:00'); setCoachId('');
      setErr(null);
    }
  }, [open]);

  const save = async () => {
    setSaving(true); setErr(null);
    try {
      if (!validTimeRange(start, end)) throw new Error('End time must be after start time');
      if (!Number.isFinite(Number(coachId))) throw new Error('Please choose a coach');
      await onSave({ weekday, startTime: toHms(start), endTime: toHms(end), coachId: Number(coachId) });
    } catch (e:any) {
      setErr(e?.message ?? 'Failed to add slot');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Add Slot - {weekday.slice(0,1) + weekday.slice(1).toLowerCase()}</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb:2 }}>{err}</Alert>}
        <Stack spacing={2}>
          <TextField label="Start" type="time" value={start} onChange={e=>setStart(e.target.value)} />
          <TextField label="End"   type="time" value={end}   onChange={e=>setEnd(e.target.value)} />
          <FormControl fullWidth>
            <InputLabel>Coach</InputLabel>
            <Select label="Coach" value={coachId} onChange={e=>setCoachId(e.target.value as number | '')}>
              {coaches.length === 0
                ? <MenuItem value=""><em>No coaches found</em></MenuItem>
                : coaches.map(c => <MenuItem key={c.id} value={c.id}>{c.name} (#{c.id})</MenuItem>)}
            </Select>
          </FormControl>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={save} disabled={saving || coaches.length === 0}>
          {saving ? <CircularProgress size={18}/> : 'Add'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function SlotEditorRow({
  slot, coaches, onChanged, coachNameById,
}: {
  slot: ProgramSlotDto;
  coaches: {id:number; name:string}[];
  onChanged: () => void;
  coachNameById: (id: number) => string;
}) {
  const [start, setStart]   = useState(fromHms(slot.startTime));
  const [end, setEnd]       = useState(fromHms(slot.endTime));
  const [coachId, setCoach] = useState<number>(slot.coachId);

  const save = async () => {
    if (!validTimeRange(start, end)) { alert('End time must be after start time'); return; }
    await adminUpdateSlot(slot.id, {
      weekday: slot.weekday,
      startTime: toHms(start),
      endTime: toHms(end),
      coachId,
    });
    onChanged();
  };

  const del = async () => {
    if (!confirm('Delete this slot?')) return;
    await adminDeleteSlot(slot.id);
    onChanged();
  };

  return (
    <Box sx={{ border:'1px dashed rgba(0,0,0,0.2)', borderRadius:1.5, p:1 }}>
      <Stack direction={{ xs:'column', sm:'row' }} spacing={1} alignItems={{ xs:'stretch', sm:'center' }} flexWrap="wrap">
        <TextField size="small" type="time" value={start} onChange={e=>setStart(e.target.value)} sx={{ width:{ xs:'100%', sm:120 } }} />
        <TextField size="small" type="time" value={end}   onChange={e=>setEnd(e.target.value)}   sx={{ width:{ xs:'100%', sm:120 } }} />
        <FormControl size="small" sx={{ minWidth:{ xs:'100%', sm: 200 } }}>
          <InputLabel>Coach</InputLabel>
          <Select label="Coach" value={coachId} onChange={e=>setCoach(Number(e.target.value))}>
            {coaches.length === 0
              ? <MenuItem value={coachId}><em>No coaches found</em></MenuItem>
              : coaches.map(c => <MenuItem key={c.id} value={c.id}>{c.name} (#{c.id})</MenuItem>)}
          </Select>
        </FormControl>
        <Typography variant="caption" sx={{ ml: { xs: 0, sm: 'auto' }, opacity:0.7, minWidth: { sm: 140 }, textAlign: { xs:'left', sm:'right' } }}>
          {coachNameById(coachId)}
        </Typography>
        <Stack direction="row" spacing={1} sx={{ ml: { sm: 0 } }}>
          <Button size="small" onClick={save}>Save</Button>
          <IconButton size="small" onClick={del}><DeleteIcon fontSize="small" /></IconButton>
        </Stack>
      </Stack>
    </Box>
  );
}
