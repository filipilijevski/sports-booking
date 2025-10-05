import { useEffect, useMemo, useState } from 'react';
import {
  Box, Container, Typography, Paper, Stack, Chip, CircularProgress,
  Button, Dialog, DialogTitle, DialogContent, DialogActions,
  Alert, Divider, RadioGroup, FormControlLabel, Radio, Tooltip,
  Backdrop, Skeleton
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import { useAuthDialog } from '../context/AuthDialogContext';
import { useRole } from '../context/RoleContext';
import {
  fetchPublicPrograms,
  fetchPublicMembershipPlans,
  fetchMyMemberships,
  hasActiveEnrollment,
  startMembershipCheckout,
  // legacy (kept): hosted checkouts
  startProgramCheckout,
  startProgramEnrollmentCheckout,
  // inline program checkout helpers
  fetchProgramQuote,
  startProgramPaymentIntent,
  finalizeProgramEnrollmentAfterPayment,
  fetchMembershipQuote,
  finalizeMembershipAfterPayment,
  type ProgramCardDto,
  type MembershipPlanDto,
  type ProgramPackageDto,
  type MyMembershipsPayload,
} from '../lib/booking';
import { CardElement, useElements, useStripe } from '@stripe/react-stripe-js';

/* Page + Card Colors */
const PAGE_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

// Program cards
const PROGRAM_CARD_BG   = 'rgba(0,0,0,0.65)';
const PROGRAM_CARD_TEXT = '#ffffff';

// Membership cards
const MEMBERSHIP_CARD_BG   = 'rgba(15,15,15,0.7)';
const MEMBERSHIP_CARD_TEXT = '#ffffff';

/* helpers */
const BLOCK_MSG = 'Admins, Coaches or Owners cannot enroll into Memberships or Programs';
const REQUIRE_INITIAL_MSG = 'You must purchase an Initial/Annual Club Membership before purchasing other memberships or enrolling in programs.';
const INVITE_ONLY_MSG = 'This program is by invitation only. Please contact the club for enrollment.';
const ALREADY_ENROLLED_MSG = 'You already have an active enrollment for this program.';

/* Stripe overlay during processing (kept) */
function StripeRedirectOverlay({ open, message }: { open: boolean; message: string }) {
  return (
    <Backdrop open={open} sx={{ color: '#fff', zIndex: (t) => t.zIndex.modal + 1 }}>
      <Paper elevation={6} sx={{ p: 3, borderRadius: 3, minWidth: 320, maxWidth: 420, width: '90%' }}>
        <Typography variant="h6" sx={{ mb: 1, fontWeight: 800, textAlign: 'center' }}>
          Contacting Stripe...
        </Typography>
        <Stack spacing={1}>
          <Skeleton variant="rectangular" height={18} />
          <Skeleton variant="rectangular" height={18} />
          <Skeleton variant="rectangular" height={18} />
          <Skeleton variant="rectangular" height={18} />
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1.5, display: 'block', textAlign: 'center' }}>
          {message}
        </Typography>
      </Paper>
    </Backdrop>
  );
}

