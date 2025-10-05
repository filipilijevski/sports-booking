import * as React from 'react';
import { useEffect, useMemo, useState } from 'react';
import {
  AppBar,
  Toolbar,
  IconButton,
  Typography,
  Box,
  Avatar,
  Menu,
  MenuItem,
  Divider,
  ListItemIcon,
  Tooltip,
  useMediaQuery,
  useTheme,
  Breadcrumbs,
  Link as MuiLink,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import LogoutIcon from '@mui/icons-material/Logout';
import PersonIcon from '@mui/icons-material/Person';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import { Link as RouterLink, useNavigate } from 'react-router-dom';

import { getFirstName, logout } from '../../lib/auth';
import { useRole } from '../../context/RoleContext';

type Crumb = { label: string; to?: string };

export default function AdminTopBar({
  breadcrumbs,
  onOpenNav,
}: {
  title: string;
  breadcrumbs?: Crumb[];
  onOpenNav?: () => void;
}) {
  const theme = useTheme();
  const isLargeNav = useMediaQuery(theme.breakpoints.up('lg')); // permanent drawer threshold
  const { role } = useRole();
  const navigate = useNavigate();

  const [name, setName] = useState<string | null>(null);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  useEffect(() => {
    setName(getFirstName() ?? null);
    const sync = () => setName(getFirstName() ?? null);
    window.addEventListener('jwt-updated', sync);
    window.addEventListener('storage', sync);
    return () => {
      window.removeEventListener('jwt-updated', sync);
      window.removeEventListener('storage', sync);
    };
  }, []);

  const initials = useMemo(() => {
    const n = (name ?? '').trim();
    if (!n) return 'A';
    const parts = n.split(/\s+/);
    return (parts[0][0] + (parts[1]?.[0] ?? '')).toUpperCase();
  }, [name]);

  const open = Boolean(anchorEl);
  const handleMenu = (e: React.MouseEvent<HTMLElement>) => setAnchorEl(e.currentTarget);
  const handleClose = () => setAnchorEl(null);

  const goProfile = () => {
    handleClose();
    navigate('/admin/profile');
  };

  const doLogout = () => {
    handleClose();
    logout();
  };

  return (
    <AppBar
      /* Fixed + highest z-index so it always sits above the drawer and its backdrop */
      position="fixed"
      elevation={0}
      color="default"
      sx={{
        bgcolor: 'background.paper',
        borderBottom: '1px solid',
        borderColor: 'divider',
        zIndex: (t) => t.zIndex.drawer + 1,
      }}
    >
      <Toolbar sx={{ minHeight: { xs: 56, sm: 64 } }}>
        {/* Mobile hamburger - only when the drawer is not permanent */}
        {!isLargeNav && (
          <IconButton
            edge="start"
            aria-label="Open navigation"
            onClick={onOpenNav}
            sx={{ mr: 1 }}
          >
            <MenuIcon />
          </IconButton>
        )}

        {/* Breadcrumbs (truncate gracefully) */}
        <Box sx={{ display: 'flex', alignItems: 'center', minWidth: 0, flexShrink: 1 }}>
          {breadcrumbs && breadcrumbs.length > 0 && (
            <Breadcrumbs
              aria-label="breadcrumb"
              separator={<NavigateNextIcon fontSize="small" />}
              sx={{
                maxWidth: { xs: '42vw', sm: '55vw', md: '60vw' },
                overflow: 'hidden',
                '& .MuiBreadcrumbs-ol': { whiteSpace: 'nowrap' },
              }}
            >
              {breadcrumbs.map((c, idx) =>
                c.to && idx < breadcrumbs.length - 1 ? (
                  <MuiLink
                    key={`${c.label}:${idx}`}
                    component={RouterLink}
                    to={c.to}
                    underline="hover"
                    color="inherit"
                    sx={{ fontSize: 14 }}
                  >
                    {c.label}
                  </MuiLink>
                ) : (
                  <Typography key={`${c.label}:${idx}`} color="text.primary" sx={{ fontSize: 14, fontWeight: 600 }}>
                    {c.label}
                  </Typography>
                )
              )}
            </Breadcrumbs>
          )}
        </Box>

        {/* Filler to push avatar to right */}
        <Box sx={{ flexGrow: 1 }} />

        {/* Avatar + menu */}
        <Tooltip title={name ? name : 'Administrator'}>
          <IconButton onClick={handleMenu} size="small" sx={{ ml: 1 }} aria-label="Account menu">
            <Avatar sx={{ width: 36, height: 36, fontWeight: 700 }}>{initials}</Avatar>
          </IconButton>
        </Tooltip>
        <Menu
          anchorEl={anchorEl}
          open={open}
          onClose={handleClose}
          onClick={handleClose}
          PaperProps={{
            elevation: 3,
            sx: { mt: 1.5, minWidth: 180, p: 0.5 },
          }}
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        >
          <MenuItem disabled>
            <Typography variant="body2" sx={{ fontWeight: 700 }}>
              {name || 'Administrator'}
            </Typography>
          </MenuItem>
          <MenuItem disabled sx={{ py: 0.25 }}>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'capitalize' }}>
              {String(role || 'guest').toLowerCase()}
            </Typography>
          </MenuItem>
          <Divider sx={{ my: 0.5 }} />
          <MenuItem onClick={goProfile}>
            <ListItemIcon><PersonIcon fontSize="small" /></ListItemIcon>
            My Profile
          </MenuItem>
          <MenuItem onClick={doLogout}>
            <ListItemIcon><LogoutIcon fontSize="small" /></ListItemIcon>
            Logout
          </MenuItem>
        </Menu>
      </Toolbar>
    </AppBar>
  );
}
