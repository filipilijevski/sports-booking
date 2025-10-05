import { Box, Typography, Link, Stack } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useRole } from '../context/RoleContext';

const GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

type FooterLink = { label: string; to: string };

/** Helper to chunk an array into equally sized columns */
function chunk<T>(arr: T[], size: number): T[][] {
  const out: T[][] = [];
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size));
  return out;
}

export default function Footer() {
  const { role } = useRole();
  const isAdmin = role === 'OWNER' || role === 'ADMIN';

  // Public/client link set
  const publicLinks: FooterLink[] = [
    { label: 'Home',                       to: '/' },
    { label: 'Pro Shop',                   to: '/shop' },
    { label: 'About Us',                   to: '/about' },
    { label: 'Schedule and Table Rentals', to: '/table-rentals' },
  ];

  // Admin link set 
  const adminLinks: FooterLink[] = [
    { label: 'Manage Pro Shop',              to: '/admin/shop' },
    { label: 'Enroll Users',                 to: '/admin/enroll' },
    { label: 'Manage Programs & Memberships',to: '/admin/booking' },
    { label: 'Manage Program Schedules',     to: '/admin/schedules' },
    { label: 'Admin Orders',                 to: '/admin/orders' },
    { label: 'Manage Profiles',              to: '/admin/profiles' },
    { label: 'Coaching and Memberships',     to: '/coaching' },
    { label: 'Schedule and Table Rentals',   to: '/table-rentals' },
    { label: 'Home',                         to: '/' },
  ];

  const links = isAdmin ? adminLinks : publicLinks;

  // Column shape requirements:
  // Admin: exactly 3 columns x 3 links
  // Public: 2 columns with 2 links each
  const columns = isAdmin ? chunk(links, 3) : chunk(links, Math.ceil(links.length / 2));

  // Max width for the links panel so it never pushes the layout horizontally.
  const linksMaxWidth = {
    xs: '100%',
    md: isAdmin ? 720 : 420, // tuned for readability and wrapping
  } as const;

  return (
    <Box
      component="footer"
      sx={{
        mt: 0,
        py: 4,
        paddingLeft: 0,
        width: '100%',
        color: 'common.white',
        background: GRADIENT,
        overflowX: 'hidden', // ensure no horizontal scroll is ever introduced
      }}
    >
      {/* Full-width grid. Left block pinned to the far left, links constrained on the right. */}
      <Box
        sx={{
          // No maxWidth here - contact block sits as left as possible (respecting px padding)
          display: 'grid',
          columnGap: { xs: 2, md: 4 },
          rowGap: 3,
          gridTemplateColumns: {
            xs: '1fr',          // stack on small screens
            md: 'auto 1fr',     // left fits content; right takes remaining space
          },
          alignItems: 'start',
          paddingLeft: 2
        }}
      >
        {/* Left: stacked club info - consistent sizing, pinned to left edge */}
        <Stack spacing={0.5} sx={{ minWidth: 0 }}>
          <Typography variant="body1" sx={{ fontWeight: 700 }}>
            © {new Date().getFullYear()} Sports Booking Club. All rights reserved.
          </Typography>
          <Typography variant="body1" sx={{ fontWeight: 700 }}>999-999-9999</Typography>
          <Link href="mailto:youremail@gmail.com" color="inherit" underline="hover">
            youremail@gmail.com
          </Link>
        </Stack>

        {/* Right: title + quick links (width-limited, wraps cleanly) */}
        <Box
          sx={{
            justifySelf: { xs: 'start', md: 'end' }, // place the links block to the right on md+
            width: '100%',
            maxWidth: linksMaxWidth,
            minWidth: 0,  
            px: 2          // allow content to shrink without overflow
          }}
        >

          <Box
            component="nav"
            aria-label="Footer quick links"
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: {
                xs: '1fr',                                            // stack on small screens
                sm: isAdmin ? 'repeat(3, minmax(140px, 1fr))'         // 3 columns (admin)
                           : 'repeat(2, minmax(140px, 1fr))',         // 2 columns (public)
              },
              minWidth: 0,
            }}
          >
            {columns.map((col, idx) => (
              <Stack key={idx} spacing={0.75} sx={{ minWidth: 0 }}>
                {col.map(link => (
                  <Link
                    key={link.label}
                    component={RouterLink}
                    to={link.to}
                    underline="hover"
                    color="inherit"
                    sx={{
                      fontWeight: 500,
                      lineHeight: 1.6,
                      display: 'inline-block',
                      whiteSpace: 'normal',
                      wordBreak: 'break-word', // allow multi-line wrapping for long labels
                      '&:focus-visible': {
                        outline: '2px solid rgba(255,255,255,0.8)',
                        outlineOffset: 2
                      },
                    }}
                  >
                    {link.label}
                  </Link>
                ))}
              </Stack>
            ))}
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