export default function Coaching() {
  const [programs, setPrograms] = useState<ProgramCardDto[]>([]);
  const [plans, setPlans] = useState<MembershipPlanDto[]>([]);
  const [my, setMy] = useState<MyMembershipsPayload | null>(null);
  const [loading, setLoading] = useState(true);

  const { role } = useRole();
  const blockedRole = role === 'ADMIN' || role === 'OWNER' || role === 'COACH';

  // Treat any known role as authenticated (works in cookie-auth and token modes)
  const authed = role === 'CLIENT' || role === 'COACH' || role === 'ADMIN' || role === 'OWNER';

  const { open } = useAuthDialog();

  useEffect(() => {
    (async () => {
      try {
        const [p, m] = await Promise.all([
          fetchPublicPrograms().catch(() => []),
          fetchPublicMembershipPlans().catch(() => []),
        ]);
        setPrograms(p ?? []);
        setPlans((m ?? []).filter(pl => pl.active));

        // Always attempt to fetch "my" memberships; swallow 401/404 gracefully.
        const mine = await fetchMyMemberships().catch(() => null);
        setMy(mine ?? null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const hasInitial = useMemo(
    () => !!my?.memberships?.some(mm => (mm?.planType === 'INITIAL' || mm?.planName?.toLowerCase().includes('initial')) && mm.active),
    [my?.memberships]
  );

  return (
    <Box sx={{ width: '100%', background: PAGE_GRADIENT, py: { xs: 6, md: 10 } }}>
      <Container maxWidth="xl">
        <Typography variant="h2" align="center" gutterBottom sx={{ fontWeight: 800, color: 'common.white' }}>
          Coaching & Programs
        </Typography>
        <Typography variant='h6' align="center" sx={{ color: 'common.white', opacity: 0.92, mb: 4, maxWidth: 900, mx: 'auto' }}>
          Explore memberships and training paths for every level - from first rallies to tournament play.
        </Typography>

        {blockedRole && (
          <Paper sx={{ p:2, mb:3, borderRadius:3, background:alpha('#000',0.55), color:'white' }}>
            <Alert severity="info" sx={{ background:'transparent', color:'inherit' }}>
              {BLOCK_MSG}
            </Alert>
          </Paper>
        )}

        {loading ? (
          <Box sx={{ textAlign:'center', py:6, color:'white' }}>
            <CircularProgress />
          </Box>
        ) : programs.length === 0 ? (
          <Paper sx={{ p:3, borderRadius:3, background:alpha('#000',0.55), color:'white', mb:6 }}>
            <Typography>No active programs yet. Please check back soon.</Typography>
          </Paper>
        ) : (
          <Stack spacing={3.5} sx={{ mb: 6 }}>
            {programs.map(p => (
              <ProgramCard
                key={p.id}
                p={p}
                my={my}
                onRequireAuth={open}
                blockedRole={blockedRole}
                hasInitial={hasInitial}
                authed={authed}
              />
            ))}
          </Stack>
        )}

        <Typography variant="h2" align="center" gutterBottom sx={{ fontWeight: 800, color: 'common.white', mt: { xs: 2, md: 4 } }}>
          Memberships
        </Typography>
        <Typography variant='h6' align="center" sx={{ color: 'common.white', opacity: 0.92, mb: 4, maxWidth: 900, mx: 'auto' }}>
          Join the club and unlock practice hours, program credits, and tournament perks.
        </Typography>

        {loading ? (
          <Box sx={{ textAlign:'center', py:6, color:'white' }}>
            <CircularProgress />
          </Box>
        ) : plans.length === 0 ? (
          <Paper sx={{ p:3, borderRadius:3, background:alpha('#000',0.55), color:'white' }}>
            <Typography>No active memberships right now.</Typography>
          </Paper>
        ) : (
          <Stack spacing={3.5}>
            {plans.map(pl => (
              <MembershipCard
                key={pl.id}
                plan={pl}
                my={my}
                onRequireAuth={open}
                blockedRole={blockedRole}
                hasInitial={hasInitial}
                authed={authed}
              />
            ))}
          </Stack>
        )}

      </Container>
    </Box>
  );
}

function ProgramCard({
  p, my, onRequireAuth, blockedRole, hasInitial, authed,
}: {
  p: ProgramCardDto;
  my: MyMembershipsPayload | null;
  onRequireAuth: () => void;
  blockedRole: boolean;
  hasInitial: boolean;
  authed: boolean;
}) {
  const week = useMemo(() => {
    const m = new Map<string, { start:string; end:string; coach?:string|null }[]>();
    p.weekly.forEach(w => { m.set(w.weekday, w.times); });
    return m;
  }, [p.weekly]);

  const [enrollOpen, setEnrollOpen] = useState(false);
  const [checking, setChecking] = useState(false);
  const [alreadyRemote, setAlreadyRemote] = useState(false);

  // derive already-enrolled from /my/enrollments so we can disable the button pre-click
  const alreadyFromMy = useMemo(
    () => !!my?.enrollments?.some(e => e.programId === p.id && String(e.status).toUpperCase() === 'ACTIVE'),
    [my?.enrollments, p.id]
  );

  const alreadyEnrolled = alreadyFromMy || alreadyRemote;
  const activePackages = p.packages.filter(pk => pk.active);

  const handleOpenEnroll = async () => {
    if (!authed) { onRequireAuth(); return; }
    setEnrollOpen(true);
    // If we already know from /my/enrollments, no need to ping server
    if (alreadyFromMy) {
      setAlreadyRemote(true);
      return;
    }
    setChecking(true);
    try {
      const active = await hasActiveEnrollment(p.id);
      setAlreadyRemote(active);
    } finally {
      setChecking(false);
    }
  };

  const isInviteOnly = (p.enrollmentMode ?? 'OPEN') === 'ADMIN_ONLY';

  const blockTooltip =
    blockedRole ? BLOCK_MSG :
    (!hasInitial ? REQUIRE_INITIAL_MSG :
    (isInviteOnly ? INVITE_ONLY_MSG :
    (checking ? 'Checking your enrollments...' :
    (alreadyEnrolled ? ALREADY_ENROLLED_MSG : ''))));

  const enrollDisabled = blockedRole || !hasInitial || checking || alreadyEnrolled || isInviteOnly;

  return (
    <Paper elevation={3} sx={{ p: { xs: 2.5, sm: 3.5 }, borderRadius: 3, backgroundColor: PROGRAM_CARD_BG, color: PROGRAM_CARD_TEXT }}>
      <Typography variant="h4" sx={{ fontWeight: 800, mb: 1 }}>{p.title}</Typography>

      {!!p.packages.length && (
        <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" sx={{ mb: 1 }}>
          {activePackages.map(pk => (
            <Chip
              key={pk.id}
              variant="outlined"
              label={`${pk.name} • $${pk.priceCad.toFixed(2)} • ${pk.sessionsCount} sessions`}
              sx={{
                color: PROGRAM_CARD_TEXT,
                borderColor: alpha(PROGRAM_CARD_TEXT, 0.45),
              }}
            />
          ))}
        </Stack>
      )}

      {p.description && (
        <Typography variant="body1" sx={{ opacity: 0.95, mb: 2 }}>{p.description}</Typography>
      )}

      {/* Weekly schedule grid */}
      <Box sx={{ display:'grid', gridTemplateColumns: { xs:'1fr', md:'repeat(7, 1fr)' }, gap:1 }}>
        {['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'].map((dayKey, idx) => {
          const entries = week.get(dayKey) ?? [];
          const abbr = ['MON','TUE','WED','THU','FRI','SAT','SUN'][idx];
          return (
            <Box key={abbr} sx={{ border:`1px solid ${alpha(PROGRAM_CARD_TEXT,0.18)}`, borderRadius: 2, p: 1.2, minHeight: 84 }}>
              <Typography variant="subtitle2" sx={{ opacity: 0.9, mb: 0.5, color: PROGRAM_CARD_TEXT }}>{abbr}</Typography>
              {entries.length === 0 ? (
                <Typography variant="caption" sx={{ opacity: 0.7, color: PROGRAM_CARD_TEXT }}>-</Typography>
              ) : (
                <Stack spacing={0.5}>
                  {entries.map((t, i) => (
                    <Typography key={i} variant="caption" sx={{ display:'block', color: PROGRAM_CARD_TEXT }}>
                      {t.start}-{t.end}{t.coach ? ` • ${t.coach}` : ''}
                    </Typography>
                  ))}
                </Stack>
              )}
            </Box>
          );
        })}
      </Box>

      {!!p.coaches.length && (
        <Typography variant="subtitle2" sx={{ mt: 2, fontWeight: 700, color: PROGRAM_CARD_TEXT }}>
          Coaches: {p.coaches.join(', ')}
        </Typography>
      )}

      {activePackages.length > 0 && (
        <Stack direction="row" spacing={1.5} sx={{ mt: 2 }}>
          <Tooltip title={blockTooltip}>
            <span>
              <Button
                variant="contained"
                onClick={handleOpenEnroll}
                disabled={enrollDisabled}
              >
                Enroll
              </Button>
            </span>
          </Tooltip>
        </Stack>
      )}

      <ProgramEnrollDialog
        open={enrollOpen}
        onClose={() => setEnrollOpen(false)}
        programTitle={p.title}
        programId={p.id}
        packages={activePackages}
        blockedRole={blockedRole}
        hasInitial={hasInitial}
        inviteOnly={isInviteOnly}
        alreadyEnrolled={alreadyEnrolled}
      />
    </Paper>
  );
}

function ProgramEnrollDialog({
  open, onClose, programTitle, programId, packages, blockedRole, hasInitial, inviteOnly, alreadyEnrolled,
}: {
  open: boolean;
  onClose: () => void;
  programTitle: string;
  programId: number;
  packages: ProgramPackageDto[];
  blockedRole: boolean;
  hasInitial: boolean;
  inviteOnly: boolean;
  alreadyEnrolled: boolean;
}) {
  const stripe = useStripe();
  const elements = useElements();

  const [selected, setSelected] = useState<number | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [priceCad, setPrice] = useState<number | null>(null);
  const [taxCad, setTax]     = useState<number | null>(null);
  const [totalCad, setTotal] = useState<number | null>(null);
  const [currency, setCurr]  = useState<string>('CAD');

  const [clientSecret, setCS] = useState<string | null>(null);
  const [bookingId, setBookingId] = useState<number | null>(null);

  const [phase, setPhase] = useState<'REVIEW'|'PAY'>('REVIEW');

  // cache quotes for instant display on selection
  const [quotes, setQuotes] = useState<Record<number, { priceCad: number; taxCad: number; totalCad: number; currency: string }>>({});
  const [quotesLoading, setQuotesLoading] = useState(false);

  useEffect(() => {
    if (open) {
      const first = packages.find(p => p.active)?.id ?? null;
      setSelected(first);
      setErr(null);
      setPhase('REVIEW');
      setCS(null);
      setBookingId(null);
      setPrice(null); setTax(null); setTotal(null); setCurr('CAD');

      // Prefetch quotes for all active packages to minimize latency in UI
      (async () => {
        setQuotesLoading(true);
        try {
          const entries = await Promise.all(
            packages
              .filter(p => p.active)
              .map(async (pk) => {
                const q = await fetchProgramQuote(pk.id).catch(() => undefined);
                if (q) return [pk.id, q] as const;
                return [pk.id, { priceCad: Number(pk.priceCad || 0), taxCad: 0, totalCad: Number(pk.priceCad || 0), currency: 'CAD' }] as const;
              })
          );
          const next: Record<number, { priceCad: number; taxCad: number; totalCad: number; currency: string }> = {};
          for (const [id, q] of entries) next[id] = q;
          setQuotes(next);
          // If we have a selection, seed the summary immediately
          if (first && next[first]) {
            setPrice(next[first].priceCad);
            setTax(next[first].taxCad);
            setTotal(next[first].totalCad);
            setCurr(next[first].currency);
          }
        } finally {
          setQuotesLoading(false);
        }
      })();
    }
  }, [open, packages]);

  const sel = packages.find(p => p.id === selected) || null;
  const isBlocked = blockedRole || !hasInitial || inviteOnly || alreadyEnrolled;

  // Keep summary in sync with selection using cached quotes
  useEffect(() => {
    if (!open || !sel) return;
    const q = quotes[sel.id];
    if (q) {
      setPrice(q.priceCad);
      setTax(q.taxCad);
      setTotal(q.totalCad);
      setCurr(q.currency);
    } else {
      // Fallback instantly to package price and show loading UI for tax/total until quote arrives
      setPrice(Number(sel.priceCad || 0));
      setTax(null);
      setTotal(null);
      setCurr('CAD');
      // On-demand fetch (rare if prefetch covered it)
      (async () => {
        const fetched = await fetchProgramQuote(sel.id).catch(() => undefined);
        if (fetched) {
          setQuotes(prev => ({ ...prev, [sel.id]: fetched }));
          if (selected === sel.id) {
            setPrice(fetched.priceCad);
            setTax(fetched.taxCad);
            setTotal(fetched.totalCad);
            setCurr(fetched.currency);
          }
        }
      })();
    }
  }, [open, sel?.id, quotes, selected, sel]);

  const begin = async () => {
    if (!sel || isBlocked) return;
    setBusy(true); setErr(null);
    try {
      const res = await startProgramPaymentIntent(sel.id);

      // Inline PI vs legacy hosted
      if ('clientSecret' in res) {
        setCS(res.clientSecret);
        if (res.priceCad != null) setPrice(res.priceCad);
        if (res.taxCad   != null) setTax(res.taxCad);
        if (res.totalCad != null) setTotal(res.totalCad);
        if (res.currency)         setCurr(res.currency);
        setBookingId(res.bookingId ?? null);
        setPhase('PAY');
        return;
      }

      if ('url' in res) {
        // Legacy server only - hosted checkout (kept for bwc)
        window.location.assign(res.url);
        return;
      }

      throw new Error('Checkout is not available at the moment.');
    } catch (e: any) {
      const msg = (e?.message || '');
      const code = (e?.code || '').toString();
      const status = Number(e?.status || 0);

      if (code === 'DUPLICATE_ENROLLMENT' || status === 409 || msg.toLowerCase().includes('active enrollment')) {
        setErr(ALREADY_ENROLLED_MSG);
      } else if (msg.toLowerCase().includes('initial annual club membership is required')) {
        setErr(REQUIRE_INITIAL_MSG);
      } else if (msg.toLowerCase().includes('invitation only') || msg.toLowerCase().includes('admin only')) {
        setErr(INVITE_ONLY_MSG);
      } else {
        setErr(msg || 'Unable to start checkout for this program package.');
      }
    } finally {
      setBusy(false);
    }
  };

  const pay = async () => {
    if (!stripe || !elements || !clientSecret || !sel) return;
    setBusy(true); setErr(null);
    try {
      const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: { card: elements.getElement(CardElement)! },
      });
      if (error) throw new Error(error.message || 'Payment failed');

      if (paymentIntent?.id) {
        await finalizeProgramEnrollmentAfterPayment({
          paymentIntentId: paymentIntent.id,
          bookingId: bookingId ?? undefined,
          programPackageId: sel.id,
        }).catch(() => {});
      }

      const q = new URLSearchParams();
      if (bookingId) q.set('bookingId', String(bookingId));
      q.set('programId', String(programId));
      q.set('packageId', String(sel.id));
      window.location.assign(`/thank-you/program?${q.toString()}`);
    } catch (e: any) {
      setErr(e?.message || 'Payment failed');
    } finally {
      setBusy(false);
    }
  };

  const fmt = (n: number | null) => n == null ? '' : n.toFixed(2);
  const showTaxLoader   = (taxCad == null) || (quotesLoading && !!sel && !quotes[sel.id]);
  const showTotalLoader = (totalCad == null) || (quotesLoading && !!sel && !quotes[sel.id]);

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>Enroll in {programTitle}</DialogTitle>
        <DialogContent dividers>
          {isBlocked && (
            <Alert severity="info" sx={{ mb: 2 }}>
              {blockedRole ? BLOCK_MSG : inviteOnly ? INVITE_ONLY_MSG : (!hasInitial ? REQUIRE_INITIAL_MSG : ALREADY_ENROLLED_MSG)}
            </Alert>
          )}
          {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}

          {/* Package selection stays the same */}
          <Typography variant="subtitle2" sx={{ mb: 1 }}>Choose a package</Typography>
          <RadioGroup
            value={selected ?? ''}
            onChange={e => setSelected(Number(e.target.value))}
          >
            {packages.map(pk => (
              <FormControlLabel
                key={pk.id}
                value={pk.id}
                control={<Radio />}
                label={`${pk.name} - ${pk.sessionsCount} sessions - $${pk.priceCad.toFixed(2)}`}
              />
            ))}
          </RadioGroup>

          <Divider sx={{ my: 2 }} />

          {/* Summary backed by cached server quote */}
          <Typography variant="subtitle2" sx={{ mb: 1 }}>Summary</Typography>
          <Stack spacing={0.5} sx={{ mb: 2 }}>
            <Typography variant="body2">
              Subtotal: ${fmt(priceCad)} {currency}
            </Typography>

            <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              Tax:&nbsp;
              {showTaxLoader ? (
                <Skeleton variant="text" width={48} />
              ) : (
                <>${fmt(taxCad)} {currency}</>
              )}
            </Typography>

            <Typography variant="h6" sx={{ mt: 0.5, display: 'flex', alignItems: 'center', gap: 1 }}>
              Total:&nbsp;
              {showTotalLoader ? (
                <Skeleton variant="text" width={72} />
              ) : (
                <>${fmt(totalCad ?? (priceCad != null && taxCad != null ? priceCad + taxCad : priceCad))} {currency}</>
              )}
            </Typography>
          </Stack>

          {phase === 'PAY' && (
            <>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Payment Details</Typography>
              <Box sx={{ p:2, border:'1px solid #ddd', borderRadius:1 }}>
                <CardElement options={{ hidePostalCode: true }} />
              </Box>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display:'block' }}>
                Your payment is processed securely by Stripe.
              </Typography>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={busy}>Cancel</Button>
          {phase === 'REVIEW' ? (
            <Button
              variant="contained"
              onClick={begin}
              disabled={busy || isBlocked || !selected}
            >
              {busy ? 'Preparing...' : 'Proceed to Payment'}
            </Button>
          ) : (
            <Button
              variant="contained"
              onClick={pay}
              disabled={busy || !clientSecret}
            >
              {busy ? 'Processing...' : `Pay $${fmt(totalCad)}`}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Kept to show progress if needed; not used during inline flow unless network is slow */}
      <StripeRedirectOverlay open={busy && phase === 'PAY'} message="Finalizing your payment..." />
    </>
  );
}

function MembershipCard({
  plan, my, onRequireAuth, blockedRole, hasInitial, authed,
}: {
  plan: MembershipPlanDto;
  my: MyMembershipsPayload | null;
  onRequireAuth: () => void;
  blockedRole: boolean;
  hasInitial: boolean;
  authed: boolean;
}) {
  const duration =
    typeof plan.durationDays === 'number' && plan.durationDays > 0
      ? `${plan.durationDays} day${plan.durationDays === 1 ? '' : 's'}`
      : 'No expiry';

  const hasActiveForPlan = useMemo(() => {
    if (!my?.memberships?.length) return false;
    return my.memberships.some(m =>
      m?.planId === plan.id && m?.active === true
    );
  }, [my, plan.id]);

  const [open, setOpen] = useState(false);

  const requiresInitial = plan.type === 'SPECIAL' && !hasInitial;

  const tooltip =
    blockedRole ? BLOCK_MSG :
    (requiresInitial ? REQUIRE_INITIAL_MSG : (hasActiveForPlan ? 'You already have an active membership for this plan.' : ''));

  const disabled = blockedRole || requiresInitial || hasActiveForPlan;

  return (
    <Paper elevation={3} sx={{ p: { xs: 2.5, sm: 3.5 }, borderRadius: 3, backgroundColor: MEMBERSHIP_CARD_BG, color: MEMBERSHIP_CARD_TEXT }}>
      <Stack spacing={1}>
        <Typography variant="h4" sx={{ fontWeight: 800 }}>{plan.name}</Typography>
        <Typography variant="subtitle2" sx={{ opacity: 0.85 }}>
          Type: {plan.type} • Duration: {duration}
        </Typography>
        <Typography variant="h6" sx={{ fontWeight: 700 }}>
          ${Number(plan.priceCad ?? 0).toFixed(2)} CAD
        </Typography>

        {!!plan.description && (
          <Typography variant="body1" sx={{ opacity: 0.95 }}>
            {plan.description}
          </Typography>
        )}

        {plan.entitlements?.length ? (
          <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" sx={{ mt: 1 }}>
            {plan.entitlements.map(e => (
              <Chip
                key={e.id}
                variant="outlined"
                label={`${e.kind.replace(/_/g,' ')}: ${e.amount}`}
                sx={{
                  color: MEMBERSHIP_CARD_TEXT,
                  borderColor: alpha(MEMBERSHIP_CARD_TEXT, 0.45),
                }}
              />
            ))}
          </Stack>
        ) : null}

        <Stack direction="row" spacing={1.5} sx={{ mt: 2 }}>
          <Tooltip title={tooltip}>
            <span>
              <Button
                variant="contained"
                onClick={() => (authed ? setOpen(true) : onRequireAuth())}
                disabled={disabled}
              >
                Buy Now
              </Button>
            </span>
          </Tooltip>
        </Stack>
      </Stack>

      <MembershipCheckoutDialog
        open={open}
        onClose={() => setOpen(false)}
        plan={plan}
        blockedRole={blockedRole}
        requiresInitial={requiresInitial}
      />
    </Paper>
  );
}

function MembershipCheckoutDialog({
  open, onClose, plan, blockedRole, requiresInitial,
}: {
  open: boolean;
  onClose: () => void;
  plan: MembershipPlanDto;
  blockedRole: boolean;
  requiresInitial: boolean;
}) {
  const stripe = useStripe();
  const elements = useElements();

  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [priceCad, setPrice] = useState<number | null>(null);
  const [taxCad, setTax]     = useState<number | null>(null);
  const [totalCad, setTotal] = useState<number | null>(null);
  const [currency, setCurr]  = useState<string>('CAD');

  const [clientSecret, setCS] = useState<string | null>(null);
  const [bookingId, setBookingId] = useState<number | null>(null);

  const [phase, setPhase] = useState<'REVIEW'|'PAY'>('REVIEW');

  const isBlocked = blockedRole || requiresInitial;

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setErr(null);
    setPhase('REVIEW');
    setCS(null);
    setBookingId(null);
    (async () => {
      try {
        const q = await fetchMembershipQuote(plan.id);
        if (cancelled) return;
        if (q?.priceCad != null) setPrice(q.priceCad);
        if (q?.taxCad != null)   setTax(q.taxCad);
        if (q?.totalCad != null) setTotal(q.totalCad);
        if (q?.currency)         setCurr(q.currency);
      } catch {
        setPrice(Number(plan.priceCad ?? 0));
        setTax(null);
        setTotal(null);
      }
    })();
    return () => { cancelled = true; };
  }, [open, plan.id, plan.priceCad]);

  const begin = async () => {
    if (isBlocked) return;
    setBusy(true); setErr(null);
    try {
      const res = await startMembershipCheckout(plan.id);
      if (!res?.clientSecret) throw new Error('Checkout is not available at the moment.');
      setCS(res.clientSecret);
      if (res.priceCad != null) setPrice(res.priceCad);
      if (res.taxCad   != null) setTax(res.taxCad);
      if (res.totalCad != null) setTotal(res.totalCad);
      if (res.currency)         setCurr(res.currency);
      setBookingId(res.bookingId ?? null);
      setPhase('PAY');
    } catch (e: any) {
      const msg = (e?.message || '');
      if (msg.toLowerCase().includes('initial annual club membership is required')) {
        setErr(REQUIRE_INITIAL_MSG);
      } else {
        setErr(msg || 'Unable to start membership checkout.');
      }
    } finally {
      setBusy(false);
    }
  };

  const pay = async () => {
    if (!stripe || !elements || !clientSecret) return;
    setBusy(true); setErr(null);
    try {
      const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
        payment_method: { card: elements.getElement(CardElement)! },
      });
      if (error) throw new Error(error.message || 'Payment failed');

      if (paymentIntent?.id) {
        await finalizeMembershipAfterPayment({
          paymentIntentId: paymentIntent.id,
          bookingId: bookingId ?? undefined,
        }).catch(() => {});
      }

      const q = new URLSearchParams();
      if (bookingId) q.set('bookingId', String(bookingId));
      q.set('planId', String(plan.id));
      window.location.assign(`/thank-you/membership?${q.toString()}`);
    } catch (e: any) {
      setErr(e?.message || 'Payment failed');
    } finally {
      setBusy(false);
    }
  };

  const fmt = (n: number | null) => n == null ? '—' : n.toFixed(2);

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle>Purchase: {plan.name}</DialogTitle>
        <DialogContent dividers>
          {(blockedRole || requiresInitial) && (
            <Alert severity="info" sx={{ mb: 2 }}>
              {blockedRole ? BLOCK_MSG : REQUIRE_INITIAL_MSG}
            </Alert>
          )}
          {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}

          <Typography variant="subtitle2" sx={{ mb: 1 }}>Summary</Typography>
          <Stack spacing={0.5} sx={{ mb: 2 }}>
            <Typography variant="body2">Plan: {plan.name} ({plan.type})</Typography>
            <Typography variant="body2">Subtotal: ${fmt(priceCad)} {currency}</Typography>
            <Typography variant="body2">Tax: ${fmt(taxCad)} {currency}</Typography>
            <Typography variant="h6" sx={{ mt: 0.5 }}>
              Total: ${fmt(totalCad)} {currency}
            </Typography>
          </Stack>

          {phase === 'PAY' && (
            <>
              <Divider sx={{ my: 2 }} />
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Payment Details</Typography>
              <Box sx={{ p:2, border:'1px solid #ddd', borderRadius:1 }}>
                <CardElement options={{ hidePostalCode: true }} />
              </Box>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display:'block' }}>
                Your payment is processed securely by Stripe.
              </Typography>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={busy}>Cancel</Button>
          {phase === 'REVIEW' ? (
            <Button
              variant="contained"
              onClick={begin}
              disabled={busy || (blockedRole || requiresInitial)}
            >
              {busy ? 'Preparing...' : 'Proceed to Payment'}
            </Button>
          ) : (
            <Button
              variant="contained"
              onClick={pay}
              disabled={busy || !clientSecret}
            >
              {busy ? 'Processing...' : `Pay $${fmt(totalCad)}`}
            </Button>
          )}
        </DialogActions>
      </Dialog>

      <StripeRedirectOverlay open={busy && phase === 'PAY'} message="Finalizing your payment..." />
    </>
  );
}
