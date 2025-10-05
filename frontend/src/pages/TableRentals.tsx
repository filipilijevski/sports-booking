/*  public info page + read-only weekly schedule   */
/*  + Inline Stripe checkout for table rental credits (CLIENT only) */

import { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Container,
  Paper,
  Typography,
  Stack,
  List,
  ListItem,
  ListItemText,
  CircularProgress,
  Alert,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Chip,
} from '@mui/material';
import { alpha, styled } from '@mui/material/styles';
import {
  fetchPublicOccurrences,
  fetchPublicPrograms,
  fetchTableRentalPackages,
  startTableRentalPaymentIntent,
  finalizeTableRentalAfterPayment,
  fetchMyMemberships, // used for “INITIAL” membership guard
  type ProgramOccurrenceDto,
  type TableRentalPackageDto,
} from '../lib/booking';
import { useRole } from '../context/RoleContext';
import { useAuthDialog } from '../context/AuthDialogContext';
import { CardElement, useElements, useStripe } from '@stripe/react-stripe-js';
import { useNavigate } from 'react-router-dom';

const INFO_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

/* Readonly calendar helpers  */
const DAY_NAMES = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

const VISIBLE_START_HOUR = 7;
const VISIBLE_END_HOUR   = 22;
const PX_PER_HOUR        = 50;
const VISIBLE_HOURS      = VISIBLE_END_HOUR - VISIBLE_START_HOUR;
const COLUMN_HEIGHT      = PX_PER_HOUR * VISIBLE_HOURS;
const HOURS_TICKS        = Array.from({ length: VISIBLE_HOURS + 1 }, (_, i) => VISIBLE_START_HOUR + i);

/* Calendar surface styling */
const CALENDAR_SURFACE_BG =
  'linear-gradient(180deg, rgba(67, 27, 128, 0.86) 0%, rgba(41, 15, 82, 0.97) 100%)';
const GRID_LINE = 'rgba(255,255,255,0.18)';
const HOUR_LINE = 'rgba(255,255,255,0.10)';
const TIME_GUTTER_TEXT = 'rgba(255,255,255,0.85)';

/* Event pill base color (purple-tinted); border uses program accent */
const PILL_BASE = '#7C3AED'; // purple-600

function startOfWeek(d: Date): Date {
  const day = d.getDay(); // 0=Sun..6=Sat
  const mondayDelta = (day === 0 ? -6 : 1 - day);
  const res = new Date(d);
  res.setHours(0, 0, 0, 0);
  res.setDate(res.getDate() + mondayDelta);
  return res;
}
function addDays(d: Date, n: number): Date {
  const res = new Date(d);
  res.setDate(res.getDate() + n);
  return res;
}
function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}
function yFromDate(d: Date) {
  const h = d.getHours() + d.getMinutes() / 60;
  return (h - VISIBLE_START_HOUR) * PX_PER_HOUR;
}
function clamp(n: number, min: number, max: number) { return Math.max(min, Math.min(max, n)); }

/** Deterministic color per program id (used only for a subtle border accent) */
function programColor(id: number | null | undefined): string {
  const palette = [
    '#4F46E5', '#0EA5E9', '#10B981', '#F59E0B', '#EF4444',
    '#8B5CF6', '#14B8A6', '#DC2626', '#2563EB', '#683f6eff',
    '#502f9cff', '#108f80ff', '#966a6aff', '#0a2664a6', '#c918e4ff',
    '#10ca0aff', '#7a9155ff', '#0051ffff', '#00a00dff', '#782485ff',
  ];
  const k = typeof id === 'number' ? id : 0;
  return palette[Math.abs(k) % palette.length];
}

type CalendarEvent = {
  id: string;
  programId: number | null;
  title: string;
  start: Date; // local time
  end: Date;   // local time
};

