/**
 * Attendance API client (admin-only)
 * Uses the central api() wrapper, inherits auth and refresh.
 * Endpoints:
 *   GET  /api/admin/booking/attendance?occurrenceId=...   -> eligible list
 *   GET  /api/admin/booking/attendance?programId=...&date=YYYY-MM-DD
 *   POST /api/admin/booking/attendance/mark  { occurrenceId, userId, present }
 */

import { api } from './api';

export interface AttendanceOccurrence {
  id: number;
  programId: number | null;
  title: string | null;
  start: string;  // ISO
  end: string;    // ISO
}

export interface AttendanceEligibleUser {
  userId: number;
  name: string | null;
  email: string;
  enrollmentId: number;
  sessionsRemaining: number;
  present: boolean; // whether attendance for this occurrence already exists
}

export interface AttendanceListResp {
  occurrence: AttendanceOccurrence;
  users: AttendanceEligibleUser[];
}

/** Load eligible users for an occurrence (preferred) or by program/date. */
export const adminFetchAttendance = (args:
  | { occurrenceId: number }
  | { programId: number; date: string } // date = YYYY-MM-DD (local)
) => {
  const params = new URLSearchParams();
  if ('occurrenceId' in args) params.set('occurrenceId', String(args.occurrenceId));
  if ('programId' in args && 'date' in args) {
    params.set('programId', String(args.programId));
    params.set('date', args.date);
  }
  return api<AttendanceListResp>(`/admin/booking/attendance?${params.toString()}`);
};

export const adminMarkAttendance = (payload: {
  occurrenceId: number;
  userId: number;
  present: boolean;  // true = consume 1 session; false = undo
}) =>
  api<void>('/admin/booking/attendance/mark', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
