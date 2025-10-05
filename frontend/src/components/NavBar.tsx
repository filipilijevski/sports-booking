import {
  AppBar, Toolbar, IconButton, Drawer, List, ListItemButton, ListItemText,
  Box, Avatar, Divider, Badge, Typography,
} from '@mui/material';
import MenuIcon         from '@mui/icons-material/Menu';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import { useState, useEffect } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useCart }  from '../context/CartContext';
import { useRole }  from '../context/RoleContext';
import Logo         from '../assets/Logo.png';

const BAR_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

interface Props {
  authed:       boolean;
  firstName?:   string | null;
  onLogout:     () => void;
  onLoginClick: () => void;
}

export default function NavBar({
  authed, firstName: propName, onLogout, onLoginClick,
}: Props) {
  const [drawer, setDrawer] = useState(false);
  const [name,   setName]   = useState<string | null>(propName ?? null);

  const { state: { totalCount }, openDrawer } = useCart();
  const { role } = useRole();
  const nav      = useNavigate();

  const isAdmin  = role === 'OWNER' || role === 'ADMIN';
  const isClient = role === 'CLIENT';

  /* keep greeting in-sync */
  useEffect(() => {
    setName(propName ?? null);
    const cb = (e: StorageEvent) => {
      if (e.key === 'firstName')                   setName(e.newValue);
      if (e.key === 'accessToken' && !e.newValue) setName(null);
    };
    window.addEventListener('storage', cb);
    return () => window.removeEventListener('storage', cb);
  }, [propName]);

  /* menu entries to action */
  const trigger = (txt: string) => {
    setDrawer(false);
    switch (txt) {
      case 'Home':                                 nav('/');                         break;
      case 'News and Events':                      nav('/news');                     break;
      case 'Pro Shop':                             nav('/shop');                     break;
      case 'Manage Pro Shop':                      nav('/admin/shop');               break;
      case 'Manual In-Person Checkout':            nav('/admin/manual-shop');        break;  
      case 'Manage Blog Posts':                    nav('/admin/blog');               break;
      case 'Manage Profiles':                      nav('/admin/profiles');           break;
      case 'Enroll Users':                         nav('/admin/enroll');             break;
      case 'Manage Programs & Memberships':        nav('/admin/booking');            break;
      case 'Manage Program Schedules':             nav('/admin/schedules');          break;
      case 'Admin Orders':                         nav('/admin/orders');             break;
      case 'My Order History':                     nav('/my-orders');                break;
      case 'Coaching and Memberships':             nav('/coaching');                 break;
      case 'About Us':                             nav('/about');                    break;
      case 'Schedule and Table Rentals':           nav('/table-rentals');            break;
      case 'My Profile':                           nav('/profile');                  break;
      case 'Login / Register':                     onLoginClick();                   break;
      case 'Log out':                              onLogout(); nav('/');             break;
      default: /* no-op */ ;
    }
  };

  /* menu construction */
  const baseClient = [
    'Home',
    'Coaching and Memberships',
    'News and Events',
    isAdmin ? 'Manage Pro Shop' : 'Pro Shop',
    ...(isClient && authed ? ['My Order History'] : []),
    'About Us',
    'Schedule and Table Rentals',
  ];
  const adminExtrasOld = isAdmin
    ? [
        'Admin Orders',
        'Manage Profiles',  
        'Enroll Users',
        'Manage Programs & Memberships',
        'Manage Program Schedules',
        'Manage Blog Posts',
      ]
    : [];
  const extra = authed ? ['My Profile', 'Log out'] : ['Login / Register'];

  // Admins: exact order requested + new Manual Checkout page
  const adminOrdered = [
    'Home',
    'Coaching and Memberships',
    'News and Events',
    'About Us',
    'Schedule and Table Rentals',
    'Admin Orders',
    'Manage Pro Shop',
    'Manual In-Person Checkout',   
    'Manage Programs & Memberships',
    'Manage Program Schedules',
    'Manage Blog Posts',
    'Manage Profiles',
    'Enroll Users',
    'My Profile',
    'Log out',
  ];

  const menu = isAdmin ? adminOrdered : [...baseClient, ...adminExtrasOld, ...extra];

  return (
    <>
      <AppBar
        position="relative"
        elevation={0}
        sx={{
          background: BAR_GRADIENT,
          left: 0,
          right: 0,
          width: '100%',
          maxWidth: '100vw',
          overflowX: 'hidden',
        }}
      >
        <Toolbar
          disableGutters
          sx={{
            width: '100%',
            boxSizing: 'border-box',
            px: { xs: 2, md: 4 },
            display: 'flex',
            alignItems: 'center',
            minHeight: { xs: 64, md: 80 },
          }}
        >
          {/* Leftmost: Logo */}
          <Box
            component={RouterLink}
            to="/"
            sx={{ display: 'flex', alignItems: 'center', minWidth: 0 }}
          >
            <Avatar
              src={Logo}
              variant="rounded"
              sx={{
                width:  { xs: 120, sm: 140, md: 150 },
                height: { xs: 80,  sm: 93,  md: 100 },
                borderRadius: 3,
              }}
            />
          </Box>

          <Box sx={{ flexGrow: 1 }} />

          {/* Rightmost: Cart then Menu */}
          <IconButton sx={{ mr: 1, color: '#fff' }} onClick={openDrawer} aria-label="Open cart">
            <Badge badgeContent={totalCount} color="error">
              <ShoppingCartIcon />
            </Badge>
          </IconButton>

          <IconButton sx={{ color: '#fff' }} onClick={() => setDrawer(true)} aria-label="Open menu">
            <MenuIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Drawer anchor="right" open={drawer} onClose={() => setDrawer(false)}>
        <Box sx={{ width: 260, pt: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
          <List sx={{ flexGrow: 1 }}>
            {menu.map(txt => (
              <ListItemButton key={txt} onClick={() => trigger(txt)}>
                <ListItemText primary={txt} />
              </ListItemButton>
            ))}
          </List>

          {authed && name && (
            <>
              <Divider />
              <Box sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="caption" color="text.secondary" display="block">
                  {role}
                </Typography>
                <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                  Hello {name}
                </Typography>
              </Box>
            </>
          )}
        </Box>
      </Drawer>
    </>
  );
}
