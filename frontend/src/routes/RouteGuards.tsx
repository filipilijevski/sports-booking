import { Navigate } from 'react-router-dom';
import { type PropsWithChildren } from 'react';
import { useRole } from '../context/RoleContext';
import type { Role } from '../context/RoleContext';

const USE_COOKIE_AUTH =
  String(import.meta.env.VITE_USE_COOKIE_AUTH ?? '').toLowerCase() === 'true';

function hasTokenLegacy() {
  return typeof window !== 'undefined' && !!localStorage.getItem('accessToken');
}

/** Generic role-based guard. Denies if not authed or role not allowed. */
export function ProtectedRoute({
  children,
  allowed,
  deny = [],
}: PropsWithChildren<{ allowed: Role[]; deny?: Role[] }>) {
  const { role, ready } = useRole();

  if (USE_COOKIE_AUTH) {
    if (!ready) return null;                 // wait for /users/me probe
    const authed = role !== 'GUEST';
    if (!authed) return <Navigate to="/" replace />;
    if (deny.includes(role)) return <Navigate to="/" replace />;
    if (!allowed.includes(role)) return <Navigate to="/" replace />;
    return <>{children}</>;
  }

  // Legacy behavior: localStorage token + role checks
  if (!hasTokenLegacy()) return <Navigate to="/" replace />;
  if (deny.includes(role)) return <Navigate to="/" replace />;
  if (!allowed.includes(role)) return <Navigate to="/" replace />;
  return <>{children}</>;
}

/** Convenience wrappers */
export const AdminRoute  = ({ children }: PropsWithChildren) =>
  <ProtectedRoute allowed={['ADMIN', 'OWNER']} deny={[]} >{children}</ProtectedRoute>;

export const ClientRoute = ({ children }: PropsWithChildren) =>
  <ProtectedRoute allowed={['CLIENT']} deny={['ADMIN', 'OWNER']} >{children}</ProtectedRoute>;
