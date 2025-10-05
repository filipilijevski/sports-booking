import { useEffect, useMemo, useState } from 'react';
import { Box, Container, Paper, Typography, Button, Stack } from '@mui/material';
import { useSearchParams, Link } from 'react-router-dom';
import { fetchPublicMembershipPlans, type MembershipPlanDto } from '../lib/booking';

const BG = 'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

export default function ThankYouMembership() {
  const [params] = useSearchParams();
  const [plans, setPlans] = useState<MembershipPlanDto[]>([]);
  const [loading, setLoading] = useState(true);

  const bookingId = params.get('bookingId');
  const planId    = params.get('planId');

  useEffect(() => {
    (async () => {
      try { setPlans((await fetchPublicMembershipPlans()) ?? []); }
      catch { /* non-fatal */ }
      finally { setLoading(false); }
    })();
  }, []);

  const planName = useMemo(() => {
    if (!planId) return null;
    const id = Number(planId);
    return plans.find(p => p.id === id)?.name ?? null;
  }, [plans, planId]);

  return (
    <Box sx={{ minHeight: '100dvh', background: BG, py: { xs: 6, md: 10 } }}>
      <Container maxWidth="sm">
        <Paper elevation={6} sx={{ p: { xs: 3, md: 4 }, borderRadius: 3 }}>
          <Typography variant="h4" sx={{ fontWeight: 800, mb: 1 }}>
            Thank you for your purchase!
          </Typography>
          <Typography variant="body1" sx={{ mb: 2 }}>
            {planName
              ? <>Your <strong>{planName}</strong> membership is now active.</>
              : <>Your membership is now active.</>}
          </Typography>
          {bookingId && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
              Reference: #{bookingId}
            </Typography>
          )}
          <Typography variant="body2" sx={{ mb: 3 }}>
            You can review your membership details any time on your profile page.
          </Typography>
          <Stack direction="row" spacing={1.5}>
            <Button variant="contained" component={Link} to="/profile">Go to My Profile</Button>
            <Button variant="outlined" component={Link} to="/coaching">Browse Programs</Button>
          </Stack>
        </Paper>
      </Container>
    </Box>
  );
}
