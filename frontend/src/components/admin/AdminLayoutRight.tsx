import { useMemo, useState, useEffect } from 'react';
import {
  Box, CssBaseline, Drawer, Container, useMediaQuery, useTheme,
} from '@mui/material';
import { Outlet, useLocation } from 'react-router-dom';
import AdminRightNav, { NAV_WIDTH } from './AdminRightNav';
import AdminTopBar from './AdminTopBar';
import {
  ADMIN_NAV_SECTIONS,
  ADMIN_SECONDARY_ITEMS,
} from './adminNavConfig';

const INFO_GRADIENT = 'linear-gradient(180deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

type Crumb = { label: string; to?: string };

/** Build a simple lookup for exact matches of known admin routes */
function useNavIndex() {
  return useMemo(() => {
    const map = new Map<string, { section?: string; sectionTo?: string; label: string }>();
    for (const s of ADMIN_NAV_SECTIONS) {
      if (s.to) map.set(s.to, { label: s.label });
      for (const it of s.items ?? []) {
        if (it.to) map.set(it.to, { section: s.label, sectionTo: s.to, label: it.label });
      }
    }
    for (const it of ADMIN_SECONDARY_ITEMS) {
      if (it.to) map.set(it.to, { label: it.label });
    }
    return map;
  }, []);
}

/** Derive breadcrumbs from nav config, with graceful fallback for unknown subpaths */
function useBreadcrumbs(pathname: string) {
  const idx = useNavIndex();

  const crumbs: Crumb[] = useMemo(() => {
    // Always start with the admin root
    const base: Crumb[] = [{ label: 'Overview', to: '/admin' }];

    const info = idx.get(pathname);
    if (info) {
      if (info.section) base.push({ label: info.section }); // no direct section route in most cases
      base.push({ label: info.label });
      return base;
    }

    // Fallback: derive from the path segments (for preview pages, etc.)
    const parts = pathname.replace(/^\/+|\/+$/g, '').split('/');
    if (parts[0] !== 'admin') return base;

    const tail = parts.slice(1);
    if (tail.length === 0) return base;

    const labels = tail.map(seg =>
      seg
        .replace(/[-_]/g, ' ')
        .replace(/\b\w/g, (m) => m.toUpperCase())
    );

    // Accumulate path for intermediate clickable crumbs if they are known
    let acc = '/admin';
    for (let i = 0; i < tail.length; i++) {
      acc += `/${tail[i]}`;
      const known = idx.get(acc);
      base.push(known ? { label: known.label, to: i < tail.length - 1 ? acc : undefined } : { label: labels[i] });
    }
    return base;
  }, [idx, pathname]);

  return crumbs;
}

export default function AdminLayoutRight() {
  const theme = useTheme();
  const isLargeNav = useMediaQuery(theme.breakpoints.up('lg')); // permanent drawer threshold
  const isSmUp     = useMediaQuery(theme.breakpoints.up('sm')); // toolbar height breakpoint
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();

  // Consistent toolbar height with AdminTopBar: xs=56, sm+=64
  const TOPBAR_PX = isSmUp ? 64 : 56;

  // Scroll to top on route change (dashboard UX nice to have)
  useEffect(() => { window.scrollTo({ top: 0 }); }, [location.pathname]);

  const toggleDrawer = () => setMobileOpen(v => !v);

  // Page title heuristic (kept for pages that read it)
  const pageTitle = useMemo(() => {
    const p = location.pathname.replace(/^\/+|\/+$/g, '');
    if (!p || p === 'admin') return 'Overview';
    const parts = p.split('/').slice(1); // drop "admin"
    const last = parts[parts.length - 1] || 'Overview';
    return last
      .replace(/[-_]/g, ' ')
      .replace(/\b\w/g, (m) => m.toUpperCase());
  }, [location.pathname]);

  const breadcrumbs = useBreadcrumbs(location.pathname);

  return (
    <Box sx={{ display: 'flex', minHeight: '100dvh', background: INFO_GRADIENT }}>
      <CssBaseline />

      {/* Global top bar across the full viewport width */}
      <AdminTopBar
        title={pageTitle}
        breadcrumbs={breadcrumbs}
        onOpenNav={!isLargeNav ? toggleDrawer : undefined}
      />

      {/* Left drawer */}
      <Box
        component="nav"
        sx={{ width: { lg: NAV_WIDTH }, flexShrink: { lg: 0 } }}
        aria-label="admin navigation"
      >
        {/* Mobile drawer (temporary) - constrained below the fixed top bar */}
        <Drawer
          anchor="left"
          variant="temporary"
          open={mobileOpen}
          onClose={toggleDrawer}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', lg: 'none' },
            zIndex: (t) => t.zIndex.drawer, // below AppBar (which is drawer+1)
            '& .MuiDrawer-paper': {
              top: `${TOPBAR_PX}px`,
              backgroundColor: 'background.paper',
              width: NAV_WIDTH,
              boxSizing: 'border-box',
              height: `calc(100dvh - ${TOPBAR_PX}px)`,
              borderRight: '1px solid',
              borderColor: 'divider',
            },
          }}
        >
          <AdminRightNav />
        </Drawer>

        {/* Desktop drawer (permanent) — starts right under the fixed top bar */}
        <Drawer
          anchor="left"
          variant="permanent"
          open
          sx={{
            display: { xs: 'none', lg: 'block' },
            '& .MuiDrawer-paper': {
              position: 'fixed',
              top: `${TOPBAR_PX}px`,
              left: 0,
              width: NAV_WIDTH,
              boxSizing: 'border-box',
              height: `calc(100dvh - ${TOPBAR_PX}px)`,
              backgroundColor: 'background.paper',
              borderRight: '1px solid',
              borderColor: 'divider',
            },
          }}
        >
          <AdminRightNav />
        </Drawer>
      </Box>

      {/* Main content area (to the right of the fixed left drawer, below the top bar) */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          pt: `${TOPBAR_PX}px`,            // space for fixed top bar
          ml: { lg: `5px` },    // offset for fixed drawer
          width: '100%',
          minWidth: 0,
          display: 'flex',
          flexDirection: 'column',
          overflowX: 'scroll',
        }}
      >
        <Container maxWidth="xl" sx={{ py: { xs: 1, md: 2 }, minWidth: 0 }}>
          <Outlet />
        </Container>
      </Box>
    </Box>
  );
}
