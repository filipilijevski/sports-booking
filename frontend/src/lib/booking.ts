/**
 * Booking API client
 * ------------------
 * SAFE defaults:
 *  - Uses central api() wrapper (adds Bearer, refreshes on 401).
 *  - All new helpers catch & propagate errors to callers unless noted.
 */

import { api, apiPublic } from './api';

/* Types aligned with backend DTOs */

export type DayOfWeek =
  'MONDAY'|'TUESDAY'|'WEDNESDAY'|'THURSDAY'|'FRIDAY'|'SATURDAY'|'SUNDAY';

export type EnrollmentMode = 'OPEN' | 'ADMIN_ONLY';

export interface ProgramPackageDto {
  id: number;
  programId: number;
  name: string;
  sessionsCount: number;
  priceCad: number;
  active: boolean;
  sortOrder?: number | null;
}

export interface ProgramSlotDto {
  id: number;
  programId: number;
  weekday: DayOfWeek;
  startTime: string; // "HH:mm:ss" from LocalTime
  endTime: string;   // "HH:mm:ss"
  coachId: number;
  coachName?: string | null;
}

export interface ProgramDto {
  id: number;
  title: string;
  description?: string | null;
  active: boolean;
  packages: ProgramPackageDto[];
  slots: ProgramSlotDto[];
  /** Admin-facing enrollment gating. Missing then treated as OPEN for bwc. */
  enrollmentMode?: EnrollmentMode;
}

/* public card */
export interface ProgramCardDto {
  id: number;
  title: string;
  description?: string | null;
  active: boolean;
  packages: ProgramPackageDto[];
  weekly: { weekday: DayOfWeek; times: { start: string; end: string; coach?: string | null }[] }[];
  coaches: string[];
  /**
   * Enrollment gating (OPEN | ADMIN_ONLY). Missing then treat as OPEN for bwc.
   * Backend uses enum Program.EnrollmentMode { OPEN, ADMIN_ONLY }.
   */
  enrollmentMode?: EnrollmentMode | string;
}

/* occurrences feed dto (from ProgramOccurrenceService -> ProgramOccurrenceDto) */
export interface ProgramOccurrenceDto {
  programId: number | null;
  title: string | null;
  start: string;              // Instant (ISO string)
  end: string;                // Instant (ISO string)
  coachName?: string | null;
}

/* membership plan */
export type PlanType = 'INITIAL' | 'SPECIAL';
export type EntitlementKind = 'TABLE_HOURS' | 'PROGRAM_CREDITS' | 'TOURNAMENT_ENTRIES';

export interface MembershipEntitlementDto {
  id: number;
  planId: number;
  kind: EntitlementKind;
  amount: number;
}

export interface MembershipPlanDto {
  id: number;
  type: PlanType;
  holderKind?: 'INDIVIDUAL' | 'GROUP' | string; // server sends this
  name: string;
  priceCad: number;
  description?: string | null;       // optional
  durationDays?: number | null;
  active: boolean;
  entitlements: MembershipEntitlementDto[];
}

/* Admin Programs CRUD */

export const adminListPrograms = () =>
  api<ProgramDto[]>('/admin/programs');

export const adminCreateProgram = (payload: {
  title: string;
  description?: string;
  active?: boolean;
  enrollmentMode?: EnrollmentMode; // optional
}) =>
  api<ProgramDto>('/admin/programs', { method:'POST', body: JSON.stringify(payload) });

export const adminUpdateProgram = (id: number, payload: Partial<{
  title: string;
  description: string;
  active: boolean;
  enrollmentMode: EnrollmentMode; // optional
}>) =>
  api<ProgramDto>(`/admin/programs/${id}`, { method:'PUT', body: JSON.stringify(payload) });

export const adminDeleteProgram = (id: number) =>
  api<void>(`/admin/programs/${id}`, { method:'DELETE' });

export const adminAddPackage = (programId: number, payload: { name: string; sessionsCount: number; priceCad: number; active?: boolean; sortOrder?: number }) =>
  api<ProgramPackageDto>(`/admin/programs/${programId}/packages`, { method:'POST', body: JSON.stringify(payload) });

export const adminUpdatePackage = (packageId: number, payload: Partial<{ name: string; sessionsCount: number; priceCad: number; active: boolean; sortOrder: number }>) =>
  api<ProgramPackageDto>(`/admin/programs/packages/${packageId}`, { method:'PUT', body: JSON.stringify(payload) });

