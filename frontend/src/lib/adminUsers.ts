import { api } from './api';

export interface AdminUser {
  id: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: string;
}

export async function searchAdminUsers(q: string, page = 0, size = 5): Promise<{
  content: AdminUser[];
  totalPages: number;
  totalElements: number;
}> {
  const usp = new URLSearchParams();
  if (q) usp.set('q', q);
  usp.set('page', String(page));
  usp.set('size', String(size));

  const res = await api<any>(`/admin/users?${usp.toString()}`);
  // Accept both Page<T> and raw arrays for hardening
  if (res && Array.isArray(res.content)) {
    return {
      content: res.content as AdminUser[],
      totalPages: Number(res.totalPages ?? 1),
      totalElements: Number(res.totalElements ?? (res.content as any[]).length),
    };
  }
  return {
    content: Array.isArray(res) ? res as AdminUser[] : [],
    totalPages: 1,
    totalElements: Array.isArray(res) ? (res as any[]).length : 0,
  };
}
