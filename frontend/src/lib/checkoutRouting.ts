/* One-liner helper for the drawer's button.
Keeps routing logic out of navbar/cart drawer component.*/

import { type Location, type NavigateFunction } from 'react-router-dom';

export function handleNavbarCheckout(opts: {
  role?: string | null;
  locationPath: string;
  navigate: NavigateFunction;
  closeDrawer?: () => void;
}) {
  const { role, locationPath, navigate, closeDrawer } = opts;
  const isAdmin = role === 'ADMIN' || role === 'OWNER';

  if (isAdmin) {
    if (locationPath.startsWith('/admin/manual-shop')) {
      // Already on manual page then just close the drawer
      closeDrawer?.();
      return;
    }
    // Go to manual page (cart is prepopulated there)
    closeDrawer?.();
    navigate('/admin/manual-shop');
    return;
  }

  // Client flow (3-step online checkout)
  closeDrawer?.();
  navigate('/checkout');
}