export const adminDeletePackage = (packageId: number) =>
  api<void>(`/admin/programs/packages/${packageId}`);

export const adminAddSlot = (programId: number, payload: { weekday: DayOfWeek; startTime: string; endTime: string; coachId: number }) =>
  api<ProgramSlotDto>(`/admin/programs/${programId}/slots`, { method:'POST', body: JSON.stringify(payload) });

export const adminUpdateSlot = (slotId: number, payload: Partial<{ weekday: DayOfWeek; startTime: string; endTime: string; coachId: number }>) =>
  api<ProgramSlotDto>(`/admin/programs/slots/${slotId}`, { method:'PUT', body: JSON.stringify(payload) });

export const adminDeleteSlot = (slotId: number) =>
  api<void>(`/admin/programs/slots/${slotId}`);

export const adminListCoaches = () =>
  api<{ id:number; name:string; email:string }[]>('/admin/booking/coaches');

/* Public */

export const fetchPublicPrograms = () =>
  apiPublic<ProgramCardDto[]>('/programs');

export const fetchPublicMembershipPlans = () =>
  apiPublic<MembershipPlanDto[]>('/memberships/plans');

/* public occurrences feed */
export const fetchPublicOccurrences = (from?: string, to?: string) => {
  const params = new URLSearchParams();
  if (from) params.set('from', from);
  if (to)   params.set('to', to);
  const qs = params.toString();
  return apiPublic<ProgramOccurrenceDto[]>(`/programs/occurrences${qs ? `?${qs}` : ''}`);
};

/* Membership Admin */

export const adminListPlans = () =>
  api<MembershipPlanDto[]>('/admin/memberships/plans');

export const adminCreatePlan = (payload: {
  type: 'INITIAL' | 'SPECIAL';
  name: string;
  priceCad: number;
  durationDays: number;
  active?: boolean;
  description?: string | null;
  holderKind?: 'INDIVIDUAL' | 'GROUP';
}) =>
  api<MembershipPlanDto>('/admin/memberships/plans', { method:'POST', body: JSON.stringify(payload) });

export const adminUpdatePlan = (id: number, payload: Partial<{
  type: 'INITIAL' | 'SPECIAL';
  name: string;
  priceCad: number;
  durationDays: number;
  active: boolean;
  description: string | null;
  holderKind: 'INDIVIDUAL' | 'GROUP';
}>) =>
  api<MembershipPlanDto>(`/admin/memberships/plans/${id}`, { method:'PUT', body: JSON.stringify(payload) });

export const adminDeletePlan = (id: number) =>
  api<void>(`/admin/memberships/plans/${id}`);

export const adminAddEntitlement = (planId: number, payload: { kind: EntitlementKind; amount: number }) =>
  api<MembershipEntitlementDto>(`/admin/memberships/plans/${planId}/entitlements`, { method:'POST', body: JSON.stringify(payload) });

export const adminUpdateEntitlement = (entId: number, payload: Partial<{ kind: EntitlementKind; amount: number }>) =>
  api<MembershipEntitlementDto>(`/admin/memberships/entitlements/${entId}`, { method:'PUT', body: JSON.stringify(payload) });

export const adminDeleteEntitlement = (entId: number) =>
  api<void>(`/admin/memberships/entitlements/${entId}`);

/* Admin Groups Read */

export interface EntitlementsSummary {
  tableHoursRemaining?: number | null;
  programCreditsRemaining?: number | null;
  tournamentEntriesRemaining?: number | null;
}

export interface GroupListItem {
  id: number;
  planId: number;
  planName: string;
  planType: string;          // e.g., "INITIAL" | "SPECIAL"
  holderKind: string;        // "INDIVIDUAL" | "GROUP"
  ownerId: number;
  ownerName: string;
  ownerEmail: string;
  startTs: string;           // ISO
  endTs: string;             // ISO
  active: boolean;
  membersCount: number;
  entitlements?: EntitlementsSummary;
}

export interface GroupMemberRow {
  userMembershipId: number;
  userId: number;
  name: string;
  email: string;
  active: boolean;
}

export interface GroupDetail extends GroupListItem {
  members: GroupMemberRow[];
}

/** Optional audit event (consumption/history). Fail-open if API not present. */
export interface GroupAuditEvent {
  id?: number;
  ts: string; // ISO instant
  kind: EntitlementKind;
  /** Positive for credits granted, negative for consumption. */
  delta: number;
  remaining?: number | null;
  actorName?: string | null;
  memberName?: string | null;
  note?: string | null;
}

