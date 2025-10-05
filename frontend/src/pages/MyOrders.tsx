import {
  Box, Container, Typography, Paper, Stack, TextField,
  MenuItem, Button, Chip, Divider, CircularProgress, IconButton, Pagination,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import SearchIcon from '@mui/icons-material/Search';
import { useEffect, useState } from 'react';

/* theme bits */
const INFO_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

/* types (mirrored from backend DTOs) */

type Money = string | number;

type ProductDto = {
  id: number;
  sku: string;
  name: string;
  price: Money;
  brand?: string;
};

type OrderItemDto = {
  id: number;
  product: ProductDto;
  quantity: number;
  unitPrice: Money;
  totalPrice: Money;
};

type ShippingAddressDto = {
  fullName?: string;
  phone?: string;
  email?: string;
  line1?: string;
  line2?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  country?: string;
};

type RefundLineSummaryDto = {
  orderItemId: number;
  quantity: number;
};

type RefundEventDto = {
  id: number;
  provider: 'STRIPE' | 'OFFLINE' | string;
  providerTxnId?: string | null;
  amount: Money;
  currency: string;
  status?: string | null;
  reason?: string | null;
  includesShipping: boolean;
  shippingAmount: Money;
  createdAt?: string;
  lines: RefundLineSummaryDto[] | null | undefined;
};

type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'FULFILLED' | 'CANCELLED' | 'REFUNDED';
type Origin = 'ONLINE' | 'IN_PERSON';

type OrderDto = {
  id: number;
  userId: number;
  status: OrderStatus;
  subtotalAmount: Money;
  taxAmount: Money;
  shippingAmount: Money;
  discountAmount: Money;
  totalAmount: Money;
  shippingMethod: 'REGULAR' | 'EXPRESS';
  shippingAddress: ShippingAddressDto | null;
  createdAt: string;
  updatedAt: string;
  items: OrderItemDto[];
  stripePaymentIntentId?: string;

  origin?: Origin;
  couponCode?: string | null;

  /* refund summary (optional-aware) */
  refundedAmount?: Money;
  shippingRefundedAmount?: Money;
  fullyRefunded?: boolean | null;
  refunds?: RefundEventDto[] | null;
};

type AmountBand = 'ALL' | '0-200' | '200-500' | '500+';

/* utility helpers */

function formatCurrency(n: Money) {
  const num = typeof n === 'string' ? Number(n) : n;
  if (!Number.isFinite(num)) return String(n);
  return new Intl.NumberFormat('en-CA', { style: 'currency', currency: 'CAD' }).format(num);
}

function statusChipColor(s: OrderStatus) {
  switch (s) {
    case 'PAID':             return 'info';
    case 'FULFILLED':        return 'success';
    case 'REFUNDED':         return 'secondary';
    case 'CANCELLED':        return 'error';
    case 'PENDING_PAYMENT':  return 'default';
    default:                 return 'default';
  }
}

function toUtcStart(dateYYYYMMDD: string | null): string | undefined {
  if (!dateYYYYMMDD) return undefined;
  return new Date(`${dateYYYYMMDD}T00:00:00Z`).toISOString();
}

function toUtcEnd(dateYYYYMMDD: string | null): string | undefined {
  if (!dateYYYYMMDD) return undefined;
  return new Date(`${dateYYYYMMDD}T23:59:59Z`).toISOString();
}

/* simple authenticated fetch */

async function authFetch(input: RequestInfo, init: RequestInit = {}) {
  const token = localStorage.getItem('accessToken');
  const headers: HeadersInit = {
    ...(init.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
  const resp = await fetch(input, { ...init, headers });
  if (!resp.ok) {
    const text = await resp.text().catch(() => '');
    throw new Error(`HTTP ${resp.status}: ${text || resp.statusText}`);
  }
  const contentType = resp.headers.get('content-type') || '';
  return contentType.includes('application/json') ? resp.json() : null;
}

/* client-side filtering & pagination */

type PageResult<T> = { items: T[]; totalPages: number; page: number };

/* small helpers */
function num(n: Money | undefined | null): number {
  const x = typeof n === 'string' ? Number(n) : n ?? 0;
  return Number.isFinite(x) ? x : 0;
}
function deriveRefundSums(o: OrderDto): { items: number; shipping: number; total: number } {
  const fromSummaryItems    = num(o.refundedAmount);
  const fromSummaryShipping = num(o.shippingRefundedAmount);
  if (fromSummaryItems > 0 || fromSummaryShipping > 0) {
    return { items: fromSummaryItems, shipping: fromSummaryShipping, total: fromSummaryItems + fromSummaryShipping };
  }
  const evts = Array.isArray(o.refunds) ? o.refunds! : [];
  if (evts.length === 0) return { items: 0, shipping: 0, total: 0 };
  let items = 0, ship = 0;
  for (const e of evts) {
    const amt  = num(e.amount);
    const shipAmt = num(e.shippingAmount);
    items += Math.max(0, amt - shipAmt);
    ship  += shipAmt;
  }
  return { items, shipping: ship, total: items + ship };
}

function applyFiltersAndPage(
  source: OrderDto[],
  opts: { amountBand: AmountBand; dateFrom: string | null; dateTo: string | null; page: number; size: number }
): PageResult<OrderDto> {
  const fromIso = toUtcStart(opts.dateFrom ?? null);
  const toIso   = toUtcEnd(opts.dateTo ?? null);

  const filtered = source
    .filter(o => o.status !== 'CANCELLED' && o.status !== 'PENDING_PAYMENT')
    .filter(o => {
      const ts = new Date(o.createdAt).toISOString();
      if (fromIso && ts < fromIso) return false;
      if (toIso   && ts > toIso)   return false;
      return true;
    })
    .filter(o => {
      const total = Number(o.totalAmount) || 0;
      switch (opts.amountBand) {
        case '0-200':   return total >= 0   && total <= 200;
        case '200-500': return total >= 200 && total <= 500;
        case '500+':    return total >= 500;
        default:        return true;
      }
    })
    .map(o => ({ ...o, origin: o.origin ?? (o.stripePaymentIntentId ? 'ONLINE' : 'IN_PERSON') }))
    .sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt));

  const start = opts.page * opts.size;
  const items = filtered.slice(start, start + opts.size);
  const totalPages = Math.max(1, Math.ceil(filtered.length / opts.size));

  return { items, totalPages, page: opts.page };
}

/* card (read-only) */

function OrderCard({ o }: { o: OrderDto }) {
  const addr = o.shippingAddress || {};
  const origin = o.origin ?? (o.stripePaymentIntentId ? 'ONLINE' : 'IN_PERSON');

  const { items: refundedItems, shipping: refundedShipping, total: refundedTotal } = deriveRefundSums(o);
  const netPaid = Math.max(0, (Number(o.totalAmount) || 0) - refundedTotal);

  const itemNameById = (id: number) => {
    const it = o.items.find(li => li.id === id);
    return it ? it.product.name : `Item #${id}`;
  };

  return (
    <Container maxWidth="lg" sx={{ display: 'flex', justifyContent: 'center', overflowX: 'hidden'}} >
      <Paper
        elevation={3}
        sx={{
          p: 2.5,
          borderRadius: 3,
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          width: '100%',
          alignItems: 'center',
        }}
      >
        <Stack direction="row" alignItems="center" justifyContent="space-between">
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
              Order #{o.id}
            </Typography>
            <Chip label={o.status} size="small" color={statusChipColor(o.status) as any} />
            <Chip label={origin} size="small" variant="outlined" color={origin === 'ONLINE' ? 'primary' : 'warning'} />
            {o.fullyRefunded ? <Chip label="Fully Refunded" size="small" color="secondary" variant="outlined" /> : null}
            <Typography variant="body2" color="text.secondary">
              {new Date(o.createdAt).toLocaleString()}
            </Typography>
          </Stack>

          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            {formatCurrency(o.totalAmount)}
          </Typography>
        </Stack>

        <Divider sx={{ my: 1.5 }} />

        {/* Two-column grid (items left, shipping right) */}
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: '300px 1fr' },
            columnGap: 2,
            alignItems: 'start',
          }}
        >
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Items</Typography>
            <Stack spacing={1}>
              {o.items.map(li => (
                <Box
                  key={li.id}
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: '1fr auto',
                    columnGap: 2,
                    alignItems: 'start',
                  }}
                >
                  <Typography variant="body2" sx={{ mr: 1, overflowWrap: 'anywhere' }}>
                    {li.quantity} x {li.product.name}{li.product.sku ? ` (${li.product.sku})` : ''}
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 600, whiteSpace: 'nowrap' }}>
                    {formatCurrency(li.totalPrice)}
                  </Typography>
                </Box>
              ))}
            </Stack>
          </Box>

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Shipping & Totals</Typography>
            <Stack spacing={0.5}>
              {addr?.fullName && <Typography variant="body2"><strong>Name:</strong> {addr.fullName}</Typography>}
              {addr?.email &&    <Typography variant="body2"><strong>Email:</strong> {addr.email}</Typography>}
              {addr?.phone &&    <Typography variant="body2"><strong>Phone:</strong> {addr.phone}</Typography>}
              {(addr?.line1 || addr?.city) && (
                <Typography variant="body2">
                  <strong>Address:</strong>{' '}
                  {[addr?.line1, addr?.line2, addr?.city, addr?.province, addr?.postalCode, addr?.country]
                    .filter(Boolean)
                    .join(', ')}
                </Typography>
              )}
              <Typography variant="body2"><strong>Shipping:</strong> {o.shippingMethod}</Typography>

              <Divider sx={{ my: 1.5 }} />

              <Typography variant="body2">
                <strong>Subtotal:</strong> {formatCurrency(o.subtotalAmount)}
              </Typography>
              <Typography variant="body2">
                <strong>Shipping:</strong> {formatCurrency(o.shippingAmount)}
              </Typography>
              <Typography variant="body2">
                <strong>Tax:</strong> {formatCurrency(o.taxAmount)}
              </Typography>
              {Number(o.discountAmount) > 0 && (
                <>
                  {o.couponCode && (
                    <Typography variant="body2"><strong>Coupon:</strong> {o.couponCode}</Typography>
                  )}
                  <Typography variant="body2">
                    <strong>Discount:</strong> -{formatCurrency(o.discountAmount)}
                  </Typography>
                </>
              )}
              <Typography variant="body2" sx={{ fontWeight: 700 }}>
                <strong>Total:</strong> {formatCurrency(o.totalAmount)}
              </Typography>

              {/* Refunds */}
              {(refundedTotal > 0 || (o.refunds && o.refunds.length > 0)) && (
                <>
                  <Divider sx={{ my: 1.5 }} />
                  <Typography variant="subtitle2">Refunds</Typography>

                  {refundedTotal > 0 && (
                    <>
                      <Typography variant="body2">
                        <strong>Refunded so far:</strong> -{formatCurrency(refundedTotal)}
                      </Typography>
                      {refundedShipping > 0 && (
                        <Typography variant="body2">
                          <strong>Shipping refunded:</strong> -{formatCurrency(refundedShipping)}
                        </Typography>
                      )}
                      <Typography variant="body2">
                        <strong>Net paid:</strong> {formatCurrency(netPaid)}
                      </Typography>
                    </>
                  )}

                  {Array.isArray(o.refunds) && o.refunds.map(ev => (
                    <Box
                      key={ev.id}
                      sx={{
                        mt: 1,
                        p: 1,
                        borderRadius: 1.5,
                        backgroundColor: 'rgba(0,0,0,0.03)',
                      }}
                    >
                      <Stack direction="row" alignItems="center" justifyContent="space-between">
                        <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                          {new Date(ev.createdAt ?? o.updatedAt).toLocaleString()} — {ev.provider}
                          {ev.reason ? ` — ${ev.reason}` : ''}
                          {ev.includesShipping ? ` — includes shipping (${formatCurrency(ev.shippingAmount)})` : ''}
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          -{formatCurrency(ev.amount)}
                        </Typography>
                      </Stack>

                      {(ev.lines && ev.lines.length > 0) && (
                        <Stack component="ul" sx={{ pl: 2, mt: 0.5 }}>
                          {ev.lines.map((ln, idx) => (
                            <li key={`${ev.id}-${idx}`}>
                              <Typography variant="caption">
                                {ln.quantity} × {itemNameById(ln.orderItemId)}
                              </Typography>
                            </li>
                          ))}
                        </Stack>
                      )}
                    </Box>
                  ))}
                </>
              )}
            </Stack>
          </Box>
        </Box>
      </Paper>
    </Container>
  );
}

