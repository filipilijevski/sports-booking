/* Main SPA shell - routing & top-level providers */

import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';

/* context providers */
import { RoleProvider, useRole } from './context/RoleContext';
import { CartProvider }          from './context/CartContext';
import {
  AuthDialogProvider,
  useAuthDialog,
} from './context/AuthDialogContext';

/* route guards */
import { AdminRoute, ClientRoute } from './routes/RouteGuards';

/* layout components */
import NavBar       from './components/NavBar';
import Footer       from './components/Footer';
import CartDrawer   from './components/CartDrawer';

/* pages */
import Home           from './pages/Home';
import Schedule       from './pages/Schedule';
import Profile        from './pages/Profile';
import OAuth2Callback from './pages/OAuth2Callback';
import About          from './pages/About';
import Coaching       from './pages/Coaching';
import TableRentals   from './pages/TableRentals';

/*   Pro-Shop related */
import Shop           from './pages/Shop';
import ProductDetail  from './pages/ProductDetail';
import Checkout       from './pages/Checkout';
import ThankYou       from './pages/ThankYou'; // Pro-Shop only

/*  thank-you pages (booking flows) */
import ThankYouMembership from './pages/ThankYouMembership';

import AdminShop      from './pages/AdminShop';
import AdminOrders    from './pages/AdminOrders';
import MyOrders       from './pages/MyOrders';

/*  Admin: Manage Profiles  */
import AdminUsers from './pages/AdminUsers';

/*  Admin: Manage Programs & Memberships (split) */
import AdminCreateProgramPlans from './pages/AdminCreateProgramPlans';
import AdminCreateMembershipPlans from './pages/AdminCreateMembershipPlans';
import AdminTableRentalCredits from './pages/AdminTableRentalCredits';

/*  Admin: Manage Program Schedules  */
import ManageProgramSchedules from './pages/ManageProgramSchedules';

/*  Admin: Dedicated Membership Groups page */
import AdminMembershipGroups from './pages/AdminMembershipGroups';

import ThankYouProgram from './pages/ThankYouProgram';

/*  Admin: Enroll Users  */
import AdminEnrollUsers from './pages/AdminEnrollUsers';
import ThankYouTableCredits from './pages/ThankYouTableCredits';

/* Blog pages */
import News from './pages/News';
import BlogPostDetailPage from './pages/BlogPostDetail';
import AdminBlog from './pages/AdminBlog';

import AdminManualShop from './pages/AdminManualShop';

/* Admin shell and helper pages */
import AdminLayoutRight from './components/admin/AdminLayoutRight';
import AdminOverview from './pages/AdminOverview';
import AdminCoupons from './pages/AdminCoupons';
import AdminReports from './pages/AdminReports';

import ForcePasswordReset from './pages/ForcePasswordReset';

/* dedicated product audit page */
import AdminShopAudit from './pages/AdminShopAudit';

/* auth helpers */
import { logout } from './lib/auth';

const USE_COOKIE_AUTH =
  String(import.meta.env.VITE_USE_COOKIE_AUTH ?? '').toLowerCase() === 'true';

/* Helper - wraps <NavBar> so it can open the auth dialog */
function NavBarWrapper(
  { authed, firstName, onLogout }:
  { authed: boolean; firstName: string | null; onLogout: () => void },
) {
  const { open } = useAuthDialog();
  const { role } = useRole();

  // Merge: legacy prop OR cookie-mode signal
  const mergedAuthed = authed || (role !== 'GUEST');

  return (
    <NavBar
      authed={mergedAuthed}
      firstName={firstName}
      onLogout={onLogout}
      onLoginClick={open}
    />
  );
}

/* Hide public chrome for admins globally; public chrome still hidden on /admin/* */
function useIsAdminArea() {
  const location = useLocation();
  return location.pathname.startsWith('/admin');
}

export default function App() {
  const [authed,    setAuthed]    = useState<boolean>(!!localStorage.getItem('accessToken'));
  const [firstName, setFirstName] = useState<string | null>(localStorage.getItem('firstName'));

  useEffect(() => {
    const sync = () => {
      setAuthed(!!localStorage.getItem('accessToken'));
      setFirstName(localStorage.getItem('firstName'));
    };
    window.addEventListener('jwt-updated', sync);
    window.addEventListener('storage',     sync);
    return () => {
      window.removeEventListener('jwt-updated', sync);
      window.removeEventListener('storage',     sync);
    };
  }, []);

  const handleLogout = () => logout();

  return (
    <RoleProvider>
      <CartProvider>
        <AuthDialogProvider>
          <BrowserRouter>
            <Chrome authed={authed} firstName={firstName} onLogout={handleLogout} />
          </BrowserRouter>
        </AuthDialogProvider>
      </CartProvider>
    </RoleProvider>
  );
}