function layoutDay(events: CalendarEvent[]) {
  const sorted = [...events].sort((a, b) => a.start.getTime() - b.start.getTime());
  type Item = CalendarEvent & { col?: number; cols?: number };
  const items: Item[] = sorted.map(e => ({ ...e }));
  let cluster: Item[] = [];
  let clusterEnd = -1;

  const flush = (list: Item[]) => {
    if (!list.length) return;
    const colsEnd: number[] = [];
    for (const it of list) {
      let placed = false;
      for (let c = 0; c < colsEnd.length; c++) {
        if (colsEnd[c] <= it.start.getTime()) {
          it.col = c;
          colsEnd[c] = it.end.getTime();
          placed = true;
          break;
        }
      }
      if (!placed) {
        it.col = colsEnd.length;
        colsEnd.push(it.end.getTime());
      }
    }
    const total = colsEnd.length || 1;
    list.forEach(it => { it.cols = total; });
  };

  for (const it of items) {
    const st = it.start.getTime();
    if (!cluster.length || st < clusterEnd) {
      cluster.push(it);
      clusterEnd = Math.max(clusterEnd, it.end.getTime());
    } else {
      flush(cluster); cluster = [it]; clusterEnd = it.end.getTime();
    }
  }
  flush(cluster);

  return items.map(it => {
    const top = yFromDate(it.start);
    const bottom = yFromDate(it.end);
    const rawHeight = bottom - top;
    const height = Math.max(18, rawHeight);
    const col = it.col ?? 0;
    const cols = it.cols ?? 1;
    const gapX = 4; // px
    const widthPct = 100 / cols;
    const leftPct = widthPct * col;
    return { ...it, top, height, leftPct, widthPct, gapX };
  });
}

const Column = styled('div')({
  position: 'relative',
  height: COLUMN_HEIGHT,
  borderLeft: `1px solid ${GRID_LINE}`,
});

const HourLine = styled('div')({
  position: 'absolute',
  left: 0,
  right: 0,
  height: 1,
  background: HOUR_LINE,
});

/* Price row helper (unchanged) */
function PriceRow({
  title,
  price,
  children,
}: {
  title: string;
  price: string;
  children?: React.ReactNode;
}) {
  return (
    <Paper
      elevation={3}
      sx={{
        p: { xs: 2, sm: 3 },
        borderRadius: 3,
        backgroundColor: 'rgba(0,0,0,0.65)',
        color: 'common.white',
      }}
    >
      <Box
        sx={{
          display: 'grid',
          gap: 2,
          alignItems: 'center',
          gridTemplateColumns: {
            xs: '1fr',
            sm: '1fr auto',
          },
        }}
      >
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>
            {title}
          </Typography>
          <Box>{children}</Box>
        </Box>

        <Box
          sx={{
            justifySelf: { xs: 'start', sm: 'end' },
            backgroundColor: 'rgba(255,255,255,0.12)',
            border: '1px solid rgba(255,255,255,0.18)',
            px: 2.5,
            py: 1,
            borderRadius: 999,
          }}
          aria-label={`${title} price`}
        >
          <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
            {price}
          </Typography>
        </Box>
      </Box>
    </Paper>
  );
}

/* Payment dialog */

