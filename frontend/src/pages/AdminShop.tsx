import {
  Box, Container, Typography, Button, Paper, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions,
  MenuItem, Select, FormControl, InputLabel, Stack,
  TextField, CircularProgress, Pagination, InputAdornment, Chip,
  Tooltip, Alert, Divider, List, ListItem, ListItemText, ListItemSecondaryAction
} from '@mui/material';
import CloseIcon   from '@mui/icons-material/Close';
import EditIcon    from '@mui/icons-material/Edit';
import DeleteIcon  from '@mui/icons-material/Delete';
import AddIcon     from '@mui/icons-material/Add';
import ListAltIcon from '@mui/icons-material/ListAlt';
import CategoryIcon from '@mui/icons-material/Category';
import { useEffect, useMemo, useState, type ChangeEvent } from 'react';

import {
  fetchProducts, createProduct, updateProduct, deleteProduct,
  uploadProductImage,
  type Product,
} from '../lib/products';
import {
  fetchCategories, createCategory, deleteCategory as apiDeleteCategory,
  type Category,
} from '../lib/categories';
import { fetchProductAudits, type ProductAudit } from '../lib/audits';
import { useRole }        from '../context/RoleContext';
import CategoryChips      from '../components/CategoryChips';
import AdminProductCard   from '../components/AdminProductCard';
import { resolveImgUrl }  from '../lib/assets';


const BG_GRADIENT = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

const PAGE_OPTS = [10, 20, 50];

const selectorSx = {
  background: 'white',
  borderRadius: 1,
  '& .MuiOutlinedInput-notchedOutline': { borderColor: 'transparent' },
  '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'transparent' },
};

/* Read-only Quick-View modal (admin side)  */
interface ViewProps { product: Product | null; open: boolean; onClose: () => void }
function AdminQuickViewDialog({ product, open, onClose }: ViewProps) {
  if (!product) return null;

  const img    = product.images.find(i => i.isPrimary) ?? product.images[0];
  const imgSrc = resolveImgUrl(img?.url);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display:'flex', alignItems:'center', pr:4 }}>
        {product.name}
        <IconButton aria-label="close" onClick={onClose} sx={{ ml:'auto' }} size="small">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers sx={{ textAlign:'center' }}>
        <Box
          component="img"
          src={imgSrc}
          alt={img?.altText ?? product.name}
          sx={{ width:'100%', maxWidth:300, mx:'auto', mb:2 }}
        />

        <Stack direction="row" spacing={2} justifyContent="center" sx={{ mb:2 }}>
          {product.brand && (
            <Typography variant="subtitle1" color="text.secondary">
              {product.brand}
            </Typography>
          )}
          <Typography variant="h6">${product.price.toFixed(2)}</Typography>
        </Stack>

        {product.categoryName && (
          <Chip label={product.categoryName} size="small" color="primary" sx={{ mb:2 }} />
        )}

        {product.grams !== undefined && (
          <Typography variant="body2" sx={{ mb:2 }} color="text.secondary">
            {product.grams} g
          </Typography>
        )}

        {product.description && (
          <Typography variant="body2" sx={{ whiteSpace:'pre-line', textAlign:'left' }}>
            {product.description}
          </Typography>
        )}
      </DialogContent>
    </Dialog>
  );
}

/* Modal form (Add / Edit product)  */
interface EditDlgProps {
  open:        boolean;
  initial:     Product | null;
  categories:  Category[];
  onSave:      (p: Partial<Product> & { file?: File }) => void;
  onAddCat:    (name: string) => void;
  onClose:     () => void;
}

