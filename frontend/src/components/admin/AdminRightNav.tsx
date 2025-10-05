import * as React from 'react';
import { useMemo, useState, useEffect } from 'react';
import {
  Box, Divider, Typography, List, ListItemButton, ListItemIcon,
  ListItemText, Collapse, Avatar, Stack,
} from '@mui/material';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import StarIcon from '@mui/icons-material/Star';
import { NavLink, useLocation } from 'react-router-dom';
import { useRole } from '../../context/RoleContext';
import { getFirstName, logout } from '../../lib/auth';
import {
  ADMIN_NAV_SECTIONS,
  ADMIN_SECONDARY_ITEMS,
  type AdminNavItem,
  type AdminNavSection,
} from './adminNavConfig';

const NAV_WIDTH = 296;
const LOCAL_KEY = 'admin_nav_expanded_v1';

/** Persist/restore which sections are expanded */
function useExpandedState(sections: AdminNavSection[]) {
  const [state, setState] = useState<Record<string, boolean>>(() => {
    try {
      const raw = localStorage.getItem(LOCAL_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch { return {}; }
  });
  useEffect(() => { localStorage.setItem(LOCAL_KEY, JSON.stringify(state)); }, [state]);
  const toggle = (label: string) => setState(s => ({ ...s, [label]: !s[label] }));
  const isOpen = (label: string) => !!state[label];
  // Default‑open sections that have children, unless already saved
  useEffect(() => {
    setState(prev => {
      const next = { ...prev };
      for (const s of sections) {
        if ((s.items?.length ?? 0) > 0 && !(s.label in next)) next[s.label] = true;
      }
      return next;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return { isOpen, toggle, set: setState };
}

function IdentityHeader() {
  const { role } = useRole();
  const [name, setName] = useState<string | null>(null);

  useEffect(() => {
    setName(getFirstName() ?? null);
    const sync = () => setName(getFirstName() ?? null);
    window.addEventListener('jwt-updated', sync);
    window.addEventListener('storage', sync);
    return () => { window.removeEventListener('jwt-updated', sync); window.removeEventListener('storage', sync); };
  }, []);

  const initials = useMemo(() => {
    const n = (name ?? '').trim();
    if (!n) return 'A';
    const parts = n.split(/\s+/);
    return (parts[0][0] + (parts[1]?.[0] ?? '')).toUpperCase();
  }, [name]);

  return (
    <Box sx={{ p: 2 }}>
      {/* Small logo: brand block (no external asset required) */}
      <Box
        sx={{
          height: 44,
          borderRadius: 5.5,
          mx: 1.5,
          background: 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: 'white', fontWeight: 800, letterSpacing: 0.5,
        }}
        aria-label="Admin logo"
      >
        Admin
      </Box>

      <Stack direction="row" spacing={1.2} alignItems="center" sx={{ mt: 2 }}>
        <Avatar sx={{ width: 36, height: 36, fontWeight: 700 }}>{initials}</Avatar>
        <Box>
          <Typography variant="body2" sx={{ fontWeight: 700, lineHeight: 1.1 }}>
            {name || 'Administrator'}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'capitalize' }}>
            {role?.toLowerCase() || 'guest'}
          </Typography>
        </Box>
      </Stack>
    </Box>
  );
}

function SectionItem({
  item, activePath,
}: { item: AdminNavItem; activePath: string }) {
  const isActive = !!item.to && activePath === item.to;

  const handleClick = (e: React.MouseEvent) => {
    if (item.action === 'logout') {
      e.preventDefault();
      logout();
      return;
    }
    // we do not close the drawer on navigation anymore.
  };

  if (item.action === 'logout') {
    return (
      <ListItemButton onClick={handleClick} sx={{ borderRadius: 1 }}>
        {item.icon ? <ListItemIcon>{item.icon}</ListItemIcon> : null}
        <ListItemText primary={item.label} />
      </ListItemButton>
    );
  }

  return (
    <ListItemButton
      component={NavLink as any}
      to={item.to!}
      onClick={handleClick}
      sx={(theme) => ({
        borderRadius: 1,
        '&.active': {
          backgroundColor: theme.palette.action.selected,
        },
      })}
      className={isActive ? 'active' : undefined}
    >
      {item.icon ? <ListItemIcon>{item.icon}</ListItemIcon> : null}
      <ListItemText primary={item.label} />
    </ListItemButton>
  );
}

export default function AdminRightNav() {
  const location = useLocation();
  const { isOpen, toggle } = useExpandedState(ADMIN_NAV_SECTIONS);

  return (
    <Box
      role="navigation"
      aria-label="Admin navigation"
      sx={{ width: NAV_WIDTH, height: '100dvh', display: 'flex', flexDirection: 'column' }}
    >
      {/* Separate from links */}
      <IdentityHeader />
      <Divider />

      {/* Links */}
      <Box sx={{ flex: 1, overflowY: 'auto', py: 1 }}>
        <List disablePadding>
          {ADMIN_NAV_SECTIONS.map((section) => {
            const hasChildren = (section.items?.length ?? 0) > 0;
            const isActive = !!section.to && location.pathname === section.to;

            return (
              <Box key={section.label}>
                <ListItemButton
                  component={section.to && !hasChildren ? (NavLink as any) : 'button'}
                  to={section.to && !hasChildren ? section.to : undefined}
                  onClick={() => { if (hasChildren) toggle(section.label); }}
                  sx={(theme) => ({
                    borderRadius: 1,
                    mx: 1,
                    '&.active': { backgroundColor: theme.palette.action.selected },
                  })}
                  className={isActive ? 'active' : undefined}
                >
                  <ListItemIcon><StarIcon fontSize="small" /></ListItemIcon>
                  <ListItemText
                    primary={<Typography sx={{ fontWeight: 700 }}>{section.label}</Typography>}
                    secondary={!hasChildren ? undefined : (isOpen(section.label) ? 'Hide' : 'Show')}
                    secondaryTypographyProps={{ sx: { textTransform: 'uppercase', fontSize: 10, letterSpacing: 0.3 } }}
                  />
                  {hasChildren ? (isOpen(section.label) ? <ExpandLess /> : <ExpandMore />) : null}
                </ListItemButton>

                {hasChildren && (
                  <Collapse in={isOpen(section.label)} timeout="auto" unmountOnExit>
                    <List disablePadding sx={{ pl: 2, pr: 1, pb: 1 }}>
                      {section.items!.map((item) => (
                        <SectionItem
                          key={`${section.label}:${item.label}`}
                          item={item}
                          activePath={location.pathname}
                        />
                      ))}
                    </List>
                  </Collapse>
                )}
                <Divider sx={{ my: 0.5 }} />
              </Box>
            );
          })}
        </List>
      </Box>

      {/* Secondary bottom actions */}
      <Divider />
      <Box sx={{ py: 0.5 }}>
        <List disablePadding>
          {ADMIN_SECONDARY_ITEMS.map((item) => (
            <SectionItem
              key={`secondary:${item.label}`}
              item={item}
              activePath={location.pathname}
            />
          ))}
        </List>
      </Box>
    </Box>
  );
}

export { NAV_WIDTH };
