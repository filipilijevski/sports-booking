import {
  createContext,
  useContext,
  useState,
  useCallback,
  type ReactNode,
} from 'react';

import AuthDialog from '../components/AuthDialog';

/** What the hook will expose */
interface Ctx {
  open:  () => void;
  close: () => void;
}

const Dummy: Ctx = { open: () => {}, close: () => {} };
const AuthDialogCtx = createContext<Ctx>(Dummy);

/* Provider that owns the <AuthDialog/> and toggles its visibility */
export function AuthDialogProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);

  const show  = useCallback(() => setOpen(true), []);
  const hide  = useCallback(() => setOpen(false), []);

  return (
    <AuthDialogCtx.Provider value={{ open: show, close: hide }}>
      {children}

      {/* one single instance for the whole SPA */}
      <AuthDialog
        open={open}
        onClose={hide}
        onAuthSuccess={hide}      // close on successful login / register
      />
    </AuthDialogCtx.Provider>
  );
}

/* Hook - call anywhere to open/close  */
export function useAuthDialog() {
  return useContext(AuthDialogCtx);
}
