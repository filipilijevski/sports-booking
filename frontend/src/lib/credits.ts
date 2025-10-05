/**
 * Table rental credits (client + admin)
 * - Self-contained API surface for table-hours credits.
 * - Backward compatible with previous shapes (combined vs. breakdown).
 */

import { api } from './api';

/* Types */

export interface TableCreditSummary {
  /** Combined remaining hours across purchases and membership entitlements. */
  hoursRemaining: number;

  /** Optional breakdown (if the server provides it). */
  purchasedHours?: number | null;
  membershipHours?: number | null;
}

export interface AdminCreditUserRow {
  id: number;
  name: string | null;
  email: string;
  /** Combined hours (purchases + entitlements) remaining for the user. */
  tableHoursRemaining: number;
  /** Optional last usage timestamp if the backend provides it. */
  lastUsedAt?: string | null;
}

export interface Paged<T> {
  content: T[];
  page: number;          // zero-based
  size: number;
  totalElements: number;
  totalPages: number;
}

/* Client: my credit summary */

export const fetchMyTableCreditSummary = async (): Promise<TableCreditSummary> => {
  const res = await api<any>('/my/table-credits/summary').catch(() => undefined);
  if (!res) return { hoursRemaining: 0 };

  const hoursRemaining =
    typeof res.hoursRemaining === 'number'
      ? res.hoursRemaining
      : Number(res.totalHours ?? res.remaining ?? 0);

  const purchased =
    typeof res.purchasedHours === 'number'
      ? res.purchasedHours
      : (typeof res.purchases === 'number' ? res.purchases : undefined);

  const membership =
    typeof res.membershipHours === 'number'
      ? res.membershipHours
      : (typeof res.entitlements === 'number' ? res.entitlements : undefined);

  return {
    hoursRemaining,
    purchasedHours: typeof purchased === 'number' ? purchased : null,
    membershipHours: typeof membership === 'number' ? membership : null,
  };
};

/* Admin: search/consume/manual grant */

export const adminSearchUsersWithCredits = (q = '', page = 0, size = 10) => {
  const params = new URLSearchParams();
  if (q && q.trim()) params.set('q', q.trim());
  params.set('page', String(Math.max(0, page)));
  params.set('size', String(Math.min(Math.max(1, size), 100)));
  return api<Paged<AdminCreditUserRow>>(`/admin/table-credits/users?${params.toString()}`);
};

export const adminConsumeTableHours = (payload: { userId: number; hours: number }) =>
  api<{ ok: boolean; remaining: number }>('/admin/table-credits/consume', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const adminManualGrantTableHours = (payload: {
  userId: number;
  hours: number;              // positive number (0.5 increments)
  paymentRef?: string | null; // cash/etransfer ref
  paidAt?: string | null;     // ISO; default server now
  notes?: string | null;
}) =>
  api<{ ok: boolean; creditId?: number }>('/admin/table-credits/manual-grant', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
