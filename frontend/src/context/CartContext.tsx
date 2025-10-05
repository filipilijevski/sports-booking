import {
  createContext, useReducer, useContext, useEffect,
  type ReactNode,
} from 'react';

import {
  fetchCart, addToCart, updateCartItem, removeCartItem,
  type Cart as ApiCart,
} from '../lib/cart';
import { fetchProduct, type Product } from '../lib/products';
import { fetchAvailability } from '../lib/stock';
import { useRole } from './RoleContext';

/* Types */
export interface CartState {
  id:         number | null;        // null for guest
  items:      ApiCart['items'];
  subtotal:   number;
  totalCount: number;
  drawerOpen: boolean;
}

type Action =
  | { type: 'LOAD';   payload: CartState }
  | { type: 'TOGGLE'; payload: boolean }
  | { type: 'CLEAR' };

function reducer(state: CartState, action: Action): CartState {
  switch (action.type) {
    case 'LOAD':
      // Preserve current drawer state so it doesn't close on every refresh
      return { ...state, ...action.payload, drawerOpen: state.drawerOpen };
    case 'TOGGLE':
      return { ...state, drawerOpen: action.payload };
    case 'CLEAR':
      return { id: null, items: [], subtotal: 0, totalCount: 0, drawerOpen: false };
    default:
      return state;
  }
}

interface Ctx {
  state: CartState;
  totalCount: number;
  /** Optional product is used to seed guest-cart items with price & meta-data */
  add:    (pid: number, qty?: number, product?: Product) => Promise<void>;
  update: (cid: number, qty: number)   => Promise<void>;
  remove: (cid: number)                => Promise<void>;
  clear:  ()                           => void;
  openDrawer:  ()                      => void;
  closeDrawer: ()                      => void;
}

const CartCtx = createContext<Ctx>({} as Ctx);

/* persistent guest cart to localStorage */
const GUEST_KEY = 'ttclub_guest_cart';

