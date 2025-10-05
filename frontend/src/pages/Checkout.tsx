import {
  Box, Button, Container, Paper, Typography,
  Stepper, Step, StepLabel, Divider, CircularProgress, RadioGroup,
  FormControlLabel, Radio, Stack, Avatar, TextField,
} from '@mui/material';
import ArrowBackIcon   from '@mui/icons-material/ArrowBack';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import FlashOnIcon       from '@mui/icons-material/FlashOn';
import { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import { useNavigate }  from 'react-router-dom';
import { CardElement, useElements, useStripe } from '@stripe/react-stripe-js';

import { api }         from '../lib/api';
import { useCart }     from '../context/CartContext';
import { useRole }     from '../context/RoleContext';
import ShippingAddressForm      from '../components/ShippingAddressForm';
import type { ShippingAddress } from '../types';
import { shippingSchema }       from '../validation/shippingSchema';
import { canUseCoupon }         from '../lib/coupons';   // early coupon validator

const BG =
  'linear-gradient(270deg,rgba(0,54,126,1) 0%,rgba(181,94,231,1) 100%)';

type StepIdx = 1 | 2 | 3;
type Method  = 'REGULAR' | 'EXPRESS';

type Parcel = {
  grams: number;
  weightKg: number;
  lengthCm?: number | null;
  widthCm?: number | null;
  heightCm?: number | null;
} | null;

type Money = string | number;
type OrderDto = {
  id: number;
  subtotalAmount: Money;
  taxAmount: Money;
  shippingAmount: Money;
  discountAmount: Money;
  totalAmount: Money;
  couponCode?: string | null;
}

export default function Checkout() {
  const { state: cart, clear } = useCart();
  const { role }               = useRole();
  const stripe   = useStripe();
  const elements = useElements();
  const nav      = useNavigate();

  const isGuest = role === 'GUEST';

  /* local state */
  const [step, setStep]     = useState<StepIdx>(1);
  const [address, setAddr]  = useState<ShippingAddress>({
    fullName:'', phone:'', email:'', line1:'', line2:'',
    city:'', province:'', postalCode:'', country:'Canada',
  });
  const [couponCode, setCoupon] = useState<string>('');
  const [method, setMethod] = useState<Method>('REGULAR');

  /* live Canada Post quotes - default to legacy fallback so UI never blanks */
  const [regularFee, setRegFee] = useState(10);
  const [expressFee, setExpFee] = useState(20);
  const [parcel, setParcel]     = useState<Parcel>(null);
  const [quoting, setQuoting]   = useState(false);

  const [clientSecret, setCs] = useState<string>();
  const [serverOrder, setServerOrder] = useState<OrderDto | null>(null);
  const [busy, setBusy]       = useState(false);
  const [err,  setErr]        = useState('');

  /* spinner while validating coupon */
  const [validatingCoupon, setValidatingCoupon] = useState(false);

  const addressIsValid = useMemo(
    () => shippingSchema.isValidSync(address, { strict:true }),
    [address],
  );

  const subtotal  = cart.subtotal;
  const shipping  = method === 'REGULAR' ? regularFee : expressFee;
  const tax       = (subtotal + shipping) * 0.13;
  const grand     = (subtotal + shipping + tax);

  const stripeReady = !!stripe && !!elements;

  useEffect(() => {
    if ((step === 2 || step === 3) && cart.items.length === 0) {
      setErr('Your cart is empty.');
      setCs(undefined);
      setServerOrder(null);
      setStep(1);
    }
  }, [cart.items.length, step]);

  /* Shipping quotes */
  const lastReqId = useRef(0);

  const fetchQuotes = useCallback(async () => {
    if (step !== 2) return;
    if (!addressIsValid) return;

    const body: any = {
      shippingAddress: address,
      items: cart.items.map(i => ({ productId: i.product.id, quantity: i.quantity })),
    };

    const reqId = ++lastReqId.current;
    setQuoting(true);
    try {
      try {
        const res = await api<{
          regular: number; express: number;
          parcel?: { grams:number; weightKg:number;
                     lengthCm?:number|null; widthCm?:number|null; heightCm?:number|null; }
        }>('/shipping/quote/details', { method: 'POST', body: JSON.stringify(body) });

        if (reqId !== lastReqId.current) return;
        if (res) {
          if (typeof res.regular === 'number') setRegFee(res.regular);
          if (typeof res.express === 'number') setExpFee(res.express);
          if (res.parcel) setParcel({
            grams: res.parcel.grams,
            weightKg: res.parcel.weightKg,
            lengthCm: res.parcel.lengthCm ?? undefined,
            widthCm:  res.parcel.widthCm  ?? undefined,
            heightCm: res.parcel.heightCm ?? undefined,
          });
          return;
        }
      } catch {
        /* fallback to legacy */
      }

      const res2 = await api<{ regular: number; express: number }>('/shipping/quote', {
        method: 'POST',
        body:   JSON.stringify(body),
      });
      if (reqId !== lastReqId.current) return;

      if (res2 && typeof res2.regular === 'number') setRegFee(res2.regular);
      if (res2 && typeof res2.express === 'number') setExpFee(res2.express);
      setParcel(null);
    } catch (e) {
      console.error('Shipping quote error:', e);
      setParcel(null);
    } finally {
      if (reqId === lastReqId.current) setQuoting(false);
    }
  }, [step, address, addressIsValid, cart.items]);

  useEffect(() => { fetchQuotes(); }, [fetchQuotes]);

  /* Payment-Intent creation */
  async function buildPaymentIntent() {
    setBusy(true); setErr('');

    if (cart.items.length === 0) {
      setBusy(false);
      setErr('Your cart is empty.');
      setStep(1);
      return;
    }

    const useGuest = isGuest || cart.id === null;
    const url      = useGuest ? '/guest/checkout' : '/checkout';

    const body = useGuest
      ? {
          email      : address.email,
          firstName  : address.fullName.split(' ')[0] ?? '',
          lastName   : address.fullName.split(' ').slice(1).join(' ') || '—',
          shippingAddress: address,
          shippingMethod : method,
          // guests cannot use coupons (server also rejects)
          items: cart.items.map(i => ({ productId: i.product.id, quantity: i.quantity })),
        }
      : { shippingAddress: address, shippingMethod: method, couponCode: couponCode?.trim() || null };

    try {
      const res = await api<{
        order:        OrderDto & { shippingAmount: number };
        clientSecret: string;
      }>(url, { method:'POST', body: JSON.stringify(body) });

      if (!res?.clientSecret) throw new Error('Missing client secret');

      if (typeof res.order.shippingAmount === 'number') {
        if (method === 'REGULAR') setRegFee(Number(res.order.shippingAmount));
        else                      setExpFee(Number(res.order.shippingAmount));
      }

      setCs(res.clientSecret);
      setServerOrder(res.order);
    } catch (e) {
      setErr((e as Error).message);
    } finally { setBusy(false); }
  }

  useEffect(() => {
    if (step === 3 && !clientSecret) buildPaymentIntent();
  }, [step, clientSecret]);                     // eslint-disable-line

  useEffect(() => { setCs(undefined); setServerOrder(null); }, [method, couponCode]);
  useEffect(() => { if (step === 2) { setCs(undefined); setServerOrder(null); } }, [step]);

  useEffect(() => {
    if (step !== 3) return;
    if (stripeReady) return;
    const t = setTimeout(() => {
      setErr(prev => prev || 'Payment form is taking longer than usual to load. Please check your connection or disable ad blockers and try again.');
    }, 8000);
    return () => clearTimeout(t);
  }, [step, stripeReady]);

  /* Clear any prior error once user edits coupon */
  useEffect(() => { setErr(''); }, [couponCode]);

  async function payNow() {
    if (cart.items.length === 0) {
      setErr('Your cart is empty.');
      setStep(1);
      return;
    }
    if (!stripe || !elements || !clientSecret) return;

    setBusy(true); setErr('');
    const { error } =
      await stripe.confirmCardPayment(clientSecret, {
        payment_method: { card: elements.getElement(CardElement)! },
      });
    setBusy(false);

    if (error) { setErr(error.message ?? 'Payment failed'); return; }
    clear();
    nav('/thanks', { replace:true });
  }

  const formatWeight = (p: Parcel) => {
    if (!p) return '';
    const kg = p.weightKg ?? (p.grams ? p.grams / 1000 : 0);
    if (kg < 1) return `${Math.ceil(kg * 1000)} g`;
    return `${kg.toFixed(2)} kg`;
  };

  const curSubtotal = serverOrder ? Number(serverOrder.subtotalAmount) : subtotal;
  const curShipping = serverOrder ? Number(serverOrder.shippingAmount) : shipping;
  const curTax      = serverOrder ? Number(serverOrder.taxAmount)      : tax;
  const curDiscount = serverOrder ? Number(serverOrder.discountAmount) : 0;
  const curTotal    = serverOrder ? Number(serverOrder.totalAmount)    : grand;

  /* Step‑1 "Continue" handler with early coupon validation */
  async function proceedFromStep1() {
    setErr('');

    if (!addressIsValid || cart.items.length === 0) return; // button already disables

    // Guests cannot use coupons; skip validation in that case
    const raw = couponCode.trim().toUpperCase();
    if (!isGuest && raw) {
      setValidatingCoupon(true);
      try {
        const res = await canUseCoupon(raw);

        // Normalize state if user typed lowercase
        if (raw !== couponCode) setCoupon(raw);

        if (!res || res.valid === false) {
          setErr('Invalid coupon code. Please check and try again.');
          return;
        }
        if (res.active === false) {
          setErr('This coupon is not currently active.');
          return;
        }
        if (res.alreadyUsed === true) {
          setErr('This coupon has already been used on your account and cannot be used again.');
          return;
        }
      } catch (e: any) {
        setErr(e?.message || 'Unable to validate coupon right now. Please try again.');
        return;
      } finally {
        setValidatingCoupon(false);
      }
    }

    setStep(2);
  }

  return (
    <Box sx={{ width:'100%', minHeight:'100vh', background:BG, pt:6, pb:8 }}>
      <Container maxWidth="md">
        <Typography variant="h1" align="center" color="common.white" gutterBottom>
          Pro Shop
        </Typography>

        <Stepper activeStep={step - 1} alternativeLabel sx={{ mb:4 }}>
          {['Address','Shipping','Payment','Done'].map(l=>(
            <Step key={l}><StepLabel>{l}</StepLabel></Step>
          ))}
        </Stepper>

        <Paper sx={{ p:3, maxWidth:720, mx:'auto' }}>
          {/* Step 1 - Address and Coupon if authed */}
          {step === 1 && (
            <>
              <Typography variant="h5" gutterBottom>Shipping Address</Typography>
              <ShippingAddressForm value={address} onChange={a=>setAddr(a)} />

              <Divider sx={{ my: 2 }} />

              {!isGuest ? (
                <>
                  <Typography variant="h6" gutterBottom>Have a coupon?</Typography>
                  <TextField
                    fullWidth
                    label="Coupon Code (optional)"
                    value={couponCode}
                    onChange={e => setCoupon(e.target.value.toUpperCase())}
                    inputProps={{ style: { textTransform: 'uppercase' } }}
                    helperText="If valid and active, the discount will be applied to your total."
                  />
                </>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Coupons are available for logged‑in customers. <br/>Create an account or sign in to use a coupon.
                </Typography>
              )}

              {err && <Typography color="error" sx={{ mt:2 }}>{err}</Typography>}

              <Box sx={{ textAlign:'right', mt:3 }}>
                <Button
                  variant="contained"
                  disabled={!addressIsValid || cart.items.length === 0 || validatingCoupon}
                  onClick={proceedFromStep1}
                  startIcon={validatingCoupon ? <CircularProgress size={18} /> : undefined}
                >
                  Continue →
                </Button>
              </Box>
            </>
          )}

          {/* Step 2 - Shipping */}
          {step === 2 && (
            <>
              <Typography variant="h5" gutterBottom>Select Shipping Method</Typography>

              {quoting && (
                <Typography variant="body2" color="text.secondary" sx={{ mb:1 }}>
                  Getting live Canada Post rates…
                </Typography>
              )}

              <RadioGroup value={method} onChange={e=>setMethod(e.target.value as Method)}>
                <FormControlLabel
                  value="REGULAR"
                  control={<Radio />}
                  label={
                    <Stack direction="row" spacing={2} alignItems="center">
                      <Avatar sx={{ bgcolor:'transparent', width:24, height:24 }}>
                        <LocalShippingIcon fontSize="small"/>
                      </Avatar>
                      <Typography>
                        Regular (5-7 days) - ${regularFee.toFixed(2)}
                      </Typography>
                    </Stack>
                  }
                />
                <FormControlLabel
                  value="EXPRESS"
                  control={<Radio />}
                  label={
                    <Stack direction="row" spacing={2} alignItems="center">
                      <Avatar sx={{ bgcolor:'transparent', width:24, height:24 }}>
                        <FlashOnIcon fontSize="small"/>
                      </Avatar>
                      <Typography>
                        Expedited (1-2 days) - ${expressFee.toFixed(2)}
                      </Typography>
                    </Stack>
                  }
                />
              </RadioGroup>

              {parcel && (
                <Typography variant="body2" color="text.secondary" sx={{ mt:1 }}>
                  Estimated package:&nbsp;
                  {formatWeight(parcel)}
                  {parcel.lengthCm && parcel.widthCm && parcel.heightCm
                    ? `, ${parcel.lengthCm} × ${parcel.widthCm} × ${parcel.heightCm} cm`
                    : ''}
                </Typography>
              )}

              <Divider sx={{ my:3 }} />

              <Typography variant="subtitle1" gutterBottom>Order Summary</Typography>
              <Typography>Subtotal: ${subtotal.toFixed(2)}</Typography>
              <Typography>Shipping: ${shipping.toFixed(2)}</Typography>
              <Typography>Tax (13 %): ${tax.toFixed(2)}</Typography>
              {!isGuest && couponCode.trim() && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  Coupon <b>{couponCode.trim().toUpperCase()}</b> will be validated and applied at the next step.
                </Typography>
              )}
              <Typography variant="h6" sx={{ mt:1 }}>
                Total: ${grand.toFixed(2)}
              </Typography>

              {err && <Typography color="error" sx={{ mt:2 }}>{err}</Typography>}

              <Box sx={{ display:'flex', justifyContent:'space-between', mt:3 }}>
                <Button onClick={()=>setStep(1)} startIcon={<ArrowBackIcon />}>Back</Button>
                <Button
                  variant="contained"
                  onClick={()=>setStep(3)}
                  disabled={cart.items.length === 0}
                >
                  Continue →
                </Button>
              </Box>
            </>
          )}

          {/* Step 3 Payment */}
          {step === 3 && (
            <>
              <Typography variant="h5" gutterBottom>Payment</Typography>

              {busy && !clientSecret && (
                <Box sx={{ textAlign:'center', py:4 }}>
                  <CircularProgress />
                  <Typography sx={{ mt:2 }}>Preparing your payment…</Typography>
                </Box>
              )}

              {!busy && clientSecret && !stripeReady && (
                <Box sx={{ textAlign:'center', py:4 }}>
                  <CircularProgress />
                  <Typography sx={{ mt:2 }}>Loading secure payment form…</Typography>
                  {err && <Typography color="error" sx={{ mt:2 }}>{err}</Typography>}
                </Box>
              )}

              {!busy && clientSecret && stripeReady && (
                <>
                  <Box sx={{ p:2, border:'1px solid #ddd', borderRadius:1, mb:3 }}>
                    <CardElement options={{ hidePostalCode:true }} />
                  </Box>

                  <Divider sx={{ mb:3 }} />
                  <Typography variant="subtitle1" gutterBottom>Order Summary</Typography>
                  <Typography>Subtotal: ${curSubtotal.toFixed(2)}</Typography>
                  <Typography>Shipping: ${curShipping.toFixed(2)}</Typography>
                  <Typography>Tax: ${curTax.toFixed(2)}</Typography>
                  {serverOrder && Number(curDiscount) > 0 && (
                    <>
                      {serverOrder.couponCode && (
                        <Typography variant="body2">Coupon: {serverOrder.couponCode}</Typography>
                      )}
                      <Typography>Discount: -${curDiscount.toFixed(2)}</Typography>
                    </>
                  )}
                  <Typography variant="h6" sx={{ mt:1 }}>
                    Total: ${curTotal.toFixed(2)}</Typography>

                  {err && <Typography color="error" sx={{ mt:2 }}>{err}</Typography>}

                  <Box sx={{ display:'flex', justifyContent:'space-between', mt:3 }}>
                    <Button
                      onClick={()=>setStep(2)}
                      startIcon={<ArrowBackIcon />}
                      disabled={busy}
                    >
                      Back
                    </Button>
                    <Button
                      variant="contained"
                      onClick={payNow}
                      disabled={busy || !stripeReady || cart.items.length === 0}
                      startIcon={busy ? <CircularProgress size={18}/> : undefined}
                    >
                      Pay ${curTotal.toFixed(2)}
                    </Button>
                  </Box>
                </>
              )}
            </>
          )}
        </Paper>
      </Container>
    </Box>
  );
}