function Chrome(
  { authed, firstName, onLogout }:
  { authed: boolean; firstName: string | null; onLogout: () => void }
) {
  const isAdminArea = useIsAdminArea();
  const { role, ready, pwdChangeRequired } = useRole();
  const nav = useNavigate();
  const location = useLocation();

  const isAdmin = role === 'ADMIN' || role === 'OWNER';
  const isLogged = USE_COOKIE_AUTH ? (ready && role !== 'GUEST') : authed;

  useEffect(() => {
    if (!isAdmin) return;
    const path = location.pathname;
    if (path.startsWith('/admin') || path.startsWith('/oauth2/')) return;
    const mappings: Record<string, string> = {
      '/': '/admin',
      '/profile': '/admin/profile',
      '/about': '/admin/client/about',
      '/news': '/admin/client/news',
      '/table-rentals': '/admin/client/table-rentals',
    };
    const target = mappings[path] || '/admin';
    nav(target, { replace: true });
  }, [isAdmin, location.pathname, nav]);

  // force the dedicated change-password screen (instead of /profile)
  useEffect(() => {
    if (!isLogged) return;
    if (pwdChangeRequired && location.pathname !== '/password-reset') {
      nav('/password-reset', { replace: true });
    }
  }, [pwdChangeRequired, isLogged, location.pathname, nav]);

  const showPublicChrome = !isAdmin && !isAdminArea;

  return (
    <>
      {showPublicChrome && (
        <>
          <NavBarWrapper
            authed={authed}
            firstName={firstName}
            onLogout={onLogout}
          />
          <CartDrawer />
        </>
      )}

      <Routes>
        <Route path="/" element={<Home />} />

        {/* Pro-Shop */}
        <Route path="/shop"      element={<Shop />} />
        <Route path="/shop/:id"  element={<ProductDetail />} />
        <Route path="/checkout"  element={<Checkout />} />
        <Route path="/thanks"    element={<ThankYou />} />

        {/* booking thank-you pages */}
        <Route path="/thank-you/membership"    element={<ThankYouMembership />} />
        <Route path="/thank-you/program"       element={<ThankYouProgram />} />
        <Route path="/thank-you/table-credits" element={<ThankYouTableCredits />} />

        {/* OAuth2 callback */}
        <Route path="/oauth2/callback" element={<OAuth2Callback />} />

        {/* direct “/login” then open modal, then bounce back */}
        <Route path="/login" element={<Navigate to="/shop" replace />} />

        {/* Member-only pages - gate by RoleContext in cookie-mode */}
        <Route
          path="/schedule"
          element={isLogged ? <Schedule /> : <Navigate to="/" replace />}
        />
        <Route
          path="/profile"
          element={isLogged ? <Profile /> : <Navigate to="/" replace />}
        />

        <Route
          path="/password-reset"
          element={isLogged ? <ForcePasswordReset /> : <Navigate to="/" replace />}
        />

        {/* Public pages */}
        <Route path="/coaching"      element={<Coaching />} />
        <Route path="/about"         element={<About />} />
        <Route path="/table-rentals" element={<TableRentals />} />

        {/* public blog/news */}
        <Route path="/news"          element={<News />} />
        <Route path="/news/:id"      element={<BlogPostDetailPage />} />

        {/* Client-only page */}
        <Route
          path="/my-orders"
          element={
            <ClientRoute>
              <MyOrders />
            </ClientRoute>
          }
        />

        {/* Admin Routes */}
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminLayoutRight />
            </AdminRoute>
          }
        >
          <Route index element={<AdminOverview />} />

          {/* Shop */}
          <Route path="shop"         element={<AdminShop />} />
          <Route path="orders"       element={<AdminOrders />} />
          <Route path="manual-shop"  element={<AdminManualShop />} />
          <Route path="coupons"      element={<AdminCoupons />} />
          <Route path="shop-audit"   element={<AdminShopAudit />} /> 

          {/* Customers */}
          <Route path="profiles"     element={<AdminUsers />} />

          {/* Booking (split) */}
          <Route path="booking/program-plans"     element={<AdminCreateProgramPlans />} />
          <Route path="booking/membership-plans"  element={<AdminCreateMembershipPlans />} />
          <Route path="booking/table-credits"     element={<AdminTableRentalCredits />} />

          <Route path="enroll"                element={<AdminEnrollUsers />} />
          <Route path="schedules"             element={<ManageProgramSchedules />} />
          <Route path="membership-groups"     element={<AdminMembershipGroups />} />

          {/* Blog admin */}
          <Route path="blog"         element={<AdminBlog />} />

          {/* Client-view previews inside admin shell */}
          <Route path="client/home"            element={<Home />} />
          <Route path="client/table-rentals"   element={<TableRentals />} />
          <Route path="client/about"           element={<About />} />
          <Route path="client/news"            element={<News />} />

          {/* Admin-wrapped profile page */}
          <Route path="profile"      element={<Profile />} />

          {/* Reports placeholder */}
          <Route path="reports"      element={<AdminReports />} />
        </Route>
      </Routes>

      {!useIsAdminArea() && <Footer />}
    </>
  );
}
