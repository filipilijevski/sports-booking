import { useEffect, useMemo, useState } from 'react';
import {
  Box, Container, Typography, Paper, Stack, TextField, MenuItem,
  CircularProgress, Alert, Table, TableHead, TableRow, TableCell, TableBody,
  Chip, Button
} from '@mui/material';
import { fetchProductAudits, type ProductAudit, type ProductAuditAction } from '../lib/audits';
import { useRole } from '../context/RoleContext';

const PAGE_GRADIENT = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

export default function AdminShopAudit() {
  const { role } = useRole();
  const isAdmin = role === 'ADMIN' || role === 'OWNER';

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [raw, setRaw] = useState<ProductAudit[]>([]);

  const [limit, setLimit] = useState(200);
  const [action, setAction] = useState<'ALL' | ProductAuditAction>('ALL');
  const [q, setQ] = useState('');

  const reload = async (l = limit) => {
    setLoading(true); setErr(null);
    try {
      const data = await fetchProductAudits(l);
      setRaw(data);
    } catch (e: any) {
      setErr(e?.message || 'Failed to load audit entries.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { reload(limit); /* eslint-disable-line */ }, []);

  const rows = useMemo(() => {
    const qq = q.trim().toLowerCase();
    return raw.filter(a => {
      const okA = action === 'ALL' || a.action === action;
      const hay = `${a.name ?? ''} ${a.sku ?? ''} ${a.categoryName ?? ''}`.toLowerCase();
      const okQ = !qq || hay.includes(qq);
      return okA && okQ;
    });
  }, [raw, action, q]);

  if (!isAdmin) {
    return (
      <Box sx={{ minHeight:'100vh', background: PAGE_GRADIENT, display:'flex', alignItems:'center' }}>
        <Container maxWidth="lg">
          <Paper sx={{ p:3, borderRadius:3, backgroundColor:'rgba(255,255,255,0.94)' }}>
            <Typography variant="h5">Access denied</Typography>
          </Paper>
        </Container>
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', background: PAGE_GRADIENT, py: 6 }}>
      <Container maxWidth="lg">
        <Typography variant="h3" align="center" sx={{ color:'white', fontWeight: 800, mb: 3 }}>
          Product Audit Log
        </Typography>

        <Paper sx={{ p:2, borderRadius:3, mb: 2 }}>
          <Stack direction={{ xs:'column', md:'row' }} spacing={2}>
            <TextField
              fullWidth
              size="small"
              label="Search (name / SKU / category)"
              value={q}
              onChange={e => setQ(e.target.value)}
            />
            <TextField fullWidth select label="Action" size="small" value={action} onChange={e=>setAction(e.target.value as any)}>
              <MenuItem value="ALL">ALL</MenuItem>
              <MenuItem value="CREATE">CREATE</MenuItem>
              <MenuItem value="UPDATE">UPDATE</MenuItem>
              <MenuItem value="STOCK">STOCK</MenuItem>
              <MenuItem value="IMAGE">IMAGE</MenuItem>
              <MenuItem value="DELETE">DELETE</MenuItem>
            </TextField>
            <TextField fullWidth select label="Limit" size="small" value={limit} onChange={e=>setLimit(Number(e.target.value))}>
              {[100, 200, 500, 1000].map(n => (<MenuItem key={n} value={n}>{n}</MenuItem>))}
            </TextField>
            <Button variant="contained" sx={{ borderRadius:3, px: 4 }} onClick={() => reload(limit)} disabled={loading}>Refresh</Button>
          </Stack>
        </Paper>

        {err && (
          <Paper sx={{ p: 2, borderRadius: 3, mb: 2 }}>
            <Alert severity="error">{err}</Alert>
          </Paper>
        )}

        <Paper sx={{ p:0, borderRadius:3, overflow:'auto' }}>
          {loading ? (
            <Box sx={{ textAlign:'center', py: 6 }}><CircularProgress /></Box>
          ) : rows.length === 0 ? (
            <Box sx={{ p: 2 }}><Typography>No activity found.</Typography></Box>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell width={140}>When</TableCell>
                  <TableCell width={110}>Action</TableCell>
                  <TableCell>Product</TableCell>
                  <TableCell>SKU</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>User</TableCell>
                  <TableCell>Details</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map(r => {
                  let pretty = '';
                  if (r.detailsJson && r.action === 'UPDATE') {
                    try {
                      const d = JSON.parse(r.detailsJson);
                      const pairs = Object.entries(d as any).map(([k, v]: any) =>
                        `${k}: ${JSON.stringify(v?.from)} → ${JSON.stringify(v?.to)}`
                      );
                      pretty = pairs.join(' • ');
                    } catch {
                      pretty = r.detailsJson;
                    }
                  }
                  return (
                    <TableRow key={r.id} hover>
                      <TableCell>{new Date(r.createdAt).toLocaleString()}</TableCell>
                      <TableCell><Chip size="small" label={r.action} /></TableCell>
                      <TableCell>{r.name ?? '(deleted)'}</TableCell>
                      <TableCell>{r.sku ?? '—'}</TableCell>
                      <TableCell>{r.categoryName ?? '—'}</TableCell>
                      <TableCell>{r.actorUserId ?? '—'}</TableCell>
                      <TableCell>{pretty || '—'}</TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </Paper>
      </Container>
    </Box>
  );
}
