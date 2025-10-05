import { api } from './api';

/* shared DTOs */
export interface CartItem {
  id:         number;
  quantity:   number;
  unitPrice:  number;
  product: {
    id:            number;
    name:          string;
    brand?:        string | null;
    price:         number;
    images:        { url: string; isPrimary: boolean; altText?: string | null }[];
    categoryName?: string | null;
  };
}

export interface Cart {
  id:               number;
  subtotal:         number;
  totalItemCount:   number;
  items:            CartItem[];
}

/* CRUD helpers that hit our Spring endpoints */

export async function fetchCart(): Promise<Cart> {
  return api<Cart>('/api/cart') as Promise<Cart>;
}

export async function addToCart(productId: number, qty = 1): Promise<Cart> {
  return api<Cart>('/api/cart/items', {
    method: 'POST',
    body:   JSON.stringify({ productId, quantity: qty }),
  }) as Promise<Cart>;
}

export async function updateCartItem(cartItemId: number, qty: number): Promise<Cart> {
  return api<Cart>('/api/cart/items', {
    method: 'PATCH',
    body:   JSON.stringify({ cartItemId, quantity: qty }),
  }) as Promise<Cart>;
}

export async function removeCartItem(cartItemId: number): Promise<Cart> {
  return api<Cart>('/api/cart/items', {
    method: 'DELETE',
    body:   JSON.stringify({ cartItemId }),
  }) as Promise<Cart>;
}
