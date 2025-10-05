import {
  Box, Container, Typography, Paper, Stack, TextField,
  MenuItem, Button, Chip, Divider, Dialog, DialogTitle, DialogContent,
  DialogActions, CircularProgress, IconButton, Tooltip, InputAdornment,
  Pagination, Checkbox, FormControlLabel,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import UndoIcon from '@mui/icons-material/Undo';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import SearchIcon from '@mui/icons-material/Search';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useRole } from '../context/RoleContext';
import { api } from '../lib/api';
import { adminRefundOrder } from '../lib/refunds';

/* theme bits */
const INFO_GRADIENT = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';


/* types mirrored from backend DTOs */

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
type OfflineMethod = 'CASH' | 'ETRANSFER' | 'TERMINAL' | 'OTHER';

type OrderDto = {
  id: number;
  userId: number;
  status: OrderStatus;
  subtotalAmount: Money;
  taxAmount: Money;
  shippingAmount: Money;
  discountAmount: Money;
  totalAmount: Money;

  /* refund summary (optional-aware) */
  refundedAmount?: Money;
  shippingRefundedAmount?: Money;
  fullyRefunded?: boolean | null;
  refunds?: RefundEventDto[] | null;

  shippingMethod: 'REGULAR' | 'EXPRESS';
  stripePaymentIntentId?: string | null;
  offlinePaymentMethod?: OfflineMethod | null;

  shippingAddress: ShippingAddressDto | null;
  createdAt: string;
  updatedAt: string;
  items: OrderItemDto[];

  origin?: Origin;               // derived if missing
  couponCode?: string | null;
};

type StatusFilter = 'ALL' | 'PAID' | 'FULFILLED' | 'REFUNDED';
type AmountBand = 'ALL' | '0-200' | '200-500' | '500+';
type OriginFilter = 'ALL' | 'ONLINE' | 'IN_PERSON';
type PayFilter = 'ALL' | OfflineMethod;

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

/* client helpers */

type PageEnvelope<T> = {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;   // 0-based page index
  size: number;     // page size
};

async function searchOrders(params: {
  email?: string;
  orderId?: string;
  status?: StatusFilter;            // 'ALL' omitted
  amountBand?: AmountBand;
  dateFrom?: string | null;         // yyyy-mm-dd
  dateTo?: string | null;           // yyyy-mm-dd
  origin?: OriginFilter;
  pay?: PayFilter;
  page: number;                     // 0-based
  size: number;                     // page size
}): Promise<{ items: OrderDto[]; totalPages: number; page: number; size: number }> {
  const usp = new URLSearchParams();

  usp.set('includePendingPayment', 'false');

  if (params.status && params.status !== 'ALL') {
    usp.set('status', params.status);
  }

  if (params.email && params.email.trim()) {
    usp.set('email', params.email.trim());
  }

  if (params.orderId && /^\d+$/.test(params.orderId.trim())) {
    usp.set('orderId', params.orderId.trim());
  }

  switch (params.amountBand) {
    case '0-200':
      usp.set('amountMin', '0'); usp.set('amountMax', '200'); break;
    case '200-500':
      usp.set('amountMin', '200'); usp.set('amountMax', '500'); break;
    case '500+':
      usp.set('amountMin', '500'); break;
    default: break;
  }

  const fromIso = toUtcStart(params.dateFrom ?? null);
  const toIso   = toUtcEnd(params.dateTo ?? null);
  if (fromIso) usp.set('dateFrom', fromIso);
  if (toIso)   usp.set('dateTo',   toIso);

  if (params.origin && params.origin !== 'ALL') usp.set('origin', params.origin);
  if (params.pay    && params.pay    !== 'ALL') usp.set('paymentMethod', params.pay);

  usp.set('page', String(params.page));
  usp.set('size', String(params.size));
  usp.set('sort', 'createdAt,desc');

  const path = `/admin/orders?${usp.toString()}`;
  const data = await api<PageEnvelope<OrderDto> | OrderDto[]>(path);

  const mapper = (o: OrderDto): OrderDto => ({
    ...o,
    origin: o.origin ?? (o.stripePaymentIntentId ? 'ONLINE' : 'IN_PERSON'),
  });

  if (data && Array.isArray((data as any).content)) {
    const pg = data as PageEnvelope<OrderDto>;
    const filtered = (pg.content || [])
      .map(mapper)
      .filter(o => o.status !== 'CANCELLED' && o.status !== 'PENDING_PAYMENT');
    return {
      items: filtered,
      totalPages: pg.totalPages ?? 1,
      page: pg.number ?? params.page,
      size: pg.size ?? params.size,
    };
  }

  const list = Array.isArray(data) ? (data as OrderDto[]) : [];
  const filtered = list
    .map(mapper)
    .filter(o => o.status !== 'CANCELLED' && o.status !== 'PENDING_PAYMENT')
    .sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt));

  const start = params.page * params.size;
  const items = filtered.slice(start, start + params.size);
  const totalPages = Math.max(1, Math.ceil(filtered.length / params.size));

  return { items, totalPages, page: params.page, size: params.size };
}