export const adminListGroups = () =>
  api<GroupListItem[]>('/admin/memberships/groups');

export const adminGetGroup = (id: number) =>
  api<GroupDetail>(`/admin/memberships/groups/${id}`);

/** May not exist on backend yet; callers should handle errors gracefully. */
export const adminGetGroupAudit = (id: number) =>
  api<GroupAuditEvent[]>(`/admin/memberships/groups/${id}/audit`);

/* Client-facing (My) */

export interface MyMembershipsPayload {
  memberships?: Array<{
    userMembershipId?: number;
    planId?: number;
    planName?: string;
    planType?: string;
    active?: boolean;
    startTs?: string;
    endTs?: string;
    groupId?: number | null;
  }>;
  enrollments?: Array<{
    enrollmentId?: number;
    programId?: number;
    programTitle?: string;
    status?: 'ACTIVE' | 'EXHAUSTED' | 'CANCELLED' | string;
    sessionsPurchased?: number | null;
    sessionsRemaining?: number | null;
    lastAttendedAt?: string | null;
    startTs?: string | null;
    endTs?: string | null;
    packageName?: string | null;
  }>;
}

/**
 * Load memberships and, if available, the user's program enrollments.
 * Swallows 401/404/405 on /my/enrollments so older servers keep working.
 */
export const fetchMyMemberships = async (): Promise<MyMembershipsPayload | undefined> => {
  const [rawMemberships, rawEnrollments] = await Promise.all([
    api<any>('/my/memberships').catch(() => undefined),
    api<any>('/my/enrollments').catch(() => undefined),
  ]);

  // memberships normalization
  let memberships: MyMembershipsPayload['memberships'] | undefined;
  if (rawMemberships) {
    if (Array.isArray(rawMemberships.items)) {
      memberships = rawMemberships.items.map((m: any) => ({
        userMembershipId: m.userMembershipId,
        planId:           m.planId,
        planName:         m.planName,
        planType:         m.planType,
        active:           m.active,
        startTs:          m.startTs,
        endTs:            m.endTs,
        groupId:          m.groupId ?? null,
      }));
    } else if (Array.isArray(rawMemberships.memberships)) {
      memberships = rawMemberships.memberships as MyMembershipsPayload['memberships'];
    }
  }

  // enrollments normalization
  let enrollments: MyMembershipsPayload['enrollments'] | undefined;
  if (rawEnrollments) {
    const arr = Array.isArray(rawEnrollments)
      ? rawEnrollments
      : (Array.isArray(rawEnrollments.items) ? rawEnrollments.items : undefined);

    if (arr) {
      enrollments = arr.map((e: any) => ({
        enrollmentId:      e.enrollmentId ?? e.id ?? e.enrollment_id,
        programId:         e.programId    ?? e.program_id,
        programTitle:      e.programTitle ?? e.program_title,
        status:            e.status,
        sessionsPurchased: e.sessionsPurchased ?? e.sessions_purchased,
        sessionsRemaining: e.sessionsRemaining ?? e.sessions_remaining,
        lastAttendedAt:    e.lastAttendedAt    ?? e.last_attended_at ?? null,
        startTs:           e.startTs           ?? e.start_ts ?? null,
        endTs:             e.endTs             ?? e.end_ts   ?? null,
        packageName:       e.packageName       ?? e.package_name ?? null,
      }));
    } else if (Array.isArray((rawEnrollments as any).enrollments)) {
      enrollments = (rawEnrollments as any).enrollments as MyMembershipsPayload['enrollments'];
    }
  }

  // Unified shape fallback: if memberships payload already had enrollments
  if (!enrollments && rawMemberships && Array.isArray(rawMemberships.enrollments)) {
    enrollments = rawMemberships.enrollments as MyMembershipsPayload['enrollments'];
  }

  if (!memberships && !enrollments) return undefined;
  return { memberships, enrollments };
};

/** Best-effort check; fail-open if API not present. Returns false on error. */
export const hasActiveEnrollment = async (programId: number): Promise<boolean> => {
  try {
    const res = await api<{ active: boolean }>(`/my/enrollments/active?programId=${programId}`);
    return !!res?.active;
  } catch {
    return false; // do not block purchase attempts in UI
  }
};

/* Checkouts */

