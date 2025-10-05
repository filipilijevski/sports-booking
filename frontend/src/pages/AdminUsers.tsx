import React, { useEffect, useState } from 'react';
import {
  Box, Container, Paper, Stack, Typography, TextField, MenuItem, Button,
  CircularProgress, Chip, Divider, Pagination, Dialog, DialogTitle,
  DialogContent, DialogActions, IconButton, Tooltip, InputAdornment, Alert,
} from '@mui/material';
  import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import KeyIcon from '@mui/icons-material/Key';
import DeleteIcon from '@mui/icons-material/Delete';
import SearchIcon from '@mui/icons-material/Search';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useRole } from '../context/RoleContext';
import { api } from '../lib/api';

const INFO_GRADIENT = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

type Role = 'CLIENT' | 'COACH' | 'ADMIN' | 'OWNER';
type Provider = 'LOCAL' | 'GOOGLE';

type AdminUserDto = {
  id: number;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: Role | string;
  provider: Provider | string;
  createdAt: string;
};

type PageEnvelope<T> = {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
};

function AccessDenied() {
  return (
    <Box
      sx={{
        minHeight: '100dvh',
        backgroundImage: INFO_GRADIENT,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        display: 'flex',
        alignItems: 'center',
      }}
    >
      <Container maxWidth="lg">
        <Paper elevation={3} sx={{ p: 4, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.94)' }}>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
            Access denied
          </Typography>
          <Typography variant="body1" color="text.secondary">
            You must be signed in with an ADMIN or OWNER account to view this page.
          </Typography>
        </Paper>
      </Container>
    </Box>
  );
}

function ConfirmDialog({
  open, title, body, onCancel, onConfirm, working, confirmText = 'Confirm',
}: {
  open: boolean; title: string; body: React.ReactNode;
  onCancel: () => void; onConfirm: () => void;
  working?: boolean; confirmText?: string;
}) {
  return (
    <Dialog open={open} onClose={working ? undefined : onCancel} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" sx={{ mt: 1 }}>{body}</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={!!working}>Cancel</Button>
        <Button variant="contained" onClick={onConfirm} disabled={!!working}>
          {working ? <CircularProgress size={18} /> : confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function EditUserDialog({
  open, onClose, user, onSave,
}: {
  open: boolean; onClose: () => void; user: AdminUserDto | null;
  onSave: (payload: { firstName?: string; lastName?: string; email?: string; role?: Role }) => void;
}) {
  const [firstName, setFirst] = useState(''); const [lastName, setLast] = useState('');
  const [email, setEmail] = useState(''); const [role, setRole] = useState<Role>('CLIENT');
  useEffect(() => {
    if (!user) return;
    setFirst(user.firstName ?? ''); setLast(user.lastName ?? ''); setEmail(user.email ?? '');
    setRole((user.role as Role) ?? 'CLIENT');
  }, [user]);
  if (!user) return null;
  const isOauth = user.provider !== 'LOCAL';

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Edit User</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField label="First Name" fullWidth value={firstName} onChange={e => setFirst(e.target.value)} />
            <TextField label="Last Name"  fullWidth value={lastName} onChange={e => setLast(e.target.value)} />
          </Stack>
          <TextField label="Email" fullWidth value={email} onChange={e => setEmail(e.target.value)} disabled={isOauth}
                     helperText={isOauth ? 'Email is managed by OAuth2 and cannot be changed.' : ' '} />
          <TextField select label="Role" fullWidth value={role} onChange={e => setRole(e.target.value as Role)}>
            <MenuItem value="CLIENT">CLIENT</MenuItem>
            <MenuItem value="COACH">COACH</MenuItem>
            <MenuItem value="ADMIN">ADMIN</MenuItem>
            <MenuItem value="OWNER">OWNER</MenuItem>
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          onClick={() => {
            const payload: any = {};
            if (firstName !== (user.firstName ?? '')) payload.firstName = firstName;
            if (lastName  !== (user.lastName  ?? '')) payload.lastName  = lastName;
            if (email     !== (user.email     ?? '') && user.provider === 'LOCAL') payload.email = email;
            if (role      !== (user.role      ?? 'CLIENT')) payload.role = role;
            onSave(payload);
          }}
        >
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function ResetPasswordDialog({
  open, onClose, user, onSubmit,
}: {
  open: boolean; onClose: () => void; user: AdminUserDto | null; onSubmit: (temp: string) => void;
}) {
  const [pwd, setPwd] = useState(''); useEffect(() => setPwd(''), [open]); if (!user) return null;
  const isOauth = user.provider !== 'LOCAL';
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Reset Password</DialogTitle>
      <DialogContent dividers>
        {isOauth ? (
          <Alert severity="info">This user signed in via OAuth2. Password cannot be changed.</Alert>
        ) : (
          <TextField label="Temporary password" type="password" fullWidth value={pwd} onChange={e => setPwd(e.target.value)}
                     helperText="Minimum 8 characters. The user should log in and change it." />
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="outlined" disabled={isOauth || pwd.length < 8} onClick={() => onSubmit(pwd)}>
          Set Temporary Password
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function CreateUserDialog({
  open, onClose, onCreate,
}: {
  open: boolean; onClose: () => void;
  onCreate: (payload: { firstName: string; lastName: string; email: string; password: string; role: Role }) => void;
}) {
  const [firstName, setFirst] = useState(''); const [lastName, setLast] = useState('');
  const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('CLIENT'); const [err, setErr] = useState('');
  useEffect(() => { if (!open) { setFirst(''); setLast(''); setEmail(''); setPassword(''); setRole('CLIENT'); setErr(''); } }, [open]);

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Create User</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField label="First Name" fullWidth value={firstName} onChange={e => setFirst(e.target.value)} />
            <TextField label="Last Name"  fullWidth value={lastName} onChange={e => setLast(e.target.value)} />
          </Stack>
          <TextField label="Email" fullWidth value={email} onChange={e => setEmail(e.target.value)} />
          <TextField label="Password" type="password" fullWidth value={password} onChange={e => setPassword(e.target.value)} helperText="Minimum 8 characters" />
          <TextField select label="Role" fullWidth value={role} onChange={e => setRole(e.target.value as Role)}>
            <MenuItem value="CLIENT">CLIENT</MenuItem>
            <MenuItem value="COACH">COACH</MenuItem>
            <MenuItem value="ADMIN">ADMIN</MenuItem>
            <MenuItem value="OWNER">OWNER</MenuItem>
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          onClick={() => {
            setErr('');
            if (!firstName.trim() || !lastName.trim() || !email.trim() || password.length < 8) {
              setErr('Please fill all fields. Password must be at least 8 characters.');
              return;
            }
            onCreate({ firstName: firstName.trim(), lastName: lastName.trim(), email: email.trim(), password, role });
          }}
        >
          Create
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function AdminUsers() {
  const { role } = useRole();
  const isAdmin = role === 'OWNER' || role === 'ADMIN';
  if (!isAdmin) return <AccessDenied />;

  const [items, setItems] = useState<AdminUserDto[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const [q, setQ] = useState('');
  const [roleFilter, setRoleFilter] = useState<'ALL' | Role>('ALL');
  const [provFilter, setProvFilter] = useState<'ALL' | Provider>('ALL');

  const [editOpen, setEditOpen] = useState(false);
  const [pwdOpen, setPwdOpen]   = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmTitle, setConfirmTitle] = useState<string>('Please Confirm');
  const [confirmBody, setConfirmBody] = useState<React.ReactNode>(null);
  const [confirmAction, setConfirmAction] = useState<(() => Promise<void>) | null>(null);
  const [confirmWorking, setConfirmWorking] = useState(false);

  const [selected, setSelected] = useState<AdminUserDto | null>(null);
  const pageSize = 12;

  const fetchData = async (targetPage?: number) => {
    try {
      setLoading(true);
      setErr(null);
      const p = typeof targetPage === 'number' ? targetPage : page;
      const usp = new URLSearchParams();
      if (q.trim()) usp.set('q', q.trim());
      if (roleFilter !== 'ALL') usp.set('role', roleFilter);
      if (provFilter !== 'ALL') usp.set('provider', provFilter);
      usp.set('page', String(p));
      usp.set('size', String(pageSize));
      const data = await api<PageEnvelope<AdminUserDto>>(`/admin/users?${usp.toString()}`);

      if (!data) { setItems([]); setTotalPages(1); setPage(p); return; }
      const content = Array.isArray((data as any).content) ? (data as any).content as AdminUserDto[] : (data as unknown as AdminUserDto[]);
      setItems(content || []); setTotalPages(data.totalPages ?? 1); setPage(data.number ?? p);
    } catch (e: any) {
      setErr(e?.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(0); /* eslint-disable-next-line */ }, []);

  const handleApply = async () => fetchData(0);
  const handleReset = async () => { setQ(''); setRoleFilter('ALL'); setProvFilter('ALL'); fetchData(0); };
  const openEdit = (u: AdminUserDto) => { setSelected(u); setEditOpen(true); };
  const openPwd  = (u: AdminUserDto) => { setSelected(u); setPwdOpen(true); };

  const performWithConfirm = (title: string, body: React.ReactNode, fn: () => Promise<void>) => {
    setConfirmTitle(title); setConfirmBody(body); setConfirmAction(() => fn); setConfirmOpen(true);
  };

  const onSaveUser = (payload: any) => {
    if (!selected) return;
    const hasChanges = Object.keys(payload).length > 0;
    if (!hasChanges) { setEditOpen(false); return; }
    const body = (
      <Box>
        <Typography variant="body2" sx={{ mb: 1 }}>
          You are about to update user <b>#{selected.id}</b> ({selected.email}).
        </Typography>
        <Typography variant="body2">Changes:</Typography>
        <ul style={{ marginTop: 4, marginBottom: 0 }}>
          {Object.entries(payload).map(([k, v]) => (
            <li key={k}><Typography variant="body2"><b>{k}</b>: {String(v)}</Typography></li>
          ))}
        </ul>
      </Box>
    );
    performWithConfirm('Confirm Profile Update', body, async () => {
      await api(`/admin/users/${selected.id}`, { method: 'PATCH', body: JSON.stringify(payload) });
      setEditOpen(false);
      await fetchData();
    });
  };

  const onResetPassword = (temp: string) => {
    if (!selected) return;
    const body = (
      <Box>
        <Typography variant="body2" sx={{ mb: 1 }}>
          Set a temporary password for user <b>#{selected.id}</b> ({selected.email})?
        </Typography>
        <Typography variant="body2">They will be able to log in and change it on their Profile page.</Typography>
      </Box>
    );
    performWithConfirm('Confirm Password Reset', body, async () => {
      await api(`/admin/users/${selected.id}/reset-password`, { method: 'POST', body: JSON.stringify({ temporaryPassword: temp }) });
      setPwdOpen(false);
    });
  };

  const onDeleteUser = (u: AdminUserDto) => {
    const body = (
      <Box>
        <Typography variant="body2" sx={{ mb: 1 }}>
          You are about to delete user <b>#{u.id}</b> ({u.email}). This is a soft delete and can be reversed by an admin via DB.
        </Typography>
      </Box>
    );
    performWithConfirm('Confirm Delete', body, async () => {
      await api(`/admin/users/${u.id}`, { method: 'DELETE' });
      await fetchData();
    });
  };

  const roleColor = (r: string) => {
    switch (r) { case 'OWNER': return 'secondary'; case 'ADMIN': return 'error'; case 'COACH': return 'info'; default: return 'default'; }
  };
  const providerColor = (p: string) => (p === 'LOCAL' ? 'success' : 'warning');

  return (
    <Box component="main" sx={{ minHeight: '100dvh', position: 'relative' }}>
      <Box aria-hidden sx={{ position: 'fixed', inset: 0, zIndex: -1, backgroundImage: INFO_GRADIENT, backgroundRepeat: 'no-repeat', backgroundAttachment: 'fixed', backgroundSize: 'cover', backgroundPosition: 'center', }} />
      <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 } }}>
        <Typography variant="h3" align="center" sx={{ color: 'common.white', fontWeight: 700 }}>
          Manage Profiles
        </Typography>

        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
          <Tooltip title="Refresh">
            <span><IconButton onClick={() => fetchData()} disabled={loading} sx={{ color: '#fff' }}><RefreshIcon /></IconButton></span>
          </Tooltip>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            Create user
          </Button>
        </Stack>

        <Paper elevation={3} sx={{ p: 2, borderRadius: 3, mb: 2, backgroundColor: 'rgba(255,255,255,0.9)' }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr auto' }, gap: 2 }}>
            <TextField
              fullWidth
              label="Search (email / first / last)"
              value={q}
              onChange={e => setQ(e.target.value)}
              InputProps={{ startAdornment: (<InputAdornment position="start"><SearchIcon /></InputAdornment>) }}
            />
            <TextField select label="Role" value={roleFilter} onChange={e => setRoleFilter(e.target.value as any)} size="small">
              <MenuItem value="ALL">All</MenuItem>
              <MenuItem value="CLIENT">CLIENT</MenuItem>
              <MenuItem value="COACH">COACH</MenuItem>
              <MenuItem value="ADMIN">ADMIN</MenuItem>
              <MenuItem value="OWNER">OWNER</MenuItem>
            </TextField>
            <TextField select label="Provider" value={provFilter} onChange={e => setProvFilter(e.target.value as any)} size="small">
              <MenuItem value="ALL">All</MenuItem>
              <MenuItem value="LOCAL">LOCAL</MenuItem>
              <MenuItem value="GOOGLE">GOOGLE</MenuItem>
            </TextField>
            <Stack direction="row" spacing={1} justifyContent="flex-end" alignItems="center">
              <Button variant="contained" onClick={handleApply} disabled={loading} startIcon={<SearchIcon />}>Apply</Button>
              <Button variant="text" onClick={handleReset} disabled={loading}>Reset</Button>
            </Stack>
          </Box>
        </Paper>

        {err && (
          <Paper elevation={3} sx={{ p: 2, mb: 2, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.9)' }}>
            <Alert severity="error">{err}</Alert>
          </Paper>
        )}

        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '0.5fr 0.5fr', md: '1fr 1fr', lg: '0.5fr 0.5fr 0.5fr' }, gap: 2, maxWidth: 'lg' }}>
          {loading ? (
            <Box sx={{ gridColumn: '1 / -1', py: 6, textAlign: 'center' }}><CircularProgress /></Box>
          ) : items.length === 0 ? (
            <Paper elevation={3} sx={{ p: 1, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.9)', gridColumn: '1 / -1', textAlign: 'center' }}>
              <Typography>No users found.</Typography>
            </Paper>
          ) : (
            items.map(u => (
              <Paper key={u.id} elevation={3} sx={{ p: 2, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
                <Stack direction="row" alignItems="center" justifyContent="space-between">
                  <Stack spacing={0.5}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                      {(u.firstName || u.lastName) ? `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() : '(no name)'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">{u.email}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      Joined: {new Date(u.createdAt).toLocaleString()}
                    </Typography>
                  </Stack>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Chip size="small" label={String(u.role)} color={roleColor(String(u.role)) as any} />
                    <Chip size="small" label={String(u.provider)} color={providerColor(String(u.provider)) as any} />
                  </Stack>
                </Stack>

                <Divider sx={{ my: 1.5 }} />

                <Stack direction="row" spacing={1} justifyContent="center">
                  <Tooltip title="Edit name / email / role">
                    <span><Button size="small" variant="outlined" startIcon={<EditIcon />} onClick={() => openEdit(u)}>Edit</Button></span>
                  </Tooltip>
                  <Tooltip title={u.provider !== 'LOCAL' ? 'OAuth2 user - cannot reset password' : 'Set a temporary password'}>
                    <span><Button size="small" variant="contained" startIcon={<KeyIcon />} disabled={u.provider !== 'LOCAL'} onClick={() => openPwd(u)}>Password</Button></span>
                  </Tooltip>
                  <Tooltip title="Soft-delete this user">
                    <span><Button size="small" variant="text" color="error" startIcon={<DeleteIcon />} onClick={() => onDeleteUser(u)}>Delete</Button></span>
                  </Tooltip>
                </Stack>
              </Paper>
            ))
          )}
        </Box>

        {!loading && items.length > 0 && (
          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 5 }}>
            <Pagination color="primary" count={totalPages} page={page + 1} onChange={(_, p1) => fetchData(p1 - 1)} />
          </Box>
        )}
      </Container>

      <EditUserDialog open={editOpen} onClose={() => setEditOpen(false)} user={selected} onSave={payload => onSaveUser(payload)} />
      <ResetPasswordDialog open={pwdOpen} onClose={() => setPwdOpen(false)} user={selected} onSubmit={temp => onResetPassword(temp)} />
      <CreateUserDialog open={createOpen} onClose={() => setCreateOpen(false)} onCreate={async (p) => {
        try {
          await api('/admin/users', { method: 'POST', body: JSON.stringify(p) });
          setCreateOpen(false);
          await fetchData(0);
        } catch (e: any) {
          alert(e?.message || 'Failed to create user');
        }
      }} />
      <ConfirmDialog
        open={confirmOpen}
        title={confirmTitle}
        body={confirmBody}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={async () => {
          if (!confirmAction) return;
          try {
            setConfirmWorking(true);
            await confirmAction();
          } finally {
            setConfirmWorking(false);
            setConfirmOpen(false);
          }
        }}
        working={confirmWorking}
        confirmText="Yes, proceed"
      />
    </Box>
  );
}