function EditProductDialog({
  open, onClose, onSave, onAddCat, categories, initial,
}: EditDlgProps) {
  const [sku,   setSku]   = useState('');
  const [name,  setName]  = useState('');
  const [brand, setBrand] = useState('');
  const [price, setPrice] = useState('');
  const [qty,   setQty]   = useState('0');
  const [grams, setGrams] = useState('0');          
  const [desc,  setDesc]  = useState('');
  const [catId, setCat]   = useState<string | number>('');
  const [file,  setFile]  = useState<File>();
  const [newCat, setNewCat] = useState('');

  /* preload on edit */
  useEffect(() => {
    if (initial) {
      setSku  (initial.sku);
      setName (initial.name);
      setBrand(initial.brand ?? '');
      setPrice(initial.price.toString());
      setQty  (String(initial.inventoryQty ?? 0));
      setGrams(String(initial.grams ?? 0));        
      setDesc (initial.description ?? '');
      setCat  (initial.categoryId ?? '');
    } else {
      setSku(''); setName(''); setBrand('');
      setPrice(''); setQty('0'); setGrams('0');
      setDesc(''); setCat('');
    }
    setFile(undefined);
    setNewCat('');
  }, [initial, open]);

  /* save */
  const handleSave = () => onSave({
    id:           initial?.id,
    sku,
    name,
    brand,
    price:        parseFloat(price),
    inventoryQty: parseInt(qty, 10) || 0,
    grams:        parseInt(grams, 10) || 0,        
    description:  desc,
    categoryId:   catId ? Number(catId) : undefined,
    file,
  });

  /* quick-add category */
  const handleAddCat = async () => {
    if (!newCat.trim()) return;
    await onAddCat(newCat.trim());
    setNewCat('');
  };

  /* UI */
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{initial ? 'Edit Product' : 'Add Product'}</DialogTitle>

      <DialogContent dividers>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="SKU"        value={sku}   onChange={e=>setSku(e.target.value)}  fullWidth />
          <TextField label="Name"       value={name}  onChange={e=>setName(e.target.value)} fullWidth />
          <TextField label="Brand"      value={brand} onChange={e=>setBrand(e.target.value)}fullWidth />
          <TextField
            label="Price"
            type="number"
            value={price}
            onChange={e=>setPrice(e.target.value)}
            InputProps={{ startAdornment: <InputAdornment position="start">$</InputAdornment> }}
            fullWidth
          />
          <TextField
            label="Stock (qty)"
            type="number"
            value={qty}
            onChange={e=>setQty(e.target.value)}
            fullWidth
          />
          <TextField                               
            label="Weight (grams)"
            type="number"
            value={grams}
            onChange={e=>setGrams(e.target.value)}
            fullWidth
          />
          <TextField
            label="Description"
            value={desc}
            onChange={e=>setDesc(e.target.value)}
            fullWidth
            multiline
            minRows={3}
          />

          <FormControl fullWidth>
            <InputLabel>Category</InputLabel>
            <Select
              value={catId}
              label="Category"
              onChange={e=>setCat(e.target.value)}
            >
              <MenuItem value=""><em>None</em></MenuItem>
              {categories.map(c=>(
                <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <Stack direction="row" spacing={1}>
            <TextField
              fullWidth
              size="small"
              label="New category"
              value={newCat}
              onChange={e=>setNewCat(e.target.value)}
            />
            <Button
              disabled={!newCat.trim()}
              onClick={handleAddCat}
              variant="contained"
              sx={{ whiteSpace:'nowrap' }}
            >
              Add
            </Button>
          </Stack>

          <Button component="label" variant="outlined">
            {file ? 'Change image' : 'Select image'}
            <input
              hidden
              type="file"
              accept="image/*"
              onChange={e=>setFile(e.target.files?.[0])}
            />
          </Button>
          {file && (
            <Typography variant="caption" sx={{ wordBreak:'break-all' }}>
              {file.name}
            </Typography>
          )}
        </Stack>
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          disabled={!name.trim() || !sku.trim() || !desc.trim() || !price}
          onClick={handleSave}
        >
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/* Audit Log (quick modal) */
function AuditLogDialog({
  open, onClose,
}: { open: boolean; onClose: () => void }) {
  const [items, setItems] = useState<ProductAudit[]>([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    (async () => {
      setLoading(true); setErr(null);
      try {
        const data = await fetchProductAudits(200);
        setItems(data);
      } catch (e: any) {
        setErr(e?.message || 'Failed to load audit entries.');
      } finally {
        setLoading(false);
      }
    })();
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>
        Product Audit Log
      </DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        {loading ? (
          <Box sx={{ textAlign:'center', py: 4 }}><CircularProgress /></Box>
        ) : items.length === 0 ? (
          <Typography variant="body2">No recent admin activity.</Typography>
        ) : (
          <List dense>
            {items.map(a => {
              const when = new Date(a.createdAt).toLocaleString();
              const head = `${a.action} • ${a.name ?? a.sku ?? '(deleted product)'} • ${when}`;
              let details: string | null = null;
              if (a.detailsJson && a.action === 'UPDATE') {
                details = a.detailsJson;
                try {
                  const parsed = JSON.parse(a.detailsJson);
                  details = Object.keys(parsed).length
                    ? Object.entries(parsed).map(([k, v]: any) =>
                        `${k}: ${JSON.stringify(v?.from)} → ${JSON.stringify(v?.to)}`
                      ).join('  |  ')
                    : null;
                } catch {
                  // fall back to raw JSON
                }
              }
              return (
                <ListItem key={a.id} sx={{ py: 1.2 }}>
                  <ListItemText
                    primary={head}
                    secondary={
                      <>
                        <span>SKU: {a.sku ?? '—'} • Product ID: {a.productId ?? '—'}</span>
                        {details && (
                          <>
                            <br />
                            <span style={{ opacity: 0.85 }}>Changes: {details}</span>
                          </>
                        )}
                      </>
                    }
                  />
                </ListItem>
              );
            })}
          </List>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

/* Category Manager (safe deletes) */
function CategoryManagerDialog({
  open, onClose, categories, onDeleted,
}: {
  open: boolean;
  onClose: () => void;
  categories: Category[];
  onDeleted: () => void;
}) {
  const [busyId, setBusy] = useState<number | null>(null);
  const [err, setErr] = useState<string | null>(null);

  const doDelete = async (id: number, name: string) => {
    if (!confirm(`Delete category "${name}"?\n\nProducts currently in this category will NOT be deleted.\nThey will simply become uncategorized.`)) {
      return;
    }
    setErr(null);
    setBusy(id);
    try {
      await apiDeleteCategory(id);
      await onDeleted();
    } catch (e: any) {
      setErr(e?.message || 'Failed to delete category.');
    } finally {
      setBusy(null);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Manage Categories</DialogTitle>
      <DialogContent dividers>
        <Alert severity="info" sx={{ mb: 2 }}>
          Deleting a category will <b>not</b> remove any products. Existing items are safely uncategorized.
        </Alert>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        {categories.length === 0 ? (
          <Typography variant="body2">No categories yet.</Typography>
        ) : (
          <List dense>
            {categories.map(c => (
              <ListItem key={c.id} sx={{ py: 1 }}>
                <ListItemText primary={c.name} />
                <ListItemSecondaryAction>
                  <Tooltip title="Delete category">
                    <span>
                      <IconButton
                        edge="end"
                        onClick={() => doDelete(c.id, c.name)}
                        disabled={busyId === c.id}
                        size="small"
                      >
                        {busyId === c.id ? <CircularProgress size={18} /> : <DeleteIcon fontSize="small" />}
                      </IconButton>
                    </span>
                  </Tooltip>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}

/*  Main component */
export default function AdminShop() {
  const { role } = useRole();

  /* filters & paging */
  const [query,  setQuery ] = useState('');
  const [categoryId, setCatId] = useState<number | null>(null);
  const [brand,  setBrand ]   = useState('ANY');
  const [price,  setPrice ]   = useState('ANY');

  const [perPage, setPerPage] = useState<number>(PAGE_OPTS[0]);
  const [page,    setPage   ] = useState(0);

  /* data */
  const [products,   setProds] = useState<Product[]>([]);
  const [categories, setCats]  = useState<Category[]>([]);
  const [loading,    setLoad]  = useState(false);

  /* dialogs */
  const [dlgOpen, setDlg]  = useState(false);
  const [editing, setEdit] = useState<Product | null>(null);
  const [viewing, setViewing] = useState<Product | null>(null);

  /* dialogs */
  const [auditOpen, setAuditOpen] = useState(false);
  const [catMgrOpen, setCatMgrOpen] = useState(false);

  /* fetch helpers  */
  const pullCategories = async () => setCats(await fetchCategories());

  const pullProducts = async () => {
    setLoad(true);
    try {
      const res = await fetchProducts('', categoryId ?? undefined, 0, 1000);
      setProds(res?.content ?? []);
      setPage(0);
    } finally { setLoad(false); }
  };

  useEffect(() => { pullCategories(); pullProducts(); }, []);
  useEffect(() => { pullProducts(); /* eslint-disable-line react-hooks/exhaustive-deps */ }, [categoryId]);

  /* brand list */
  const brandOptions = useMemo(() => {
    const s = new Set<string>();
    products.forEach(p => p.brand && s.add(p.brand));
    return [...s].sort();
  }, [products]);

  /* unified filter */
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return products.filter(p => {
      const okQ = !q || (
        p.name.toLowerCase().includes(q) ||
        (p.brand ?? '').toLowerCase().includes(q) ||
        (p.categoryName ?? '').toLowerCase().includes(q)
      );
      const okB = brand === 'ANY' ||
                  (p.brand ?? '').toLowerCase() === brand.toLowerCase();
      const okP = (() => {
        switch (price) {
          case '<50':    return p.price < 50;
          case '50-100': return p.price >= 50 && p.price < 100;
          case '100-150':return p.price >= 100 && p.price < 150;
          case '>150':   return p.price >= 150;
          default:       return true;
        }
      })();
      const okC = categoryId === null || p.categoryId === categoryId;
      return okQ && okB && okP && okC;
    });
  }, [products, query, brand, price, categoryId]);

  /* paging */
  const pageCount = Math.max(1, Math.ceil(filtered.length / perPage));
  const shown     = filtered.slice(page * perPage, (page + 1) * perPage);

  /* CRUD wrappers  */
  const saveProduct = async (data: Partial<Product> & { file?: File }) => {
    const { file, ...rest } = data;

    // save (create or update)
    const saved = rest.id
      ? await updateProduct(rest.id, rest)
      : await createProduct(rest);

    if (!saved) throw new Error('Save failed: empty response from server.');

    // upload primary image if provided
    if (file) {
      await uploadProductImage(saved.id, file, true);
    }

    await pullProducts();
    setDlg(false);
  };

  const deleteProd = async (id: number) => {
    if (!confirm('Delete this product?\n\nThis operation is permanent, but past orders remain intact.')) return;
    await deleteProduct(id);
    await pullProducts();
  };

  const addCategory = async (name: string) => {
    await createCategory(name);
    pullCategories();
  };

  /* auth gate */
  if (role !== 'OWNER' && role !== 'ADMIN') {
    return (
      <Container sx={{ py:6 }}>
        <Typography variant="h4">Access denied</Typography>
      </Container>
    );
  }

  /*  UI  */
  return (
    <Box sx={{ width:'100%', minHeight:'100vh', background:BG_GRADIENT, pt:6, pb:8 }}>
      <Container maxWidth="lg">
         <Typography variant="h3" color="common.white" align="center" sx={{ color: 'common.white', fontWeight: 700, marginBottom: 3 }}>
              Manage Pro Shop Products
          </Typography>
        {/* header */}
        <Box
          sx={{
            display:'flex',
            justifyContent:'space-evenly',   
            alignItems:'flex-start',
            mb:3,
            flexWrap:'wrap',
            gap: 2,
          }}
        >
          <Box sx={{ mb:{ xs:1, md:0 } }}>
            <Typography variant="h6" color="common.white">
              Add, edit, or retire products in real time.
            </Typography>
          </Box>

          <Stack direction="row" spacing={1.5}>
            <Button
              color="inherit"
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={() => { setEdit(null); setDlg(true); }}
              sx={{
                alignSelf:{ xs:'stretch', md:'center' },
                borderRadius: 9999,
                px: 2.5,
                py: 1.25,
                fontWeight: 500,
                minWidth: 160,
                color: 'common.white'
              }}
            >
              Add New Product
            </Button>

            {/* open category manager */}
            <Button
              variant="outlined"
              color="inherit"
              startIcon={<CategoryIcon />}
              onClick={() => setCatMgrOpen(true)}
              sx={{ borderRadius: 9999, px: 2.5, py: 1.25, fontWeight: 500, color: 'common.white' }}
            >
              Manage Categories
            </Button>

            {/* open audit modal */}
            <Button
              variant="outlined"
              color="inherit"
              startIcon={<ListAltIcon />}
              onClick={() => setAuditOpen(true)}
              sx={{ borderRadius: 9999, px: 2.5, py: 1.25, fontWeight: 500, color: 'common.white' }}
            >
              Audit Log
            </Button>
          </Stack>
        </Box>

        {/* search */}
        <TextField
          fullWidth
          size="small"
          placeholder="Search products…"
          value={query}
          onChange={(e: ChangeEvent<HTMLInputElement>) => setQuery(e.target.value)}
          sx={{ mb:2, background:'white', borderRadius:1 }}
        />

        {/* filter row */}
        <Stack
          direction={{ xs:'column', sm:'row' }}
          spacing={2}
          sx={{ mb:2, justifyContent:'center' }}
        >
          <FormControl size="small" sx={{ ...selectorSx, width:{ xs:'100%', sm:200 } }}>
            <Select
              value={brand}
              onChange={e=>{ setBrand(e.target.value); setPage(0); }}
              displayEmpty
            >
              <MenuItem value="ANY">All Brands</MenuItem>
              {brandOptions.map(b=>(
                <MenuItem key={b} value={b}>{b}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ ...selectorSx, width:{ xs:'100%', sm:200 } }}>
            <Select
              value={price}
              onChange={e=>{ setPrice(e.target.value); setPage(0); }}
              displayEmpty
            >
              <MenuItem value="ANY">Any Price</MenuItem>
              <MenuItem value="<50">Below $50</MenuItem>
              <MenuItem value="50-100">$50 - $100</MenuItem>
              <MenuItem value="100-150">$100 - $150</MenuItem>
              <MenuItem value=">150">Above $150</MenuItem>
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ ...selectorSx, width:{ xs:'100%', sm:150 } }}>
            <Select
              value={perPage}
              onChange={e=>{ setPerPage(Number(e.target.value)); setPage(0); }}
            >
              {PAGE_OPTS.map(n=>(
                <MenuItem key={n} value={n}>{n} items / page</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Stack>

        {/* horizontal chips */}
        <Paper sx={{ p:2, mb:3 }}>
          <CategoryChips
            horizontal
            categories={categories}
            selectedId={categoryId}
            onChange={id=>{ setCatId(id); setPage(0); }}
          />
        </Paper>

        {/* product grid */}
        {loading ? (
          <Box sx={{ textAlign:'center', py:10}}><CircularProgress /></Box>
        ) : shown.length === 0 ? (
          <Typography color="common.white">No products found.</Typography>
        ) : (
          <>
            <Box
              sx={{
                display:'grid',
                gap:3,
                rowGap:8,
                justifyContent:'center',
                alignItems: 'center',
                gridTemplateColumns:'repeat(auto-fill,minmax(220px,1fr))',
              }}
            >
              {shown.map(p => (
                <Paper key={p.id} sx={{ position:'relative', width:220 }}>
                  <AdminProductCard
                    product={p}
                    onClick={()=>setViewing(p)}
                  />
                  <Box sx={{ position:'absolute', top:8, right:8 }}>
                    <IconButton
                      size="small"
                      onClick={e=>{ e.stopPropagation(); setEdit(p); setDlg(true); }}
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={e=>{ e.stopPropagation(); deleteProd(p.id); }}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </Paper>
              ))}
            </Box>

            <Box sx={{ display:'flex', justifyContent:'center', mt:6 }}>
              <Pagination
                page={page+1}
                count={pageCount}
                onChange={(_, p)=>setPage(p-1)}
                color="primary"
              />
            </Box>
          </>
        )}

        {/* add / edit modal */}
        <EditProductDialog
          open={dlgOpen}
          onClose={()=>setDlg(false)}
          onSave={saveProduct}
          onAddCat={addCategory}
          categories={categories}
          initial={editing}
        />

        {/* read-only quick-view modal */}
        <AdminQuickViewDialog
          product={viewing}
          open={!!viewing}
          onClose={()=>setViewing(null)}
        />

        {/* audit log modal */}
        <AuditLogDialog open={auditOpen} onClose={() => setAuditOpen(false)} />

        {/* category manager modal */}
        <CategoryManagerDialog
          open={catMgrOpen}
          onClose={() => setCatMgrOpen(false)}
          categories={categories}
          onDeleted={async () => { await pullCategories(); await pullProducts(); }}
        />
      </Container>
    </Box>
  );
}