async function patchStatus(orderId: number, status: 'FULFILLED' | 'REFUNDED') {
  await api<void>(`/admin/orders/${orderId}?status=${status}`, { method: 'PATCH' });
}

/* refund helpers */
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

/* confirm dialog component */

type ConfirmProps = {
  open: boolean;
  title: string;
  body: string;
  confirmText: string;
  onCancel: () => void;
  onConfirm: () => void;
  loading?: boolean;
};

function ConfirmDialog({
  open, title, body, confirmText, onCancel, onConfirm, loading,
}: ConfirmProps) {
  return (
    <Dialog open={open} onClose={loading ? undefined : onCancel} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" sx={{ mt: 1 }}>{body}</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={!!loading}>Cancel</Button>
        <Button variant="contained" color="primary" onClick={onConfirm} disabled={!!loading}>
          {loading ? <CircularProgress size={18} /> : confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/* Partial refund dialog */

type RefundMode = 'BY_ITEMS' | 'CUSTOM_AMOUNT';

function RefundDialog({
  open, order, onClose, onDone,
}: {
  open: boolean;
  order: OrderDto | null;
  onClose: () => void;
  onDone: () => void;
}) {
  // Mode
  const [mode, setMode] = useState<RefundMode>('BY_ITEMS');

  // By-items state
  const [lines, setLines] = useState<Record<number, number>>({});
  const [includeShip, setIncludeShip] = useState(false);

  // Custom-amount state
  const [customAmount, setCustomAmount] = useState<string>('');

  // Common
  const [reason, setReason] = useState('');
  const [working, setWorking] = useState(false);

  // Compute remaining
  const remaining = useMemo(() => {
    if (!order) return 0;
    const totals = deriveRefundSums(order);
    const left = num(order.totalAmount) - totals.total;
    return Math.max(0, left);
  }, [order]);

  useEffect(() => {
    if (!order) return;
    const init: Record<number, number> = {};
    order.items.forEach(li => { init[li.id] = 0; });
    setLines(init);
    setIncludeShip(false);
    setReason('');
    setCustomAmount('');
    setMode('BY_ITEMS');
    setWorking(false);
  }, [order, open]);

  if (!order) return null;

  const estimateItemsRefund = order.items.reduce((sum, li) => {
    const qty = lines[li.id] || 0;
    const unit = Number(li.unitPrice) || 0;
    return sum + qty * unit;
  }, 0) + (includeShip ? Number(order.shippingAmount) || 0 : 0);

  const byItemsNote = 'Amount shown is an estimate. The backend will compute the precise refund (including taxes and coupon apportionment) and restock the selected quantities.';
  const customNote  = 'Custom amount refunds do not restock inventory. If you need restocking, use "By items".';

  const submit = async () => {
    const defaultReason = (reason && reason.trim()) ? reason.trim() : 'requested_by_customer';
    setWorking(true);
    try {
      if (mode === 'BY_ITEMS') {
        const linePayload = Object.entries(lines)
          .map(([id, qty]) => ({ orderItemId: Number(id), quantity: Math.max(0, Number(qty || 0)) }))
          .filter(x => x.quantity > 0);

        if (!linePayload.length && !includeShip) {
          alert('Select at least one line or include shipping.');
          setWorking(false);
          return;
        }

        await adminRefundOrder(order.id, {
          lines: linePayload,
          refundShipping: includeShip,
          reason: defaultReason,
        });
      } else {
        const amt = Number(customAmount);
        if (!Number.isFinite(amt) || amt <= 0) {
          alert('Enter a valid amount to refund.');
          setWorking(false);
          return;
        }
        if (amt > remaining) {
          alert(`Amount exceeds remaining refundable balance (${formatCurrency(remaining)}).`);
          setWorking(false);
          return;
        }

        await adminRefundOrder(order.id, {
          amount: Number(amt.toFixed(2)),
          reason: defaultReason,
        });
      }

      onDone();
      onClose();
    } catch (e: any) {
      alert(e?.message || 'Refund failed');
    } finally {
      setWorking(false);
    }
  };

  return (
    <Dialog open={open} onClose={working ? undefined : onClose} fullWidth maxWidth="sm">
      <DialogTitle>Partial Refund - Order #{order.id}</DialogTitle>
      <DialogContent dividers>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2 }}>
          <TextField
            select
            label="Refund mode"
            value={mode}
            onChange={e => setMode(e.target.value as RefundMode)}
            size="small"
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="BY_ITEMS">By items (restock)</MenuItem>
            <MenuItem value="CUSTOM_AMOUNT">Custom amount (no restock)</MenuItem>
          </TextField>

          <Box sx={{ flex: 1 }} />

          <Typography variant="body2" sx={{ alignSelf: 'center' }}>
            Remaining: <b>{formatCurrency(remaining)}</b>
          </Typography>
        </Stack>

        {mode === 'BY_ITEMS' ? (
          <>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Select items and quantities to refund</Typography>
            <Stack spacing={1}>
              {order.items.map(li => (
                <Stack key={li.id} direction="row" alignItems="center" justifyContent="space-between">
                  <Typography variant="body2" sx={{ mr: 2, overflowWrap: 'anywhere' }}>
                    {li.product.name} — {formatCurrency(li.unitPrice)} each
                  </Typography>
                  <TextField
                    type="number"
                    size="small"
                    label="Qty"
                    value={lines[li.id] ?? 0}
                    onChange={e => {
                      const input = Number(e.target.value || 0);
                      const n = Math.max(0, Math.min(input, Number(li.quantity) || 0));
                      setLines(prev => ({ ...prev, [li.id]: n }));
                    }}
                    inputProps={{ min: 0, max: li.quantity, style: { width: 80, textAlign: 'center' } }}
                  />
                </Stack>
              ))}
            </Stack>

            <Divider sx={{ my: 2 }} />

            <FormControlLabel
              control={<Checkbox checked={includeShip} onChange={e => setIncludeShip(e.target.checked)} />}
              label={`Include shipping (${formatCurrency(order.shippingAmount)})`}
            />

            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              {byItemsNote}
            </Typography>

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle1">
              Estimated refund: <b>{formatCurrency(estimateItemsRefund)}</b>
            </Typography>
          </>
        ) : (
          <>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Enter an amount to refund (no restock)</Typography>
            <TextField
              type="number"
              size="small"
              label="Amount (CAD)"
              value={customAmount}
              onChange={e => setCustomAmount(e.target.value)}
              inputProps={{ min: 0, step: '0.01' }}
              helperText={`Up to ${formatCurrency(remaining)}`}
            />
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
              {customNote}
            </Typography>
          </>
        )}

        <Divider sx={{ my: 2 }} />

        <TextField
          fullWidth
          label="Reason"
          value={reason}
          onChange={e => setReason(e.target.value)}
          multiline minRows={2}
          helperText='Required for audit (e.g., "requested_by_customer").'
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={working}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={working}>
          {working ? <CircularProgress size={18} /> : 'Submit refund'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/* card renderer (memoized) */

function OrderCardBase({
  o,
  onAction,
  onPartialRefund,
  refundDisabled,
}: {
  o: OrderDto;
  onAction: (orderId: number, action: 'FULFILLED' | 'REFUNDED') => void;
  onPartialRefund: (o: OrderDto) => void;
  refundDisabled: boolean;
}) {
  const addr = o.shippingAddress || {};
  const origin = o.origin ?? (o.stripePaymentIntentId ? 'ONLINE' : 'IN_PERSON');
  const canFulfill   = o.status === 'PAID';
  const canRefundFull= (o.status === 'PAID' || o.status === 'FULFILLED') && !refundDisabled;
  const canRefundPartial = (o.status === 'PAID' || o.status === 'FULFILLED') && !refundDisabled; // now includes ONLINE

  const { items: refundedItems, shipping: refundedShipping, total: refundedTotal } = deriveRefundSums(o);
  const netPaid = Math.max(0, num(o.totalAmount) - refundedTotal);
  const refundEvents = Array.isArray(o.refunds) ? o.refunds! : [];

  const itemNameById = (id: number) => {
    const it = o.items.find(li => li.id === id);
    return it ? it.product.name : `Item #${id}`;
    };

  return (
    <Paper
      elevation={3}
      sx={{
        p: 2.5,
        borderRadius: 3,
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        width: '100%',
        overflowX: 'auto',
      }}
    >
      <Stack direction="row" alignItems="center" justifyContent="space-between">
        <Stack direction="row" alignItems="center" spacing={1.5}>
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            Order #{o.id}
          </Typography>
          <Chip label={o.status} size="small" color={statusChipColor(o.status) as any} />
          <Chip
            label={origin}
            size="small"
            color={origin === 'ONLINE' ? 'primary' : 'warning'}
            variant="outlined"
          />
          {o.fullyRefunded ? <Chip label="Fully Refunded" size="small" color="secondary" variant="outlined" /> : null}
          <Typography variant="body2" color="text.secondary">
            {new Date(o.createdAt).toLocaleString()}
          </Typography>
        </Stack>

        <Stack direction="row" alignItems="center" spacing={1}>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            {formatCurrency(o.totalAmount)}
          </Typography>
          <Divider flexItem orientation="vertical" sx={{ mx: 1 }} />
          <Tooltip title="Mark as Fulfilled">
            <span>
              <IconButton
                color="success"
                size="small"
                onClick={() => onAction(o.id, 'FULFILLED')}
                disabled={!canFulfill}
              >
                <DoneAllIcon />
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title={refundDisabled ? 'Refund window (90 days) has passed' : 'Full refund (remaining balance)'}>
            <span>
              <IconButton
                color="error"
                size="small"
                onClick={() => onAction(o.id, 'REFUNDED')}
                disabled={!canRefundFull}
              >
                <UndoIcon />
              </IconButton>
            </span>
          </Tooltip>
          {canRefundPartial && (
            <Tooltip title="Partial refund (items or custom amount)">
              <span>
                <IconButton
                  color="error"
                  size="small"
                  onClick={() => onPartialRefund(o)}
                >
                  <ReceiptLongIcon />
                </IconButton>
              </span>
            </Tooltip>
          )}
        </Stack>
      </Stack>

      <Divider sx={{ my: 1.5 }} />

      {/* Two-column grid */}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '1fr',
            sm: '300px 1fr',
          },
          columnGap: 2,
          alignItems: 'start',
        }}
      >
        {/* Left: Items */}
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
                <Typography
                  variant="body2"
                  sx={{ mr: 1, wordBreak: 'break-word', overflowWrap: 'anywhere', whiteSpace: 'normal' }}
                >
                  {li.quantity} x {li.product.name}
                  {li.product.sku ? ` (${li.product.sku})` : ''}
                </Typography>

                <Typography variant="body2" sx={{ fontWeight: 600, whiteSpace: 'nowrap' }}>
                  {formatCurrency(li.totalPrice)}
                </Typography>
              </Box>
            ))}
          </Stack>
        </Box>

        {/* Right: Buyer & totals (plus refunds) */}
        <Box>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>Buyer & Shipping</Typography>
          <Stack spacing={0.5}>
            <Typography variant="body2"><strong>User ID:</strong> {o.userId}</Typography>
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
            <Typography variant="body2">
              <strong>Shipping:</strong> {o.shippingMethod}
            </Typography>
            {o.offlinePaymentMethod && (
              <Typography variant="body2">
                <strong>Payment (in-person):</strong> {o.offlinePaymentMethod}
              </Typography>
            )}
            {o.couponCode && (
              <Typography variant="body2">
                <strong>Coupon:</strong> {o.couponCode}
              </Typography>
            )}
            {o.stripePaymentIntentId && (
              <Typography variant="caption" color="text.secondary">
                PI: {o.stripePaymentIntentId}
              </Typography>
            )}
          </Stack>

          <Divider sx={{ my: 1.5 }} />

          <Stack spacing={0.5}>
            <Typography variant="body2">
              <strong>Subtotal:</strong> {formatCurrency(o.subtotalAmount)}
            </Typography>
            <Typography variant="body2">
              <strong>Shipping:</strong> {formatCurrency(o.shippingAmount)}
            </Typography>
            <Typography variant="body2">
              <strong>Tax:</strong> {formatCurrency(o.taxAmount)}
            </Typography>
            {num(o.discountAmount) > 0 && (
              <Typography variant="body2">
                <strong>Discount:</strong> -{formatCurrency(o.discountAmount)}
              </Typography>
            )}
            <Typography variant="body2" sx={{ fontWeight: 700 }}>
              <strong>Total:</strong> {formatCurrency(o.totalAmount)}
            </Typography>

            {(refundedTotal > 0 || refundEvents.length > 0) && (
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

                {refundEvents.map(ev => (
                  <Box
                    key={ev.id}
                    sx={{
                      mt: 1,
                      p: 1,
                      borderRadius: 1.5,
                      backgroundColor: 'rgba(0,0,0,0.03)',
                    }}
                  >
                    <Stack direction="column" alignItems="start" justifyContent="space-between">
                      <Typography variant="body1" sx={{ fontWeight: 600 }}>
                        Refund Notice
                      </Typography>
                      <Typography variant="body2" sx={{ overflowWrap: 'break-word', mb: 1 }}>
                        {new Date(ev.createdAt ?? o.updatedAt).toLocaleString()} - {ev.provider}
                        {ev.reason ? ` - ${ev.reason}` : ''}
                        {ev.includesShipping ? ` - includes shipping (${formatCurrency(ev.shippingAmount)})` : ''}
                      </Typography>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        Refund Amount: {formatCurrency(ev.amount)}
                      </Typography>
                    </Stack>

                    {(ev.lines && ev.lines.length > 0) && (
                      <Stack component="ul" sx={{ pl: 2, mt: 0.5 }}>
                        {ev.lines.map((ln, idx) => (
                          <li key={`${ev.id}-${idx}`}>
                            <Typography variant="body2">
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
  );
}

const OrderCard = React.memo(
  OrderCardBase,
  (prev, next) =>
    prev.onAction === next.onAction &&
    prev.onPartialRefund === next.onPartialRefund &&
    prev.refundDisabled === next.refundDisabled &&
    prev.o.id === next.o.id &&
    prev.o.status === next.o.status &&
    prev.o.updatedAt === next.o.updatedAt &&
    prev.o.items.length === next.o.items.length
);

/* access denied helper */

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

/* main page */

export default function AdminOrders() {
  const { role } = useRole();
  const isAdmin = role === 'OWNER' || role === 'ADMIN';
  if (!isAdmin) return <AccessDenied />;

  const [orders, setOrders] = useState<OrderDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState<string | null>(null);

  // unified search inputs
  const [emailQ, setEmailQ]     = useState<string>('');   // email (fuzzy)
  const [orderIdQ, setOrderIdQ] = useState<string>('');   // exact id
  const [status, setStatus]       = useState<StatusFilter>('ALL');
  const [amountBand, setAmount]   = useState<AmountBand>('ALL');
  const [dateFrom, setDateFrom]   = useState<string | null>(null); // yyyy-mm-dd
  const [dateTo,   setDateTo]     = useState<string | null>(null);

  // filters
  const [origin, setOrigin]       = useState<OriginFilter>('ALL');
  const [pay,    setPay   ]       = useState<PayFilter>('ALL');

  // pagination
  const [page, setPage] = useState(0);
  const pageSize = 5;
  const [totalPages, setTotalPages] = useState(1);

  // confirm dialog state
  const [confirm, setConfirm] = useState<{
    orderId: number | null;
    action: 'FULFILLED' | 'REFUNDED' | null;
    open: boolean;
    working: boolean;
  }>({ orderId: null, action: null, open: false, working: false });

  // partial refund dialog
  const [refundOpen, setRefundOpen] = useState(false);
  const [refundOrder, setRefundOrder] = useState<OrderDto | null>(null);

  const fetchData = async (goToPage?: number) => {
    try {
      setLoading(true);
      setError(null);
      const wantPage = typeof goToPage === 'number' ? goToPage : page;
      const res = await searchOrders({
        email: emailQ.trim(),
        orderId: orderIdQ.trim(),
        status,
        amountBand,
        dateFrom,
        dateTo,
        origin,
        pay,
        page: wantPage,
        size: pageSize,
      });

      // client-side filters in case backend ignores unknown params
      const filtered = res.items.filter(o => {
        const matchOrigin = origin === 'ALL' || (o.origin ?? (o.stripePaymentIntentId ? 'ONLINE' : 'IN_PERSON')) === origin;
        const matchPay = pay === 'ALL' || (o.offlinePaymentMethod ?? '') === pay;
        return matchOrigin && matchPay;
      });

      setOrders(filtered);
      setTotalPages(res.totalPages);
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

  useEffect(() => { fetchData(0); /* eslint-disable-line react-hooks/exhaustive-deps */ }, []);

  const handleSearch = async () => { await fetchData(0); };

  const handleClearAll = async () => {
    setEmailQ('');
    setOrderIdQ('');
    setStatus('ALL');
    setAmount('ALL');
    setDateFrom(null);
    setDateTo(null);
    setOrigin('ALL');
    setPay('ALL');
    await fetchData(0);
  };

  const openConfirm = useCallback((orderId: number, action: 'FULFILLED' | 'REFUNDED') => {
    setConfirm({ orderId, action, open: true, working: false });
  }, []);

  const closeConfirm = () => {
    if (confirm.working) return;
    setConfirm({ orderId: null, action: null, open: false, working: false });
  };

  const confirmAction = async () => {
    if (!confirm.orderId || !confirm.action) return;
    try {
      setConfirm(c => ({ ...c, working: true }));
      await patchStatus(confirm.orderId, confirm.action);
      await fetchData(); // stay on same page
    } catch (e: any) {
      setError(e?.message || 'Operation failed');
    } finally {
      closeConfirm();
    }
  };

  const confirmTexts = useMemo(() => {
    if (!confirm.action || !confirm.orderId) return { title: '', body: '', btn: '' };
    if (confirm.action === 'FULFILLED') {
      return {
        title: 'Mark order as FULFILLED?',
        body: `This will mark order #${confirm.orderId} as FULFILLED.`,
        btn: 'Mark Fulfilled',
      };
    }
    return {
      title: 'Issue FULL refund?',
      body: `This will refund the entire remaining balance and set order #${confirm.orderId} to REFUNDED (inventory will be restored).`,
      btn: 'Refund Order',
    };
  }, [confirm.action, confirm.orderId]);

  /* open partial refund dialog */
  const openPartial = (o: OrderDto) => {
    setRefundOrder(o);
    setRefundOpen(true);
  };

  /* 90-day refund window (UI guard; server enforces too) */
  const isRefundPastWindow = (o: OrderDto) => {
    const created = new Date(o.createdAt).getTime();
    const cutoff  = 90 * 24 * 60 * 60 * 1000;
    return Date.now() - created > cutoff;
  };

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
        <Typography variant="h3" align="center" sx={{ color: 'common.white', fontWeight: 700 }}>
            Manage Pro Shop Orders
        </Typography>
        {/* header */}
        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 2 }}>            
          <Tooltip title="Refresh">
            <span>
              <IconButton onClick={() => fetchData()} disabled={loading} sx={{ color: '#fff' }}>
                <RefreshIcon />
              </IconButton>
            </span>
          </Tooltip>
        </Stack>

        {/* unified search & filters panel */}
        <Paper
          elevation={3}
          sx={{ p: 2, borderRadius: 3, mb: 2, backgroundColor: 'rgba(255,255,255,0.9)' }}
        >
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr' },
              gap: 2,
            }}
          >
            {/* Row 1 */}
            <TextField
              fullWidth
              label="Search by Email"
              value={emailQ}
              onChange={e => setEmailQ(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleSearch(); }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
            />
            <TextField
              fullWidth
              label="Order ID (exact)"
              value={orderIdQ}
              onChange={e => setOrderIdQ(e.target.value.replace(/[^\d]/g, ''))}
              inputProps={{ inputMode: 'numeric', pattern: '[0-9]*' }}
              onKeyDown={e => { if (e.key === 'Enter') handleSearch(); }}
            />
            <TextField
              select
              label="Status"
              value={status}
              onChange={e => setStatus(e.target.value as StatusFilter)}
              size="small"
              fullWidth
            >
              <MenuItem value="ALL">All</MenuItem>
              <MenuItem value="PAID">PAID</MenuItem>
              <MenuItem value="FULFILLED">FULFILLED</MenuItem>
              <MenuItem value="REFUNDED">REFUNDED</MenuItem>
            </TextField>

            {/* Row 2 */}
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

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
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
            </Stack>

            {/* Row 3 - Origin and Payment filters */}
            <TextField
              select
              label="Origin"
              value={origin}
              onChange={e => setOrigin(e.target.value as OriginFilter)}
              size="small"
              fullWidth
            >
              <MenuItem value="ALL">All</MenuItem>
              <MenuItem value="ONLINE">ONLINE</MenuItem>
              <MenuItem value="IN_PERSON">IN_PERSON</MenuItem>
            </TextField>
            <TextField
              select
              label="Payment (in-person)"
              value={pay}
              onChange={e => setPay(e.target.value as PayFilter)}
              size="small"
              fullWidth
            >
              <MenuItem value="ALL">All</MenuItem>
              <MenuItem value="CASH">CASH</MenuItem>
              <MenuItem value="ETRANSFER">ETRANSFER</MenuItem>
              <MenuItem value="TERMINAL">TERMINAL</MenuItem>
              <MenuItem value="OTHER">OTHER</MenuItem>
            </TextField>

            <Stack direction="row" spacing={1} justifyContent="flex-end" alignItems="center">
              <Button
                variant="contained"
                onClick={handleSearch}
                disabled={loading}
                startIcon={loading ? <CircularProgress size={18} /> : <SearchIcon />}
              >
                Apply
              </Button>
              <Button variant="text" onClick={handleClearAll} disabled={loading}>
                Reset
              </Button>
            </Stack>
          </Box>
        </Paper>

        {/* orders */}
        <Box sx={{ display: 'flex', justifyContent: 'center' }}>
          <Box sx={{ width: '100%', maxWidth: 1000 }}>
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
                  {orders.length === 0 ? (
                    <Paper
                      elevation={3}
                      sx={{ p: 3, textAlign: 'center', borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.9)' }}
                    >
                      <Typography>No orders found.</Typography>
                    </Paper>
                  ) : (
                    orders.map(o => (
                      <OrderCard
                        key={o.id}
                        o={o}
                        onAction={openConfirm}
                        onPartialRefund={openPartial}
                        refundDisabled={isRefundPastWindow(o)}
                      />
                    ))
                  )}
                </Stack>

                {/* pagination */}
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
                  <Pagination
                    color="primary"
                    count={totalPages}
                    page={page + 1}
                    onChange={(_, p1) => fetchData(p1 - 1)}
                  />
                </Box>
              </>
            )}
          </Box>
        </Box>
      </Container>

      {/* confirm dialog */}
      <ConfirmDialog
        open={confirm.open}
        title={confirmTexts.title}
        body={confirmTexts.body}
        confirmText={confirmTexts.btn}
        onCancel={closeConfirm}
        onConfirm={confirmAction}
        loading={confirm.working}
      />

      {/* partial refund dialog */}
      <RefundDialog
        open={refundOpen}
        order={refundOrder}
        onClose={() => setRefundOpen(false)}
        onDone={() => fetchData()}
      />
    </Box>
  );
}
