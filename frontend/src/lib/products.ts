import { api } from './api';
import { getAccessToken } from './auth';           // JWT for direct upload

/*  DTOs coming from backend */
export interface Product {
  id:            number;
  sku:           string;
  name:          string;
  brand?:        string | null;
  price:         number;
  description?:  string | null;

  /** inventory management */
  inventoryQty?: number;

  /** weight in grams (always present in DB) */
  grams?:        number;

  /* category shortcuts for UI filters */
  categoryId?:   number | null;
  categoryName?: string | null;

  images: {
    url:        string;
    altText?:   string | null;
    isPrimary:  boolean;
  }[];
}

/* Public Read */
export async function fetchProducts(
  q        = '',
  category?: number,
  page      = 0,
  size      = 20,
) {
  const p = new URLSearchParams();
  if (q)        p.set('q', q);
  if (category) p.set('category', String(category));
  p.set('page',  String(page));
  p.set('size',  String(size));

  return api<{
    content:        Product[];
    totalPages:     number;
    totalElements:  number;
  }>(`/products?${p.toString()}`);
}

export async function fetchProduct(id: number): Promise<Product> {
  const res = await api<Product>(`/products/${id}`);
  if (!res) throw new Error(`Product ${id} not found`);
  return res;
}

/* Admin Owner CRUD */
export const createProduct = (p: Partial<Product>) =>
  api<Product>('/products',      { method: 'POST', body: JSON.stringify(p) });

export const updateProduct  = (id: number, p: Partial<Product>) =>
  api<Product>(`/products/${id}`,{ method: 'PUT',  body: JSON.stringify(p) });

export const deleteProduct  = (id: number) =>
  api<void>(`/products/${id}`,   { method: 'DELETE' });

/* Image upload */
export async function uploadProductImage(
  productId: number,
  file: File,
  primary = false,
  altText?: string,
): Promise<Product> {

  const fd = new FormData();
  fd.append('file', file);
  fd.append('primary', String(primary));
  if (altText !== undefined) fd.append('altText', altText);

  /* add JWT if present */
  const headers: Record<string, string> = {};
  const jwt = getAccessToken();
  if (jwt) headers['Authorization'] = `Bearer ${jwt}`;

  const res = await fetch(`/api/products/${productId}/images`, {
    method: 'POST',
    body  : fd,
    headers,
  });
  if (!res.ok) throw new Error(await res.text());

  return res.json() as Promise<Product>;
}