function TableCreditsPaymentDialog({
  open,
  onClose,
  packageId,
  packName,
  onSucceeded,
}: {
  open: boolean;
  onClose: () => void;
  packageId: number | null;
  packName: string | null;
  onSucceeded: (ctx?: { bookingId?: number; packageId?: number }) => void;
}) {
  const stripe = useStripe();
  const elements = useElements();

  const [starting, setStarting]   = useState(false);
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [bookingId, setBookingId] = useState<number | null>(null);
  const [err, setErr]   = useState<string | null>(null);
  const [working, setWorking] = useState(false);

  useEffect(() => {
    (async () => {
      setErr(null);
      setClientSecret(null);
      setBookingId(null);
      if (!open || !packageId) return;
      try {
        setStarting(true);
        const res = await startTableRentalPaymentIntent(packageId);
        if (!res || !res.clientSecret) {
          throw new Error('Unable to start checkout (no client secret).');
        }
        setClientSecret(res.clientSecret);
        setBookingId(res.bookingId ?? null);
      } catch (e: any) {
        const msg = (e?.message || 'Unable to start checkout.');
        setErr(msg);
      } finally {
        setStarting(false);
      }
    })();
  }, [open, packageId]);

  const confirm = async () => {
    if (!stripe || !elements || !clientSecret) return;
    setWorking(true);
    setErr(null);
    try {
      const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: { card: elements.getElement(CardElement)! },
      });
      if (error) {
        setErr(error.message ?? 'Payment failed.');
        setWorking(false);
        return;
      }
      if (paymentIntent?.id) {
        try {
          await finalizeTableRentalAfterPayment({
            paymentIntentId: paymentIntent.id,
            bookingId: bookingId ?? undefined,
          }).catch(() => undefined); // webhook is source of truth
        } catch { /* non-fatal */ }
      }
      onSucceeded({ bookingId: bookingId ?? undefined, packageId: packageId ?? undefined });
      onClose();
    } catch (e: any) {
      setErr(e?.message ?? 'Payment failed.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <Dialog open={open} onClose={() => (!working ? onClose() : null)} fullWidth maxWidth="sm">
      <DialogTitle>Buy Table Rental Credits {packName ? `- ${packName}` : ''}</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        {starting && <Box sx={{ textAlign: 'center', py: 2 }}><CircularProgress /></Box>}
        {!starting && clientSecret && (
          <Box sx={{ mt: 1 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>Payment Details</Typography>
            <Box sx={{ p:2, border:'1px solid #ddd', borderRadius:1 }}>
              <CardElement options={{ hidePostalCode: true }} />
            </Box>
            <Typography variant="caption" sx={{ display: 'block', mt: 1, color: 'text.secondary' }}>
              Payments are processed securely by Stripe.
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={working}>Cancel</Button>
        <Button onClick={confirm} variant="contained" disabled={!clientSecret || working || starting}>
          {working ? 'Processing…' : 'Pay'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/* Page */

export default function TableRentals() {
  const { role } = useRole();
  const { open: openAuth } = useAuthDialog();
  const navigate = useNavigate();

  /* Readonly schedule state (public data only) */
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [events, setEvents]   = useState<CalendarEvent[]>([]);

  // Fixed to current (local) week; no interactivity
  const weekStart = useMemo(() => startOfWeek(new Date()), []);
  const weekDays  = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)), [weekStart]);

  // Purchase section state
  const [packs, setPacks] = useState<TableRentalPackageDto[]>([]);
  const [packsErr, setPacksErr] = useState<string | null>(null);
  const [buyingId, setBuyingId] = useState<number | null>(null);
  const [buyingName, setBuyingName] = useState<string | null>(null);
  const [purchaseMsg, setPurchaseMsg] = useState<string | null>(null);

  // client membership gating (must have active INITIAL)
  const [hasInitial, setHasInitial]   = useState<boolean | null>(null); // null = unknown (fail-open)
  const [memLoading, setMemLoading]   = useState(false);
  const [memErr, setMemErr]           = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true); setError(null);
      try {
        // Pull a wide window so admin changes for up to 12 weeks are reflected
        const now = new Date();
        const from = new Date(now); from.setDate(from.getDate() - 7 * 12);
        const to   = new Date(now); to.setDate(to.getDate() + 7 * 12);
        const toIsoDate = (d: Date) => d.toISOString().slice(0, 10);

        const [occ, pubProgs] = await Promise.all([
          fetchPublicOccurrences(toIsoDate(from), toIsoDate(to)).then(r => r ?? []),
          fetchPublicPrograms().then(r => r ?? []),
        ]);

        const activeIds = new Set(pubProgs.filter(p => p.active).map(p => p.id));
        const mapped: CalendarEvent[] = (occ as ProgramOccurrenceDto[])
          .filter(o => (o.programId == null) || activeIds.has(o.programId))
          .map((o, idx) => ({
            id: `${o.programId ?? 'na'}-${idx}-${o.start}`,
            programId: o.programId ?? null,
            title: (o.title ?? 'Program').trim(),
            start: new Date(o.start),
            end: new Date(o.end),
          }));

        setEvents(mapped);
      } catch (e: any) {
        setError(e?.message ?? 'Failed to load schedule.');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  // Fetch table credit packages (public)
  useEffect(() => {
    (async () => {
      setPacksErr(null);
      try {
        const list = await fetchTableRentalPackages();
        setPacks((list ?? []).slice().sort((a, b) => {
          const ao = (a.sortOrder ?? 0), bo = (b.sortOrder ?? 0);
          return ao - bo || a.priceCad - b.priceCad;
        }));
      } catch (e: any) {
        setPacksErr(e?.message ?? 'Failed to load credit packages.');
      }
    })();
  }, []);

  // Load membership status for CLIENTs (guards buying if no INITIAL)
  useEffect(() => {
    let cancelled = false;
    if (role !== 'CLIENT') {
      setHasInitial(null);
      return;
    }
    (async () => {
      setMemLoading(true);
      setMemErr(null);
      try {
        const my = await fetchMyMemberships();
        if (cancelled) return;
        if (my?.memberships && my.memberships.length > 0) {
          const has = my.memberships.some(m =>
            !!m.active && String(m.planType ?? '').toUpperCase() === 'INITIAL'
          );
          setHasInitial(has);
        } else {
          // Unknown - fail-open to avoid blocking on older versions
          setHasInitial(null);
        }
      } catch (e: any) {
        if (cancelled) return;
        setMemErr(e?.message ?? 'Unable to verify membership status.');
        setHasInitial(null);
      } finally {
        if (!cancelled) setMemLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [role]);

  const weekEventsByDay = useMemo(() => {
    const map: Record<number, CalendarEvent[]> = { 0:[],1:[],2:[],3:[],4:[],5:[],6:[] };
    for (const ev of events) {
      for (let i = 0; i < 7; i++) {
        const d = weekDays[i];
        if (sameDay(ev.start, d)) { map[i].push(ev); break; }
      }
    }
    return map;
  }, [events, weekDays]);

  const openBuy = (p: TableRentalPackageDto) => {
    setBuyingId(p.id);
    setBuyingName(p.name);
  };

  const onPurchaseSucceeded = (ctx?: { bookingId?: number; packageId?: number }) => {
    const id = ctx?.bookingId;
    const pkg = ctx?.packageId;
    if (id) {
      const qs = new URLSearchParams();
      qs.set('bookingId', String(id));
      if (pkg) qs.set('packageId', String(pkg));
      navigate(`/thank-you/table-credits?${qs.toString()}`);
      return;
    }
    // Fallback message if finalize returns no id (older servers)
    setPurchaseMsg('Payment succeeded. Your table credits will be available momentarily.');
  };

  const roleBlocked = role === 'ADMIN' || role === 'OWNER' || role === 'COACH';

  return (
    <Box sx={{ width: '100%', background: INFO_GRADIENT, py: { xs: 6, md: 10 } }}>
      {/* Public Program Schedule */}
      <Container maxWidth="md" sx={{ mb: { xs: 5, md: 8 } }}>
        <Typography
          variant="h2"
          align="center"
          gutterBottom
          sx={{ fontWeight: 800, color: 'common.white' }}
        >
          Program Schedule
        </Typography>

        <Typography variant="h6" align="center" sx={{ color: 'common.white', opacity: 0.9, mb: 3 }}>
          See what's on this week—coaching, clinics, and more. Discover your next session!
        </Typography>

        {error && (
          <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
            <Alert severity="error">{error}</Alert>
          </Paper>
        )}

        {loading ? (
          <Box sx={{ textAlign:'center', py: 6 }}>
            <CircularProgress color="inherit" />
          </Box>
        ) : (
          <Paper
            sx={{
              p: 0,
              borderRadius: 3,
              background: CALENDAR_SURFACE_BG,
              overflow: 'hidden',
              color: 'common.white',
            }}
          >
            {/* Header row: time gutter + weekdays (no dates) */}
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: '72px repeat(7, 1fr)',
                borderBottom: `1px solid ${GRID_LINE}`,
              }}
            >
              <Box />
              {DAY_NAMES.map((name, i) => (
                <Box
                  key={i}
                  sx={{
                    p: 1.5,
                    borderLeft: `1px solid ${GRID_LINE}`,
                    display: 'grid',
                    placeItems: 'center',
                    textAlign: 'center',
                  }}
                >
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, color: 'common.white' }}>
                    {name}
                  </Typography>
                </Box>
              ))}
            </Box>

            {/* Grid body (read-only) */}
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: '72px repeat(7, 1fr)',
                height: COLUMN_HEIGHT + 40,
                overflowX: 'auto',
                overflowY: 'hidden',
              }}
            >
              {/* time gutter */}
              <Box
                sx={{
                  position: 'relative',
                  height: COLUMN_HEIGHT,
                  borderRight: `1px solid ${GRID_LINE}`,
                }}
              >
                {HOURS_TICKS.map(h => (
                  <Box
                    key={h}
                    sx={{
                      position: 'absolute',
                      top: (h - VISIBLE_START_HOUR) * PX_PER_HOUR + 2,
                      right: 15,
                      fontSize: 11,
                      color: TIME_GUTTER_TEXT,
                    }}
                  >
                    <Typography variant="body2" sx={{ fontWeight: 700, color: 'common.white' }}>
                        {`${h}:00`}
                    </Typography>
                  </Box>
                ))}
              </Box>

              {/* day columns */}
              {weekDays.map((_, dayIdx) => {
                const laid = layoutDay(weekEventsByDay[dayIdx] ?? []);
                return (
                  <Column key={dayIdx} sx={{ pointerEvents: 'none' }}>
                    {/* hour lines */}
                    {HOURS_TICKS.map(h => (
                      <HourLine key={h} style={{ top: (h - VISIBLE_START_HOUR) * PX_PER_HOUR }} />
                    ))}

                    {/* events */}
                    {laid.map(ev => {
                      const accent = programColor(ev.programId);
                      const bg     = alpha(PILL_BASE, 0.6);
                      const border = alpha(accent, 0.85);
                      const topPx    = clamp(ev.top, 0, COLUMN_HEIGHT - 18);
                      const bottomPx = clamp(ev.top + ev.height, 0, COLUMN_HEIGHT);
                      const heightPx = Math.max(18, bottomPx - topPx);
                      if (heightPx <= 0) return null;

                      return (
                        <Box
                          key={ev.id}
                          sx={{
                            position: 'absolute',
                            top: topPx,
                            left: `calc(${ev.leftPct}% + 2px)`,
                            width: `calc(${ev.widthPct}% - ${ev.gapX + 4}px)`,
                            height: heightPx,
                            bgcolor: bg,
                            border: `1px solid ${border}`,
                            borderRadius: 2,
                            overflow: 'hidden',
                            boxShadow: 1,
                          }}
                        >
                          <Box sx={{ px: 1.25, py: 0.5 }}>
                            <Typography
                              variant="caption"
                              sx={{ display:'block', fontWeight: 700, color: 'common.white', lineHeight: 1.2 }}
                              title={ev.title || 'Program'}
                            >
                              {ev.title || 'Program'}
                            </Typography>
                          </Box>
                        </Box>
                      );
                    })}
                  </Column>
                );
              })}
            </Box>
          </Paper>
        )}
      </Container>

      <Container maxWidth="md">
        <Typography
          variant="h2"
          align="center"
          gutterBottom
          sx={{ fontWeight: 800, color: 'common.white' }}
        >
          Table Rentals
        </Typography>

        <Typography
          variant="h6"
          align="center"
          sx={{ color: 'common.white', opacity: 0.9, mb: 4 }}
        >
          Transparent pricing for members and non-members. Purchase a package today and save!
        </Typography>

        

        {/* Pricing / info */}
        <Stack spacing={3}>
          {/* Non-Members */}
          <PriceRow title="Non-Members" price="$15 + tax">
            <Typography>
              Table rental per hour for all non-members.
            </Typography>
          </PriceRow>

          {/* Club Members */}
          <PriceRow title="Club Members" price="$10 + tax">
            <Stack spacing={0.5}>
              <Typography>
                Table rental per hour for all club members.
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>
                *Club membership $50 + tax per year.
              </Typography>
            </Stack>
          </PriceRow>

          {/* Drop-in Sessions */}
          <PriceRow title="Drop-in Sessions" price="$5 + tax">
            <List dense sx={{ color: 'inherit', pl: 0 }}>
              <ListItem sx={{ py: 0, px: 0 }}>
                <ListItemText
                  primary="Mondays to Fridays from 9:00am to 11:00am and 11:00am to 1:00pm."
                  slotProps={{ primary: { variant: 'body2' } }}
                />
              </ListItem>
              <ListItem sx={{ py: 0, px: 0  }}>
                <ListItemText
                  primary="Saturdays from 9:00am to 11:00am, and Sundays from 7:00pm to 9:00pm."
                  slotProps={{ primary: { variant: 'body2' } }}
                />
              </ListItem>
              <ListItem sx={{ py: 0, px: 0  }}>
                <ListItemText
                  primary="$5 plus tax per 2-hour block per person. Available to non-members."
                  slotProps={{ primary: { variant: 'body2' } }}
                />
              </ListItem>
            </List>
          </PriceRow>

          {/* Need a Racket? */}
          <PriceRow title="Need a Racket?" price="$2 + tax">
            <Typography>
              Rent a non-professional racket for $2 plus tax for your playing time.
            </Typography>
          </PriceRow>
        </Stack>

        <Typography
          variant="h3"
          align="center"
          gutterBottom
          sx={{ fontWeight: 800, color: 'common.white', mt: 3 }}
        >
          Purchase Table Hours
        </Typography>

        <Typography
          variant="h6"
          align="center"
          sx={{ color: 'common.white', opacity: 0.9, mb: 4 }}
        >
          Choose from any number of packages online and avoid hassle at the front desk!
        </Typography>

        {/* Buy Table Rental Credits */}
        {packsErr && (
          <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
            <Alert severity="error">{packsErr}</Alert>
          </Paper>
        )}

        {purchaseMsg && (
          <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
            <Alert severity="success">{purchaseMsg}</Alert>
          </Paper>
        )}

        {memErr && role === 'CLIENT' && (
          <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
            <Alert severity="warning">{memErr}</Alert>
          </Paper>
        )}

        <Stack spacing={2} maxWidth="md" sx={{ mt: 4 }} >
          {packs.map(p => {
            const roleBlocked = role === 'ADMIN' || role === 'OWNER' || role === 'COACH';
            return (
              <Paper
                key={p.id}
                elevation={3}
                sx={{
                  p: { xs: 2, sm: 3 },
                  borderRadius: 3,
                  backgroundColor: 'rgba(0,0,0,0.65)',
                  color: 'common.white'
                }}
              >
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }}>
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="h5" sx={{ fontWeight: 700 }}>
                      {p.name}
                    </Typography>
                    <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                      <Chip size="small" variant="outlined" sx={{ color: 'common.white' }} label={`${p.hours} hour${p.hours === 1 ? '' : 's'}`} />
                      <Chip size="small" variant="outlined" sx={{ color: 'common.white' }} label={`$${p.priceCad.toFixed(2)} + tax`} />
                    </Stack>
                  </Box>

                  {role === 'GUEST' && (
                    <Button variant="contained" onClick={() => openAuth()} sx={{ whiteSpace: 'nowrap' }}>
                      Log in to buy
                    </Button>
                  )}

                  {roleBlocked && (
                    <Button variant="outlined" disabled sx={{ whiteSpace: 'nowrap' }}>
                      Not available for staff accounts
                    </Button>
                  )}

                  {!roleBlocked && role === 'CLIENT' && hasInitial === false && (
                    <Button variant="outlined" disabled sx={{ whiteSpace: 'nowrap' }}>
                      Membership required
                    </Button>
                  )}

                  {!roleBlocked && role === 'CLIENT' && hasInitial !== false && (
                    <Button
                      variant="contained"
                      onClick={() => openBuy(p)}
                      sx={{ whiteSpace: 'nowrap' }}
                      disabled={memLoading}
                    >
                      {memLoading ? 'Checking…' : 'Buy'}
                    </Button>
                  )}
                </Stack>
              </Paper>
            );
          })}
          {packs.length === 0 && !packsErr && (
            <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)' }}>
              <Alert severity="info">No table credit packages are available yet. Please check back soon.</Alert>
            </Paper>
          )}
        </Stack>
      </Container>

      {/* Payment dialog */}
      <TableCreditsPaymentDialog
        open={!!buyingId}
        onClose={() => { setBuyingId(null); setBuyingName(null); }}
        packageId={buyingId}
        packName={buyingName}
        onSucceeded={onPurchaseSucceeded}
      />
    </Box>
  );
}