export interface CheckoutResp {
  url: string;         // Stripe hosted checkout URL
  sessionId?: string;  // optional
  // Optional amounts now provided by backend (informational)
  priceCad?: number;
  taxCad?: number;
  totalCad?: number;
  currency?: string;
}

export const startProgramCheckout = (programId: number, packageId: number) =>
  api<CheckoutResp>('/booking/checkout/program', {
    method: 'POST',
    body: JSON.stringify({ programId, packageId }),
  });

/**
 * Preferred enrollment checkout (legacy hosted page), with robust fallback.
 * The UI now prefers inline payment via startProgramPaymentIntent.
 */
export const startProgramEnrollmentCheckout = async (programPackageId: number) => {
  try {
    const body = { programPackageId, packageId: programPackageId };
    return await api<CheckoutResp>('/booking/checkout/enrollment', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  } catch (_e: any) {
    return await startProgramCheckout(0 as any, programPackageId);
  }
};

/** Membership checkout (inline) returns a PaymentIntent client secret + amounts. */
export interface MembershipStartResp {
  bookingId: number;
  clientSecret: string;
  priceCad?: number;
  taxCad?: number;
  totalCad?: number;
  currency?: string;
}

export const startMembershipCheckout = (planId: number) =>
  api<MembershipStartResp>('/booking/checkout/membership', {
    method: 'POST',
    body: JSON.stringify({ planId }),
  });

/** Server-side quote for tax/total display before starting checkout. */
export const fetchMembershipQuote = (planId: number) =>
  api<{ priceCad: number; taxCad: number; totalCad: number; currency: string }>(
    `/memberships/plans/${planId}/quote`
  );

/* Program inline checkout (Stripe PaymentIntent) */

export interface ProgramStartResp {
  bookingId: number;
  clientSecret: string;
  priceCad?: number;
  taxCad?: number;
  totalCad?: number;
  currency?: string;
}

/**
 * Quote amounts for a specific program package. Tries modern endpoint first,
 * then a legacy alternative. Fails gracefully to allow UI to show package price.
 */
export const fetchProgramQuote = async (programPackageId: number) => {
  try {
    // Preferred: REST style (ProgramEnrollmentInlineController)
    return await apiPublic<{ priceCad: number; taxCad: number; totalCad: number; currency: string }>(
      `/programs/packages/${programPackageId}/quote`
    );
  } catch {
    // Legacy: central quote endpoint
    try {
      return await api<{ priceCad: number; taxCad: number; totalCad: number; currency: string }>(
        '/booking/checkout/program/quote',
        { method: 'POST', body: JSON.stringify({ programPackageId, packageId: programPackageId }) }
      );
    } catch {
      return undefined;
    }
  }
};

/**
 * Create a PaymentIntent for a program enrollment, with graceful fallback to hosted checkout.
 * Returns either ProgramStartResp (inline flow) or on older versions, a CheckoutResp with `url`.
 *
 * Important: Avoid returning `undefined` so callers don't have to handle a 3rd case.
 */
export const startProgramPaymentIntent = async (
  programPackageId: number
): Promise<ProgramStartResp | CheckoutResp> => {
  // New REST endpoint (no redirect)
  try {
    const res = await api<ProgramStartResp>(
      `/programs/packages/${programPackageId}/payment-intent`,
      { method: 'POST' }
    );
    if (res?.clientSecret) return res as ProgramStartResp;
    throw new Error('Empty response');
  } catch {
    // fall through
  }

  // Legacy inline endpoints
  try {
    const res = await api<ProgramStartResp>('/booking/checkout/program/payment', {
      method: 'POST',
      body: JSON.stringify({ programPackageId, packageId: programPackageId }),
    });
    if (res?.clientSecret) return res as ProgramStartResp;
    throw new Error('Empty response');
  } catch {
    // fall through
  }

  try {
    const res = await api<ProgramStartResp>('/booking/checkout/enrollment/payment', {
      method: 'POST',
      body: JSON.stringify({ programPackageId, packageId: programPackageId }),
    });
    if (res?.clientSecret) return res as ProgramStartResp;
    throw new Error('Empty response');
  } catch {
    // fall through
  }

  // Hosted fallback - keeps legacy instances working
  const hosted = await startProgramEnrollmentCheckout(programPackageId);
  if (hosted && (hosted as CheckoutResp).url) return hosted as CheckoutResp;

  // If even hosted returns nothing, bubble an actionable error
  throw new Error('Checkout is not available at the moment.');
};

/* Optional finalize */

/**
 * Finalize membership immediately after client-side confirmation (optional).
 * Webhook remains the source of truth; failures are swallowed.
 */
export const finalizeMembershipAfterPayment = async (payload: {
  paymentIntentId: string;
  bookingId?: number;
}): Promise<{ status?: string } | void> => {
  try {
    return await api<{ status: string }>('/booking/checkout/membership/finalize', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  } catch (e: any) {
    const msg = (e?.message || '').toLowerCase();
    if (msg.includes('404') || msg.includes('405')) return;
    if (typeof console !== 'undefined') console.warn('finalizeMembershipAfterPayment failed (non-fatal):', e);
    return;
  }
};

/**
 * Finalize program enrollment after client-side confirmation (optional).
 * Gracefully swallows 404/405 for older servers.
 */
export const finalizeProgramEnrollmentAfterPayment = async (payload: {
  paymentIntentId: string;
  bookingId?: number;
  programPackageId?: number;
}): Promise<{ status?: string } | void> => {
  // REST endpoint first
  try {
    return await api<{ status: string }>(`/programs/packages/finalize`, {
      method: 'POST',
      body: JSON.stringify({ paymentIntentId: payload.paymentIntentId, bookingId: payload.bookingId }),
    });
  } catch (e: any) {
    const msg = (e?.message || '').toLowerCase();
    if (!(msg.includes('404') || msg.includes('405'))) {
      if (typeof console !== 'undefined') console.warn('finalizeProgramEnrollmentAfterPayment (REST) failed (non-fatal):', e);
    }
  }

  // Legacy finalize endpoints
  const bodies = [
    ['/booking/checkout/program/finalize', payload],
    ['/booking/checkout/enrollment/finalize', { ...payload, packageId: payload.programPackageId }],
  ] as const;

  for (const [path, body] of bodies) {
    try {
      return await api<{ status: string }>(path, {
        method: 'POST',
        body: JSON.stringify(body),
      });
    } catch (e: any) {
      const msg = (e?.message || '').toLowerCase();
      if (msg.includes('404') || msg.includes('405')) continue; // try next / swallow
      if (typeof console !== 'undefined') console.warn('finalizeProgramEnrollmentAfterPayment failed (non-fatal):', e);
      return;
    }
  }
  return;
};

/* Admin Enrollments */

export type AdminEnrollmentStatus = 'ACTIVE' | 'EXHAUSTED' | 'CANCELLED';

export interface AdminEnrollmentRow {
  id: number;
  userId: number;
  userName: string | null;
  userEmail: string;
  programId: number;
  programTitle: string | null;
  packageId: number;
  packageName: string | null;
  status: AdminEnrollmentStatus;
  sessionsPurchased: number | null;
  sessionsRemaining: number | null;
  startTs: string | null;
  endTs: string | null;
  lastAttendedAt: string | null;
}

/** Generic page wrapper aligned to backend PageDto */
export interface Paged<T> {
  content: T[];
  page: number;          // zero-based
  size: number;
  totalElements: number;
  totalPages: number;
}

export const adminSearchEnrollments = (q?: string, page = 0, size = 10) => {
  const params = new URLSearchParams();
  if (q && q.trim()) params.set('q', q.trim());
  params.set('page', String(Math.max(0, page)));
  params.set('size', String(Math.min(Math.max(1, size), 100)));
  return api<Paged<AdminEnrollmentRow>>(`/admin/enrollments?${params.toString()}`);
};

/* Admin user search (clients only + lists) */

export interface AdminUserRow {
  id: number;
  name: string | null;
  email: string;
  hasActiveInitial: boolean;
  activeMemberships: number;
  activeEnrollments: number;
  /** sum of sessionsRemaining across ACTIVE enrollments */
  lessonsRemaining?: number;
  /** detailed lists for display */
  memberships?: Array<{
    userMembershipId: number;
    planId: number;
    planName: string | null;
    endTs: string | null;
  }>;
  enrollments?: Array<{
    enrollmentId: number;
    programTitle: string | null;
    packageName: string | null;
    sessionsRemaining: number | null;
  }>;
}

export interface AdminUserPage extends Paged<AdminUserRow> {}

const normalizeAdminUserPage = (p?: AdminUserPage) => {
  if (!p) return p;
  return {
    ...p,
    content: (p.content ?? []).map((r: any) => ({
      ...r,
      // Accept either field name; prefer the standard 'enrollments'
      enrollments: r.enrollments ?? r.enrollmentsList ?? [],
    })),
  };
};

export const adminSearchUsers = async (q = '', page = 0, size = 10) => {
  const params = new URLSearchParams();
  if (q && q.trim()) params.set('q', q.trim());
  params.set('page', String(Math.max(0, page)));
  params.set('size', String(Math.min(Math.max(1, size), 100)));
  const qs = params.toString();

  try {
    const res = await api<AdminUserPage>(`/admin/booking/users?${qs}`);
    return normalizeAdminUserPage(res);
  } catch {
    // Legacy fallback stays intact
    const res = await api<AdminUserPage>(`/admin/users?${qs}`);
    return normalizeAdminUserPage(res);
  }
};


/* Admin - Manual membership sale */

export interface AdminManualMembershipReq {
  userId: number;
  planId: number;
  paymentRef?: string | null;  // POS receipt / cash note
  paidAt?: string | null;      // ISO timestamp; default = now (server)
  notes?: string | null;
}

export interface AdminManualMembershipResp {
  ok: boolean;
  paymentId?: number | null;
}

export const adminManualMembership = (payload: AdminManualMembershipReq) =>
  api<AdminManualMembershipResp>('/admin/booking/manual/membership', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

/* Admin - Manual program enrollment */

export interface AdminManualEnrollmentReq {
  userId: number;
  programPackageId: number;
  paymentRef?: string | null;
  paidAt?: string | null;       // ISO timestamp; default = now (server)
  notes?: string | null;
}

export interface AdminManualEnrollmentResp {
  ok: boolean;
  enrollmentId?: number | null;
  paymentId?: number | null;
}

export const adminManualEnrollment = (payload: AdminManualEnrollmentReq) =>
  api<AdminManualEnrollmentResp>('/admin/booking/manual/enrollment', {
    method: 'POST',
    body: JSON.stringify(payload),
  });


/* Table Rentals */

export interface TableRentalPackageDto {
  id: number;
  name: string;
  hours: number;          // e.g. 1, 5, 10 (0.5 increments allowed)
  priceCad: number;
  active: boolean;
  sortOrder?: number | null;
}

export interface TableRentalQuote {
  priceCad: number;
  taxCad: number;
  totalCad: number;
  currency: string;
}

export interface TableRentalStartResp {
  bookingId: number;
  clientSecret: string;
  priceCad?: number;
  taxCad?: number;
  totalCad?: number;
  currency?: string;
}

/* Public table credits */
export const fetchTableRentalPackages = () =>
  apiPublic<TableRentalPackageDto[]>('/table-credits/packages');

export const fetchTableRentalQuote = (packageId: number) =>
  apiPublic<TableRentalQuote>(`/table-credits/packages/${packageId}/quote`);

export const startTableRentalPaymentIntent = (packageId: number) =>
  api<TableRentalStartResp>(`/table-credits/packages/${packageId}/payment-intent`, { method: 'POST' });

export const finalizeTableRentalAfterPayment = (payload: { paymentIntentId: string; bookingId?: number }) =>
  api<{ status: string }>('/table-credits/finalize', { method: 'POST', body: JSON.stringify(payload) });

/* Admin table credits: packages management */
export const adminListTableCreditPackages = () =>
  api<TableRentalPackageDto[]>('/admin/table-credits/packages');

export const adminCreateTableCreditPackage = (payload: { name: string; hours: number; priceCad: number; active?: boolean; sortOrder?: number | null; }) =>
  api<TableRentalPackageDto>('/admin/table-credits/packages', { method: 'POST', body: JSON.stringify(payload) });

export const adminUpdateTableCreditPackage = (id: number, payload: Partial<{ name: string; hours: number; priceCad: number; active: boolean; sortOrder: number | null; }>) =>
  api<TableRentalPackageDto>(`/admin/table-credits/packages/${id}`, { method: 'PUT', body: JSON.stringify(payload) });

export const adminDeleteTableCreditPackage = (id: number) =>
  api<void>(`/admin/table-credits/packages/${id}`, { method: 'DELETE' });

/* Table-hours credits (moved) */
export {
  fetchMyTableCreditSummary,
  adminSearchUsersWithCredits,
  adminConsumeTableHours,
  adminManualGrantTableHours,
  type TableCreditSummary,
  type AdminCreditUserRow,
} from './credits';