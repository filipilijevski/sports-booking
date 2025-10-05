/* Shop.tsx - public storefront */

import {
  Box, Container, TextField, Stack, FormControl, Select, MenuItem,
  Typography, CircularProgress, Pagination, Paper, Dialog, DialogTitle,
  DialogContent, DialogActions, Button, IconButton, Chip,
} from '@mui/material';
import CloseIcon           from '@mui/icons-material/Close';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import { type ChangeEvent, useEffect, useMemo, useState } from 'react';

import { fetchProducts, type Product }   from '../lib/products';
import { fetchCategories, type Category } from '../lib/categories';
import CategoryChips   from '../components/CategoryChips';
import ProductCard     from '../components/ProductCard';
import { useCart }     from '../context/CartContext';
import { resolveImgUrl } from '../lib/assets';

const BG_GRADIENT =
  'linear-gradient(270deg,rgba(0,54,126,1) 0%,rgba(181,94,231,1) 100%)';

const selectorSx = {
  background:'white',
  borderRadius:1,
  '& .MuiOutlinedInput-notchedOutline': { borderColor:'transparent' },
  '&:hover .MuiOutlinedInput-notchedOutline': { borderColor:'transparent' },
};

const PAGE_OPTS = [10, 20, 50];

/* Quick-view modal (client)  */
interface ViewProps {
  product: Product | null;
  open: boolean;
  onClose: () => void;
}
function QuickViewDialog({ product, open, onClose }: ViewProps) {
  const { add, openDrawer } = useCart();
  const [qty, setQty] = useState(1);

  if (!product) return null;

  const img = product.images.find(i => i.isPrimary) ?? product.images[0];
  const imgSrc = resolveImgUrl(img?.url);

  const handleAdd = async () => {
    await add(product.id, qty, product);
    openDrawer();
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display:'flex', alignItems:'center', pr:4 }}>
        {product.name}
        <IconButton
          aria-label="close"
          onClick={onClose}
          sx={{ ml:'auto' }}
          size="small"
        >
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

        <Stack
          direction="row"
          spacing={2}
          justifyContent="center"
          alignItems="center"
          sx={{ mb:2 }}
        >
          {product.brand && (
            <Typography variant="subtitle1" color="text.secondary">
              {product.brand}
            </Typography>
          )}
          <Typography variant="h6">
            ${product.price.toFixed(2)}
          </Typography>
        </Stack>

        {product.categoryName && (
          <Chip
            label={product.categoryName}
            size="small"
            color="primary"
            sx={{ mb:2 }}
          />
        )}

        {product.description && (
          <Typography
            variant="body2"
            sx={{ whiteSpace:'pre-line', textAlign:'left', mb:3 }}
          >
            {product.description}
          </Typography>
        )}

        {/* qty selector is simple for now; increment buttons can be added later */}
        <Stack
          direction="row"
          spacing={2}
          justifyContent="center"
          alignItems="center"
          sx={{ mb:2 }}
        >
          <Typography>Qty:</Typography>
          <TextField
            size="small"
            type="number"
            value={qty}
            inputProps={{ min:1, style:{ textAlign:'center', width:60 } }}
            onChange={e=>setQty(Math.max(1, Number(e.target.value)))}
          />
        </Stack>
      </DialogContent>

      <DialogActions>
        <Button
          variant="contained"
          startIcon={<AddShoppingCartIcon />}
          onClick={handleAdd}
          disabled={product.inventoryQty !== undefined && product.inventoryQty < 1}
        >
          Add to Cart
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/* Main page */
export default function Shop() {
  /* state */
  const [query, setQuery] = useState('');
  const [categoryId, setCatId] = useState<number | null>(null);
  const [apiProds, setApiProds] = useState<Product[]>([]);
  const [categories, setCats]   = useState<Category[]>([]);
  const [loading, setLoading]   = useState(false);

  const [brand, setBrand]   = useState('ANY');
  const [price, setPrice]   = useState('ANY');
  const [perPage, setPerPage] = useState(PAGE_OPTS[0]);
  const [page, setPage] = useState(0);

  /* quick-view */
  const [viewing, setViewing] = useState<Product | null>(null);

  /* fetches */
  useEffect(() => { (async()=>setCats(await fetchCategories()))(); }, []);
  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const res = await fetchProducts('', categoryId ?? undefined, 0, 1000);
        setApiProds(res?.content ?? []);
        setPage(0);
      } finally { setLoading(false); }
    })();
  }, [categoryId]);

  /* helpers */
  const brandOptions = useMemo(() => {
    const s = new Set<string>();
    apiProds.forEach(p => p.brand && s.add(p.brand));
    return [...s].sort();
  }, [apiProds]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return apiProds.filter(p => {
      const okQ = !q || (
        p.name.toLowerCase().includes(q) ||
        (p.brand ?? '').toLowerCase().includes(q) ||
        (p.categoryName ?? '').toLowerCase().includes(q)
      );
      const okB = brand === 'ANY' || (p.brand ?? '').toLowerCase() === brand.toLowerCase();
      const okP = (() => {
        switch (price) {
          case '<50': return p.price < 50;
          case '50-100': return p.price >= 50 && p.price < 100;
          case '100-150': return p.price >= 100 && p.price < 150;
          case '>150': return p.price >= 150;
          default: return true;
        }
      })();
      return okQ && okB && okP;
    });
  }, [apiProds, query, brand, price]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / perPage));
  const shown     = filtered.slice(page * perPage, (page + 1) * perPage);

  return (
    <Box sx={{ width:'100%', minHeight:'100vh', background:BG_GRADIENT, pt:6, pb:8 }}>
      <Container maxWidth="lg">
        {/* masthead */}
        <Typography variant="h1" align="center" color="common.white" gutterBottom>
           Pro Shop
        </Typography>
        <Typography variant="h6" align="center" color="common.white" sx={{ mb:4 }}>
          Hand-picked gear to unleash your best game.
        </Typography>

        {/* search */}
        <TextField
          fullWidth size="small" placeholder="Search products…"
          value={query} onChange={(e: ChangeEvent<HTMLInputElement>)=>setQuery(e.target.value)}
          sx={{ mb:2, background:'white', borderRadius:1 }}
        />

        {/* filter bar */}
        <Stack direction={{ xs:'column', sm:'row' }} spacing={2} sx={{ mb:2, justifyContent:'center' }}>
          <FormControl size="small" sx={{ ...selectorSx, width:{ xs:'100%', sm:200 } }}>
            <Select value={brand} onChange={e=>{ setBrand(e.target.value); setPage(0); }} displayEmpty>
              <MenuItem value="ANY">All Brands</MenuItem>
              {brandOptions.map(b=>(<MenuItem key={b} value={b}>{b}</MenuItem>))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ ...selectorSx, width:{ xs:'100%', sm:200 } }}>
            <Select value={price} onChange={e=>{ setPrice(e.target.value); setPage(0); }} displayEmpty>
              <MenuItem value="ANY">Any Price</MenuItem>
              <MenuItem value="<50">Below $50</MenuItem>
              <MenuItem value="50-100">$50-$100</MenuItem>
              <MenuItem value="100-150">$100-$150</MenuItem>
              <MenuItem value=">150">Above $150</MenuItem>
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ ...selectorSx, width:{ xs:'100%', sm:120 } }}>
            <Select value={perPage} onChange={e=>{ setPerPage(Number(e.target.value)); setPage(0); }}>
              {PAGE_OPTS.map(n=>(<MenuItem key={n} value={n}>{n} / page</MenuItem>))}
            </Select>
          </FormControl>
        </Stack>

        {/* category chips */}
        <Paper sx={{ p:2, mb:3 }}>
          <CategoryChips
            horizontal
            categories={categories}
            selectedId={categoryId}
            onChange={id=>{ setCatId(id); setPage(0); }}
          />
        </Paper>

        {/* products */}
        {loading ? (
          <Box sx={{ textAlign:'center', py:10 }}><CircularProgress /></Box>
        ) : shown.length === 0 ? (
          <Typography color="common.white">No products found.</Typography>
        ) : (
          <>
            <Box
              sx={{
                display:'grid',
                gap:2,
                rowGap: 8,
                justifyContent:'center',
                gridTemplateColumns:'repeat(auto-fill,minmax(220px,1fr))',
              }}
            >
              {shown.map(p => (
                <ProductCard
                  key={p.id}
                  product={p}
                  onClick={() => setViewing(p)}
                />
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

        {/* quick-view modal */}
        <QuickViewDialog
          product={viewing}
          open={!!viewing}
          onClose={()=>setViewing(null)}
        />
      </Container>
    </Box>
  );
}
