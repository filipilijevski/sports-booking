import {
  createContext, useContext, useEffect, useState, type ReactNode,
} from 'react';
import { api } from '../lib/api';

export type Role = 'OWNER' | 'ADMIN' | 'COACH' | 'CLIENT' | 'GUEST';

interface MeDto { role?: string | { name?: string } }

interface Ctx {
  role: Role;
  ready: boolean;
  pwdChangeRequired: boolean;
  refresh: () => Promise<void>;
}

const RoleCtx = createContext<Ctx>({
  role: 'GUEST',
  ready: false,
  pwdChangeRequired: false,
  refresh: async () => {},
});

export function RoleProvider({ children }: { children: ReactNode }) {
  const [role, setRole] = useState<Role>('GUEST');
  const [ready, setReady] = useState(false);
  const [mustChange, setMustChange] = useState(false);

  const fetchRole = async () => {
    try {
      const me = await api<MeDto>('/users/me');
      const raw = typeof me?.role === 'string'
        ? me.role
        : (me?.role as any)?.name;
      setRole((raw as Role) ?? 'GUEST');
    } catch {
      setRole('GUEST');
    } finally {
      setReady(true);
    }

    try {
      const sess = await api<{ pwdChangeRequired: boolean }>('/auth/session');
      setMustChange(!!sess?.pwdChangeRequired);
    } catch {
      setMustChange(false);
    }
  };

  useEffect(() => { fetchRole(); }, []);

  useEffect(() => {
    const cb = (e: StorageEvent) => {
      if (e.key === 'accessToken') fetchRole();
    };
    window.addEventListener('storage', cb);
    return () => window.removeEventListener('storage', cb);
  }, []);

  useEffect(() => {
    const cb = () => fetchRole();
    window.addEventListener('jwt-updated', cb);
    return () => window.removeEventListener('jwt-updated', cb);
  }, []);

  return (
    <RoleCtx.Provider value={{ role, ready, pwdChangeRequired: mustChange, refresh: fetchRole }}>
      {children}
    </RoleCtx.Provider>
  );
}

export const useRole = () => useContext(RoleCtx);
