import { api } from './api';

export type CouponType = 'PERCENT' | 'AMOUNT';

export interface Coupon {
  id: number;
  code: string;
  type: CouponType;           // 'PERCENT' or 'AMOUNT'
  value: number;              // percent (0..100) or absolute amount (CAD)
  active: boolean;
  startAt?: string | null;    // ISO
  endAt?: string | null;      // ISO
  minSpend?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

/** Map server to UI (derives type/value from percentOff/amountOff) */
function normalize(dto: any): Coupon {
  const startsAt = dto.startsAt ?? dto.startAt ?? dto.startDate ?? dto.starts ?? null;
  const expiresAt = dto.expiresAt ?? dto.endAt ?? dto.endDate ?? dto.expiryDate ?? dto.expiresAt ?? null;

  let type: CouponType = 'AMOUNT';
  let value = 0;

  if (dto.percentOff != null) {
    type = 'PERCENT';
    const pct = Number(dto.percentOff);
    value = isFinite(pct) ? Math.round(pct * 100) : 0;
  } else {
    type = 'AMOUNT';
    const amt = Number(dto.amountOff ?? dto.amount ?? 0);
    value = isFinite(amt) ? amt : 0;
  }

  return {
    id: Number(dto.id),
    code: String(dto.code ?? dto.couponCode ?? ''),
    type,
    value,
    active: Boolean(dto.active ?? dto.enabled ?? false),
    startAt: startsAt,
    endAt: expiresAt,
    minSpend: dto.minSpend != null ? Number(dto.minSpend) : null,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
  };
}

/** Map UI to server `CouponDto` */
function toServerPayload(c: Partial<Coupon>) {
  const isPercent = c.type === 'PERCENT';
  const pct =
    isPercent && typeof c.value === 'number'
      ? Number((c.value / 100).toFixed(4))
      : null;

  const amt =
    !isPercent && typeof c.value === 'number'
      ? Number(c.value.toFixed(2))
      : null;

  return {
    id: c.id,
    code: c.code?.trim().toUpperCase(),
    percentOff: pct,
    amountOff: amt,
    minSpend: c.minSpend ?? null,
    startsAt: c.startAt ?? null,
    expiresAt: c.endAt ?? null,
    active: Boolean(c.active),
  };
}

export async function fetchCoupons(): Promise<Coupon[]> {
  const res = await api<any[]>('/admin/coupons');
  return (Array.isArray(res) ? res : []).map(normalize);
}

export async function getCoupon(id: number): Promise<Coupon> {
  const res = await api<any>(`/admin/coupons/${id}`);
  if (!res) throw new Error('Coupon not found');
  return normalize(res);
}

export async function createCoupon(payload: Partial<Coupon>): Promise<Coupon> {
  const res = await api<any>('/admin/coupons', {
    method: 'POST',
    body: JSON.stringify(toServerPayload(payload)),
  });
  return normalize(res);
}

export async function updateCoupon(id: number, payload: Partial<Coupon>): Promise<Coupon> {
  const res = await api<any>(`/admin/coupons/${id}`, {
    method: 'PUT',
    body: JSON.stringify(toServerPayload(payload)),
  });
  return normalize(res);
}

export async function deleteCoupon(id: number): Promise<void> {
  await api<void>(`/admin/coupons/${id}`, { method: 'DELETE' });
}

/** UI helper */
export function formatCouponValue(c: Coupon): string {
  return c.type === 'PERCENT'
    ? `${(c.value ?? 0).toFixed(0)}% off`
    : `$${(c.value ?? 0).toFixed(2)} off`;
}

export function isEffectivelyActive(c: Coupon, now = new Date()): boolean {
  const start = c.startAt ? new Date(c.startAt) : null;
  const end   = c.endAt   ? new Date(c.endAt)   : null;
  if (!c.active) return false;
  if (start && now < start) return false;
  if (end && now > end) return false;
  return true;
}

/* Early validator (logged-in end-users)
 * GET /api/coupons/can-use?code=CODE  (requires auth)
 * Returns: { valid, alreadyUsed, active, ... } */
export async function canUseCoupon(code: string): Promise<{
  code: string;
  valid: boolean;
  alreadyUsed: boolean;
  active: boolean;
  percentOff?: number | null;
  amountOff?: number | null;
  minSpend?: number | null;
  startsAt?: string | null;
  expiresAt?: string | null;
}> {
  const usp = new URLSearchParams({ code: code.trim().toUpperCase() });
  const res = await api<any>(`/coupons/can-use?${usp.toString()}`);
  return res as any;
}

/* Admin validator (validate for a selected user id)
 * GET /api/admin/coupons/can-use?code=CODE&userId=123  */
export async function adminCanUseCoupon(code: string, userId: number): Promise<{
  code: string;
  valid: boolean;
  alreadyUsed: boolean;
  active: boolean;
  percentOff?: number | null;
  amountOff?: number | null;
  minSpend?: number | null;
  startsAt?: string | null;
  expiresAt?: string | null;
}> {
  const usp = new URLSearchParams({
    code: code.trim().toUpperCase(),
    userId: String(userId),
  });
  const res = await api<any>(`/admin/coupons/can-use?${usp.toString()}`);
  return res as any;
}
