import { useEffect, useMemo, useState } from 'react';
import { Box, Container, Paper, Typography, Button, Stack } from '@mui/material';
import { useSearchParams, Link } from 'react-router-dom';
import { fetchTableRentalPackages, type TableRentalPackageDto } from '../lib/booking';

const BG = 'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

export default function ThankYouTableCredits() {
  const [params] = useSearchParams();
  const [packs, setPacks] = useState<TableRentalPackageDto[]>([]);
  const [loading, setLoading] = useState(true);

  const bookingId = params.get('bookingId');
  const packageId = params.get('packageId');

  useEffect(() => {
    (async () => {
      try { setPacks((await fetchTableRentalPackages()) ?? []); }
      catch { /* non-fatal */ }
      finally { setLoading(false); }
    })();
  }, []);

  const packName = useMemo(() => {
    if (!packageId) return null;
    const id = Number(packageId);
    return packs.find(p => p.id === id)?.name ?? null;
  }, [packs, packageId]);

  return (
    <Box sx={{ minHeight: '100dvh', background: BG, py: { xs: 6, md: 10 } }}>
      <Container maxWidth="sm">
        <Paper elevation={6} sx={{ p: { xs: 3, md: 4 }, borderRadius: 3 }}>
          <Typography variant="h4" sx={{ fontWeight: 800, mb: 1 }}>
            Thank you for your purchase!
          </Typography>
          <Typography variant="body1" sx={{ mb: 2 }}>
            {packName
              ? <>Your table credit package <strong>{packName}</strong> has been added to your account.</>
              : <>Your table rental credits have been added to your account.</>}
          </Typography>
          {bookingId && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
              Reference: #{bookingId}
            </Typography>
          )}
          <Typography variant="body2" sx={{ mb: 3 }}>
            You can view your remaining credits on your profile page.
          </Typography>
          <Stack direction="row" spacing={1.5}>
            <Button variant="contained" component={Link} to="/profile">Go to My Profile</Button>
            <Button variant="outlined" component={Link} to="/table-rentals">Back to Table Rentals</Button>
          </Stack>
        </Paper>
      </Container>
    </Box>
  );
}