export function CartProvider({ children }: { children: ReactNode }) {
  const { role } = useRole();                  // refreshes whenever JWT changes

  const [state, dispatch] = useReducer(reducer, {
    id: null, items: [], subtotal: 0, totalCount: 0, drawerOpen: false,
  });

  /* helpers */
  const saveGuest = (s: CartState) => localStorage.setItem(GUEST_KEY, JSON.stringify(s));
  const loadGuest = (): CartState | null => {
    try { return JSON.parse(localStorage.getItem(GUEST_KEY) ?? 'null'); }
    catch { return null; }
  };

  const computeTotals = (items: ApiCart['items']) => {
    const subtotal   = items.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0);
    const totalCount = items.reduce((sum, i) => sum + i.quantity, 0);
    return { subtotal, totalCount };
  };

  const clampToAvailable = async (pid: number, desired: number, fallbackMax?: number) => {
    try {
      const map = await fetchAvailability([pid]);
      const max = map[pid];
      // if backend returned a number, honor it; else fall back to provided limit
      const limit = Number.isFinite(max) ? max : (fallbackMax ?? Infinity);
      return Math.max(0, Math.min(desired, limit));
    } catch {
      // network issue then be conservative and use fallback if any
      return Math.max(0, Math.min(desired, fallbackMax ?? desired));
    }
  };

  /* initial load */
  useEffect(() => {
    (async () => {
      if (role === 'GUEST') {
        const persisted = loadGuest();
        if (persisted) dispatch({ type: 'LOAD', payload: persisted });
      } else {
        const api = await fetchCart();
        dispatch({ type: 'LOAD', payload: adapt(api) });
        /* merge guest cart (if any) once after first login */
        const guest = loadGuest();
        if (guest && guest.items.length) {
          await Promise.all(
            guest.items.map(i => addToCart(i.product.id, i.quantity)),
          );
          localStorage.removeItem(GUEST_KEY);
          const fresh = await fetchCart();
          dispatch({ type: 'LOAD', payload: adapt(fresh) });
        }
      }
    })();
  }, [role]);                    // re-run on every “login / logout”

  /* shared ops */
  const refresh = async () => {
    if (role === 'GUEST') {
      const g = loadGuest() ?? state;
      dispatch({ type: 'LOAD', payload: g });
    } else {
      const api = await fetchCart();
      dispatch({ type: 'LOAD', payload: adapt(api) });
    }
  };

  /* Add (guest branch rewritten so items have price & meta-data) 
    and now limits to server-reported AVAILABLE stock */
  const add = async (pid: number, qty = 1, product?: Product) => {
    if (role === 'GUEST') {
      const curr = loadGuest() ?? state;

      // Get a conservative fallback (legacy) from product data if present
      const legacyMax = product?.inventoryQty ?? Infinity;

      // Determine the allowed increment given current quantity
      const existing = curr.items.find(i => i.product.id === pid);
      const currentQty = existing?.quantity ?? 0;

      // Ask backend how many are truly available (inventory - pending)
      const targetTotal = await clampToAvailable(pid, currentQty + qty, legacyMax);
      const allowedInc  = Math.max(0, targetTotal - currentQty);

      if (existing) {
        existing.quantity = currentQty + allowedInc;
      } else {
        // ensure we know price and name for UI totals
        let full = product;
        if (!full) {
          try { full = await fetchProduct(pid); } catch { /* keep minimal fallback */ }
        }

        curr.items.push({
          id: Date.now(), // stub id (unique per session)
          quantity:  allowedInc, // already limited
          unitPrice: full?.price ?? 0,
          product:   full ? (full as any) : ({ id: pid } as any),
        });
      }

      // Remove zero-qty rows (in case nothing was allowed)
      const items = curr.items.filter(i => i.quantity > 0);
      const next  = { ...curr, items, ...computeTotals(items) };
      saveGuest(next);
      dispatch({ type: 'LOAD', payload: next });
    } else {
      try {
        await addToCart(pid, qty);
        await refresh();
      } catch (e) {
        /* surface error so UI can react */
        throw e;
      }
    }
  };

  const update = async (cid: number, qty: number) => {
    if (role === 'GUEST') {
      const curr = loadGuest() ?? state;
      const it   = curr.items.find(i => i.id === cid);
      if (!it) return;

      const pid = it.product.id;
      const legacyMax = Infinity; // no local cap - rely on server availability
      const clamped = await clampToAvailable(pid, qty, legacyMax);

      if (clamped <= 0) {
        // remove item if no units available
        const items = curr.items.filter(i => i.id !== cid);
        const next  = { ...curr, items, ...computeTotals(items) };
        saveGuest(next);
        dispatch({ type: 'LOAD', payload: next });
      } else {
        it.quantity = clamped;
        const next = { ...curr, ...computeTotals(curr.items) };
        saveGuest(next);
        dispatch({ type: 'LOAD', payload: next });
      }
    } else {
      await updateCartItem(cid, qty);
      await refresh();
    }
  };

  const remove = async (cid: number) => {
    if (role === 'GUEST') {
      const curr  = loadGuest() ?? state;
      const items = curr.items.filter(i => i.id !== cid);
      const next  = { ...curr, items, ...computeTotals(items) };
      saveGuest(next);
      dispatch({ type: 'LOAD', payload: next });
    } else {
      await removeCartItem(cid);
      await refresh();
    }
  };

  const clear = () => {
    if (role === 'GUEST') localStorage.removeItem(GUEST_KEY);
    dispatch({ type: 'CLEAR' });
  };

  const openDrawer  = () => dispatch({ type: 'TOGGLE', payload: true });
  const closeDrawer = () => dispatch({ type: 'TOGGLE', payload: false });

  return (
    <CartCtx.Provider value={{
      state,
      totalCount: state.totalCount,
      add,
      update,
      remove,
      clear,
      openDrawer,
      closeDrawer,
    }}>
      {children}
    </CartCtx.Provider>
  );
}

export function useCart() { return useContext(CartCtx); }

/* helper */
function adapt(api: ApiCart): CartState {
  return {
    id:         api.id,
    items:      api.items,
    subtotal:   api.subtotal,
    totalCount: api.totalItemCount,
    drawerOpen: false, // reducer preserves existing open/close value
  };
}