/* main page */

export default function MyOrders() {
  const [all, setAll]             = useState<OrderDto[]>([]);
  const [view, setView]           = useState<OrderDto[]>([]);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState<string | null>(null);

  // search inputs
  const [amountBand, setAmount]   = useState<AmountBand>('ALL');
  const [dateFrom, setDateFrom]   = useState<string | null>(null);
  const [dateTo,   setDateTo]     = useState<string | null>(null);

  // pagination
  const [page, setPage]           = useState(0);
  const pageSize                  = 10;
  const [totalPages, setTotal]    = useState(1);

  const refresh = async (goToPage?: number) => {
    try {
      setLoading(true);
      setError(null);
      const data = await authFetch('/api/orders'); // returns current user's orders
      const list = Array.isArray(data) ? (data as OrderDto[]) : [];

      const sorted = list
        .map(o => ({ ...o, origin: o.origin ?? (o.stripePaymentIntentId ? 'ONLINE' : 'IN_PERSON') }))
        .sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt));
      setAll(sorted);

      const target = typeof goToPage === 'number' ? goToPage : 0;
      const res = applyFiltersAndPage(sorted, { amountBand, dateFrom, dateTo, page: target, size: pageSize });
      setView(res.items);
      setTotal(res.totalPages);
      setPage(res.page);
    } catch (e: any) {
      const msg = (e?.message || '').toLowerCase().includes('http 401')
        ? 'Your session has expired. Please log in again.'
        : (e?.message || 'Failed to load orders');
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = (goToPage = 0) => {
    const res = applyFiltersAndPage(all, { amountBand, dateFrom, dateTo, page: goToPage, size: pageSize });
    setView(res.items);
    setTotal(res.totalPages);
    setPage(res.page);
  };

  const handleReset = () => {
    setAmount('ALL');
    setDateFrom(null);
    setDateTo(null);
    applyFilters(0);
  };

  useEffect(() => { refresh(0); }, []); // initial load

  useEffect(() => { applyFilters(0); /* eslint-disable-line react-hooks/exhaustive-deps */ }, [amountBand, dateFrom, dateTo, all.length]);

  return (
    <Box component="main" sx={{ minHeight: '100dvh', position: 'relative' }}>
      {/* Full-bleed fixed gradient background */}
      <Box
        aria-hidden
        sx={{
          position: 'fixed',
          inset: 0,
          zIndex: -1,
          backgroundImage: INFO_GRADIENT,
          backgroundRepeat: 'no-repeat',
          backgroundAttachment: 'fixed',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      />

      <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 } }}>
        {/* header */}
        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>
          <Typography variant="h4" sx={{ color: 'common.white', fontWeight: 700 }}>
            My Order History
          </Typography>
          <IconButton onClick={() => refresh(page)} disabled={loading} sx={{ color: '#fff' }}>
            <RefreshIcon />
          </IconButton>
        </Stack>

        {/* search (amount + date range) */}
        <Paper elevation={3} sx={{ p: 2, borderRadius: 3, mb: 2, backgroundColor: 'rgba(255,255,255,0.9)' }}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' },
              gap: 2,
              alignItems: 'center',
            }}
          >
            <TextField
              select
              label="Order Value"
              value={amountBand}
              onChange={e => setAmount(e.target.value as AmountBand)}
              size="small"
              fullWidth
            >
              <MenuItem value="ALL">All</MenuItem>
              <MenuItem value="0-200">$0 - $200</MenuItem>
              <MenuItem value="200-500">$200 - $500</MenuItem>
              <MenuItem value="500+">$500+</MenuItem>
            </TextField>

            <TextField
              label="From (date)"
              type="date"
              value={dateFrom ?? ''}
              onChange={e => setDateFrom(e.target.value || null)}
              size="small"
              InputLabelProps={{ shrink: true }}
              fullWidth
            />
            <TextField
              label="To (date)"
              type="date"
              value={dateTo ?? ''}
              onChange={e => setDateTo(e.target.value || null)}
              size="small"
              InputLabelProps={{ shrink: true }}
              fullWidth
            />

            <Box sx={{ gridColumn: { xs: '1', md: '1 / -1' }, display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
              <Button
                variant="contained"
                onClick={() => applyFilters(0)}
                disabled={loading}
                startIcon={<SearchIcon />}
              >
                Apply
              </Button>
              <Button variant="text" onClick={handleReset} disabled={loading}>
                Reset
              </Button>
            </Box>
          </Box>
        </Paper>

        {/* orders */}
        <Box maxWidth="lg" sx={{ display: 'flex', justifyContent: 'center'}}>
          <Box maxWidth="lg" sx={{ width: '100%', maxWidth: 1000}}>
            {error && (
              <Paper elevation={3} sx={{ p: 2, mb: 2, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.9)' }}>
                <Typography color="error">{error}</Typography>
              </Paper>
            )}

            {loading ? (
              <Box sx={{ py: 6, display: 'flex', justifyContent: 'center' }}>
                <CircularProgress />
              </Box>
            ) : (
              <>
                <Stack spacing={2}>
                  {view.length === 0 ? (
                    <Paper
                      elevation={3}
                      sx={{ p: 3, textAlign: 'center', borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.9)' }}
                    >
                      <Typography>No orders found.</Typography>
                    </Paper>
                  ) : (
                    view.map(o => <OrderCard key={o.id} o={o} />)
                  )}
                </Stack>

                {/* pagination */}
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
                  <Pagination
                    color="primary"
                    count={totalPages}
                    page={page + 1}
                    onChange={(_, p1) => {
                      const nextPage = p1 - 1;
                      const res = applyFiltersAndPage(all, {
                        amountBand, dateFrom, dateTo, page: nextPage, size: pageSize,
                      });
                      setView(res.items);
                      setTotal(res.totalPages);
                      setPage(res.page);
                    }}
                  />
                </Box>
              </>
            )}
          </Box>
        </Box>
      </Container>
    </Box>
  );
}
