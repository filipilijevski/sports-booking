import { api } from './api';

export type ProductAuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'STOCK' | 'IMAGE';

export interface ProductAudit {
  id: number;
  action: ProductAuditAction;
  productId: number | null;
  sku?: string | null;
  name?: string | null;
  price?: number | null;
  inventoryQty?: number | null;
  brand?: string | null;
  grams?: number | null;
  categoryId?: number | null;
  categoryName?: string | null;
  imageModified?: boolean;
  actorUserId?: number | null;
  detailsJson?: string | null;  // when present on UPDATE
  createdAt: string;            // ISO
}

/** GET /api/admin/products/audit?limit= */
export async function fetchProductAudits(limit = 200): Promise<ProductAudit[]> {
  const usp = new URLSearchParams({ limit: String(Math.max(1, Math.min(1000, limit))) });
  const res = await api<ProductAudit[]>(`/admin/products/audit?${usp.toString()}`);
  return Array.isArray(res) ? res : [];
}
