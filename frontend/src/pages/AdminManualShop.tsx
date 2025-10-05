/* src/pages/AdminManualShop.tsx */
import {
  Box, Container, Typography, Paper, TextField, Stack, Button, Divider,
  MenuItem, Chip, IconButton, CircularProgress, Dialog, DialogTitle,
  DialogContent, DialogActions, Tooltip,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import DeleteIcon from '@mui/icons-material/Delete';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import { useEffect, useMemo, useState } from 'react';
import { useRole } from '../context/RoleContext';
import { useCart } from '../context/CartContext';
import { fetchProducts, type Product } from '../lib/products';
import { fetchCategories, type Category } from '../lib/categories';
import CategoryChips from '../components/CategoryChips';
import ProductCard from '../components/ProductCard';
import ShippingAddressForm from '../components/ShippingAddressForm';
import type { ShippingAddress } from '../types';
import {
  manualCheckout,
  type ManualCheckoutPayload,
  type OrderDto,
  type OfflinePaymentMethod,
} from '../lib/manualOrders';
import { searchAdminUsers, type AdminUser } from '../lib/adminUsers';
import { adminCanUseCoupon } from '../lib/coupons';  

/* gradient used site-wide */
const BG = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

const PAYMENT_OPTS: OfflinePaymentMethod[] = ['CASH', 'ETRANSFER', 'TERMINAL', 'OTHER'];

/** Helper: round to 2 decimals */
function round2(n: number) { return Math.round(n * 100) / 100; }

export default function AdminManualShop() {
  const { role } = useRole();
  const { state: cartState, update, remove, add } = useCart();

  const isAdmin = role === 'OWNER' || role === 'ADMIN';
  if (!isAdmin) {
    return (
      <Container sx={{ py:6 }}>
        <Typography variant="h4">Access denied</Typography>
      </Container>
    );
  }

  /* product browsing */
  const [query, setQuery] = useState('');
  const [categoryId, setCatId] = useState<number | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCats]   = useState<Category[]>([]);
  const [loading, setLoading]   = useState(false);

  useEffect(() => { (async()=>setCats(await fetchCategories()))(); }, []);
  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const res = await fetchProducts('', categoryId ?? undefined, 0, 1000);
        setProducts(res?.content ?? []);
      } finally { setLoading(false); }
    })();
  }, [categoryId]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return products.filter(p =>
      !q ||
      p.name.toLowerCase().includes(q) ||
      (p.brand ?? '').toLowerCase().includes(q) ||
      (p.categoryName ?? '').toLowerCase().includes(q)
    );
  }, [products, query]);

  /* manual checkout form */
  const [fullName, setFullName] = useState('');
  const [email,    setEmail]    = useState('');
  const [phone,    setPhone]    = useState('');
  const [address,  setAddr]     = useState<ShippingAddress>({
    fullName:'', phone:'', email:'', line1:'', line2:'',
    city:'', province:'', postalCode:'', country:'Canada',
  });
  const [hasShipping, setHasShipping] = useState(false);
  const [shippingFee, setShipFee]     = useState<string>('0');
  const [payMethod,   setPayMethod]   = useState<OfflinePaymentMethod>('CASH');
  const [coupon,      setCoupon]      = useState('');
  const [assocQ,      setAssocQ]      = useState('');
  const [assocUser,   setAssocUser]   = useState<AdminUser | null>(null);
  const [searching,   setSearching]   = useState(false);
  const [matches,     setMatches]     = useState<AdminUser[]>([]);
  const [placing,     setPlacing]     = useState(false);
  const [placed,      setPlaced]      = useState<OrderDto | null>(null);

  // coupon preview state
  const [checkingCoupon, setCheckingCoupon] = useState(false);
  const [couponApplied, setCouponApplied] = useState(false);
  const [couponInfo, setCouponInfo] = useState<{
    valid: boolean;
    active: boolean;
    alreadyUsed: boolean;
    percentOff?: number | null;
    amountOff?: number | null;
    minSpend?: number | null;
  } | null>(null);

  // confirmation dialog
  const [confirmOpen, setConfirmOpen] = useState(false);

  const doSearchUser = async () => {
    if (!assocQ.trim()) return;
    setSearching(true);
    try {
      const res = await searchAdminUsers(assocQ.trim(), 0, 5);
      setMatches(res.content || []);
    } finally { setSearching(false); }
  };

  const subtotal = cartState.subtotal;
  const shipN = parseFloat(shippingFee || '0') || 0;
  const estTax = (subtotal + shipN) * 0.13;

  // Estimated discount if coupon applied and valid
  const estDiscount = useMemo(() => {
    if (!couponApplied || !couponInfo || !couponInfo.valid) return 0;
    const base = subtotal + shipN;
    const tax  = base * 0.13;
    const discountBase = base + tax; // POST_TAX base (matches server)
    if (couponInfo.percentOff != null) {
      return round2(discountBase * Number(couponInfo.percentOff));
    }
    if (couponInfo.amountOff != null) {
      return round2(Math.min(discountBase, Number(couponInfo.amountOff)));
    }
    return 0;
  }, [couponApplied, couponInfo, subtotal, shipN]);

  const estTotal = round2(subtotal + shipN + estTax - estDiscount);

  const canPlace =
    fullName.trim().length > 0 &&
    email.trim().length > 0 &&
    phone.trim().length > 0 &&
    cartState.items.length > 0;

  const tryApplyCoupon = async () => {
    const raw = coupon.trim().toUpperCase();
    if (!raw) {
      setCoupon('');
      setCouponInfo(null);
      setCouponApplied(false);
      return;
    }
    if (!assocUser?.id) {
      alert('Select an existing user before applying a coupon.');
      return;
    }
    setCheckingCoupon(true);
    try {
      const res = await adminCanUseCoupon(raw, assocUser.id);
      setCoupon(raw);
      setCouponInfo({
        valid: Boolean(res.valid),
        active: Boolean(res.active),
        alreadyUsed: Boolean(res.alreadyUsed),
        percentOff: res.percentOff != null ? Number(res.percentOff) : null,
        amountOff:  res.amountOff  != null ? Number(res.amountOff)  : null,
        minSpend:   res.minSpend   != null ? Number(res.minSpend)   : null,
      });
      setCouponApplied(Boolean(res.valid));
      if (!res.valid) {
        const why = res.alreadyUsed ? 'Coupon already used by this user.' :
                    !res.active     ? 'Coupon is not active.' :
                                      'Invalid coupon.';
        alert(why);
      }
    } catch (e: any) {
      alert(e?.message || 'Unable to validate coupon right now.');
    } finally {
      setCheckingCoupon(false);
    }
  };

  const clearCoupon = () => {
    setCoupon('');
    setCouponInfo(null);
    setCouponApplied(false);
  };

  const openConfirm = () => {
    if (!canPlace) return;
    if (coupon.trim() && !assocUser?.id) {
      alert('To use a coupon, please associate an existing user first.');
      return;
    }
    setConfirmOpen(true);
  };

  const placeOrder = async () => {
    if (!canPlace) return;
    setPlacing(true);
    try {
      const shippingAddr: ShippingAddress = hasShipping
        ? {
            ...address,
            fullName: address.fullName || fullName.trim(),
            phone:    address.phone    || phone.trim(),
            email:    address.email    || email.trim(),
          }
        : {
            fullName: fullName.trim(),
            phone:    phone.trim(),
            email:    email.trim(),
            line1:    '',
            line2:    '',
            city:     '',
            province: '',
            postalCode: '',
            country:  'Canada',
          };

      const payload: ManualCheckoutPayload = {
        clientFullName: fullName.trim(),
        clientEmail:    email.trim(),
        clientPhone:    phone.trim(),
        clientUserId:   assocUser?.id ?? null,
        shippingAddress: shippingAddr,
        shippingFee:    shippingFee !== '' ? (parseFloat(shippingFee) || 0) : 0,
        paymentMethod:  payMethod,
        couponCode:     coupon.trim() ? coupon.trim().toUpperCase() : null,
      };

      const order = await manualCheckout(payload);
      setPlaced(order);
    } catch (e: any) {
      alert(e?.message || 'Manual checkout failed');
    } finally { setPlacing(false); setConfirmOpen(false); }
  };

  const handleCloseSuccess = () => {
    setPlaced(null);
    window.location.reload();
  };

  return (
    <Box sx={{ width:'100%', minHeight:'100vh', background:BG, pt:6, pb:8 }}>
      <Container maxWidth="xl">
        <Typography variant="h3" align="center" sx={{ fontWeight: 700, color: 'common.white', py: 3 }}>
          Manual In-Person Checkout
        </Typography>

        {/* Search and Categories */}
        <Paper sx={{ p: 2, mb: 2 }}>
          <Stack direction={{ xs:'column', sm:'row' }} spacing={2} alignItems="center">
            <TextField
              fullWidth
              size="small"
              placeholder="Search products…"
              value={query}
              onChange={e => setQuery(e.target.value)}
            />
            <Button startIcon={<SearchIcon />}>Search</Button>
          </Stack>
        </Paper>
        <Paper sx={{ p:2, mb:2 }}>
          <CategoryChips
            horizontal
            categories={categories}
            selectedId={categoryId}
            onChange={(id)=>setCatId(id)}
          />
        </Paper>

        {/* Product Grid */}
        {loading ? (
          <Box sx={{ textAlign:'center', py:6 }}><CircularProgress /></Box>
        ) : (
          <Box
            sx={{
              display:'grid',
              gap:2, rowGap: 6,
              gridTemplateColumns: {
                xs: 'repeat(2, minmax(220px, 1fr))',
                sm: 'repeat(3, minmax(220px, 1fr))',
                md: 'repeat(3, minmax(220px, 1fr))',
                lg: 'repeat(5, minmax(220px, 1fr))',
              },
              justifyItems: 'center',
              justifyContent: 'center',
            }}
          >
            {filtered.map(p => (
              <ProductCard key={p.id} product={p} onClick={() => add(p.id, 1, p)} />
            ))}
          </Box>
        )}

        {/* Checkout Panel */}
        <Box sx={{ width: '100%', mt: 10 }}>
          <Paper sx={{ p:2, maxWidth: 720, mx:'auto' }}>
            <Typography variant="h6" gutterBottom>Customer Details</Typography>
            <Stack spacing={2}>
              <TextField label="Full Name" value={fullName} onChange={e=>setFullName(e.target.value)} required />
              <TextField label="Email"     value={email}    onChange={e=>setEmail(e.target.value)}     required />
              <TextField label="Phone"     value={phone}    onChange={e=>setPhone(e.target.value)}     required />
            </Stack>

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle1">Associate with existing user (optional)</Typography>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ my:1 }}>
              <TextField
                size="small" fullWidth
                placeholder="Search by email or name"
                value={assocQ} onChange={e=>setAssocQ(e.target.value)}
              />
              <Button variant="outlined" onClick={doSearchUser} disabled={searching}>
                {searching ? 'Searching…' : 'Search'}
              </Button>
            </Stack>
            <Stack direction="row" spacing={1} flexWrap="wrap">
              {matches.map(u => (
                <Chip
                  key={u.id}
                  color={assocUser?.id === u.id ? 'primary' : 'default'}
                  onClick={() => setAssocUser(u)}
                  label={`${u.firstName ?? ''} ${u.lastName ?? ''} ${u.email ? `• ${u.email}` : ''}`}
                  sx={{ mb: 1 }}
                />
              ))}
              {assocUser && (
                <Chip
                  color="secondary"
                  label={`Selected: ${assocUser.firstName ?? ''} ${assocUser.lastName ?? ''}`}
                  onDelete={() => setAssocUser(null)}
                  sx={{ mb: 1 }}
                />
              )}
            </Stack>

            <Divider sx={{ my: 2 }} />

            <Typography variant="h6" gutterBottom>Cart</Typography>
            <Stack spacing={1}>
              {cartState.items.map(li => (
                <Stack
                  key={li.id}
                  direction="row"
                  alignItems="center"
                  justifyContent="space-between"
                  sx={{ border: '1px solid #eee', borderRadius: 1, p: 1 }}
                >
                  <Typography variant="body2" sx={{ mr: 1, overflowWrap: 'anywhere' }}>
                    {li.quantity} × {li.product.name}
                  </Typography>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      ${(li.unitPrice * li.quantity).toFixed(2)}
                    </Typography>
                    <IconButton size="small" onClick={() => remove(li.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                </Stack>
              ))}
              {cartState.items.length === 0 && <Typography color="text.secondary">Cart is empty.</Typography>}
            </Stack>

            <Divider sx={{ my: 2 }} />

            <Typography variant="h6" gutterBottom>Payment & Shipping</Typography>
            <Stack spacing={2}>
              <TextField
                select fullWidth label="Payment Method" value={payMethod}
                onChange={e=>setPayMethod(e.target.value as OfflinePaymentMethod)}
              >
                {PAYMENT_OPTS.map(p => (<MenuItem key={p} value={p}>{p}</MenuItem>))}
              </TextField>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <TextField
                  fullWidth label="Coupon Code (optional)"
                  value={coupon} onChange={e=>setCoupon(e.target.value.toUpperCase())}
                  inputProps={{ style: { textTransform: 'uppercase' } }}
                />
                <Button variant="outlined" onClick={tryApplyCoupon} disabled={checkingCoupon}>
                  {checkingCoupon ? 'Checking…' : 'Apply'}
                </Button>
                {couponApplied && (
                  <Button variant="text" onClick={clearCoupon}>
                    Clear
                  </Button>
                )}
              </Stack>

              <TextField
                fullWidth label="Shipping Fee (optional)"
                type="number" value={shippingFee}
                onChange={e=>setShipFee(e.target.value)}
                helperText="Leave 0 for pickup or in-club delivery"
              />

              <Button
                variant={hasShipping ? 'outlined' : 'contained'}
                onClick={()=>setHasShipping(v=>!v)}
              >
                {hasShipping ? 'Remove Shipping Address' : 'Add Shipping Address'}
              </Button>

              {hasShipping && (
                <Paper variant="outlined" sx={{ p: 1 }}>
                  <ShippingAddressForm value={address} onChange={setAddr} />
                </Paper>
              )}
            </Stack>

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle1" gutterBottom>Estimated Totals</Typography>
            <Stack spacing={0.5}>
              <Typography variant="body2"><b>Subtotal:</b> ${subtotal.toFixed(2)}</Typography>
              <Typography variant="body2"><b>Shipping:</b> {shipN.toFixed(2)}</Typography>
              <Typography variant="body2"><b>Tax (13% est.):</b> ${estTax.toFixed(2)}</Typography>
              {couponApplied && couponInfo?.valid && (
                <>
                  <Typography variant="body2"><b>Coupon:</b> {coupon}</Typography>
                  <Typography variant="body2"><b>Discount (est.):</b> -${estDiscount.toFixed(2)}</Typography>
                </>
              )}
              <Typography variant="h6"><b>Grand Total (est.):</b> ${estTotal.toFixed(2)}</Typography>
              <Typography variant="caption" color="text.secondary">
                Final amounts are calculated by the backend at checkout.
              </Typography>
            </Stack>

            <Divider sx={{ my: 2 }} />

            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Tooltip title={canPlace ? '' : 'Fill mandatory fields and add items to cart'}>
                <span>
                  <Button
                    variant="contained"
                    startIcon={<AddShoppingCartIcon />}
                    onClick={openConfirm}
                    disabled={!canPlace || placing}
                  >
                    Review & Place Order
                  </Button>
                </span>
              </Tooltip>
            </Stack>
          </Paper>
        </Box>

        {/* Confirm dialog */}
        <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Confirm Manual Order</DialogTitle>
          <DialogContent dividers>
            <Stack spacing={1}>
              <Typography><b>Buyer:</b> {fullName} • {email} • {phone}</Typography>
              {assocUser && (
                <Typography><b>Associated user:</b> {assocUser.firstName} {assocUser.lastName} ({assocUser.email})</Typography>
              )}
              <Divider sx={{ my: 1 }} />
              <Typography><b>Subtotal:</b> ${subtotal.toFixed(2)}</Typography>
              <Typography><b>Shipping:</b> ${shipN.toFixed(2)}</Typography>
              <Typography><b>Tax (est.):</b> ${estTax.toFixed(2)}</Typography>
              {couponApplied && couponInfo?.valid && (
                <>
                  <Typography><b>Coupon:</b> {coupon}</Typography>
                  <Typography><b>Discount (est.):</b> -${estDiscount.toFixed(2)}</Typography>
                </>
              )}
              <Typography variant="h6"><b>Total (est.):</b> ${estTotal.toFixed(2)}</Typography>
              <Typography variant="caption" color="text.secondary">
                The backend will validate and compute the final amounts precisely.
              </Typography>
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setConfirmOpen(false)} disabled={placing}>Cancel</Button>
            <Button variant="contained" onClick={placeOrder} disabled={placing}>
              {placing ? 'Placing…' : 'Place Order'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Success dialog */}
        <Dialog open={!!placed} onClose={handleCloseSuccess} maxWidth="sm" fullWidth>
          <DialogTitle>Order Placed</DialogTitle>
          <DialogContent dividers>
            {placed ? (
              <Stack spacing={1}>
                <Typography><b>Order ID:</b> #{placed.id}</Typography>
                <Typography><b>Status:</b> {placed.status}</Typography>
                <Typography><b>Origin:</b> {placed.origin ?? (placed.stripePaymentIntentId ? 'ONLINE' : 'IN_PERSON')}</Typography>
                {placed.offlinePaymentMethod && (
                  <Typography><b>Payment:</b> {placed.offlinePaymentMethod}</Typography>
                )}
                {placed.couponCode && (
                  <Typography><b>Coupon:</b> {placed.couponCode}</Typography>
                )}
                <Divider sx={{ my: 1 }} />
                <Typography><b>Subtotal:</b> ${Number(placed.subtotalAmount).toFixed(2)}</Typography>
                <Typography><b>Shipping:</b> ${Number(placed.shippingAmount).toFixed(2)}</Typography>
                <Typography><b>Tax:</b> ${Number(placed.taxAmount).toFixed(2)}</Typography>
                {Number(placed.discountAmount) > 0 && (
                  <Typography><b>Discount:</b> -${Number(placed.discountAmount).toFixed(2)}</Typography>
                )}
                <Typography variant="h6"><b>Total:</b> ${Number(placed.totalAmount).toFixed(2)}</Typography>
              </Stack>
            ) : null}
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseSuccess}>Close</Button>
          </DialogActions>
        </Dialog>
      </Container>
    </Box>
  );
}
