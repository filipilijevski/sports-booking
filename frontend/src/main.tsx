import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Elements }   from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';

import App from './App.tsx';
import './index.css';

const pubKey = import.meta.env.VITE_STRIPE_PUB_KEY;
if (!pubKey) console.warn('VITE_STRIPE_PUB_KEY missing - Stripe disabled');
const stripePromise = pubKey ? loadStripe(pubKey) : Promise.resolve(null);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Elements stripe={stripePromise}>
      <App />
    </Elements>
  </StrictMode>,
);
