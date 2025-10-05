import { useEffect, useMemo, useState } from 'react';
import { Box, Container, Paper, Typography, Button, Stack } from '@mui/material';
import { useSearchParams, Link } from 'react-router-dom';
import { fetchPublicPrograms, type ProgramCardDto } from '../lib/booking';

const BG = 'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

export default function ThankYouProgram() {
  const [params] = useSearchParams();
  const [programs, setPrograms] = useState<ProgramCardDto[]>([]);
  const [loading, setLoading] = useState(true);

  const bookingId = params.get('bookingId');
  const programId = params.get('programId');
  const packageId = params.get('packageId');

  useEffect(() => {
    (async () => {
      try { setPrograms((await fetchPublicPrograms()) ?? []); }
      catch { /* non-fatal */ }
      finally { setLoading(false); }
    })();
  }, []);

  const { programName, packageName } = useMemo(() => {
    const id = Number(programId);
    const pid = Number(packageId);
    const program = programs.find(p => p.id === id);
    const pkg = program?.packages.find(pk => pk.id === pid);
    return {
      programName: program?.title ?? null,
      packageName: pkg?.name ?? null,
    };
  }, [programs, programId, packageId]);

  return (
    <Box sx={{ minHeight: '100dvh', background: BG, py: { xs: 6, md: 10 } }}>
      <Container maxWidth="sm">
        <Paper elevation={6} sx={{ p: { xs: 3, md: 4 }, borderRadius: 3 }}>
          <Typography variant="h4" sx={{ fontWeight: 800, mb: 1 }}>
            Thank you for your enrollment!
          </Typography>
          <Typography variant="body1" sx={{ mb: 2 }}>
            {programName
              ? <>Your enrollment in <strong>{programName}</strong>{packageName ? <> ({packageName})</> : null} is confirmed.</>
              : <>Your program enrollment is confirmed.</>}
          </Typography>
          {bookingId && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
              Reference: #{bookingId}
            </Typography>
          )}
          <Typography variant="body2" sx={{ mb: 3 }}>
            You can review your enrollments any time on your profile page.
          </Typography>
          <Stack direction="row" spacing={1.5}>
            <Button variant="contained" component={Link} to="/profile">Go to My Profile</Button>
            <Button variant="outlined" component={Link} to="/coaching">Browse More Programs</Button>
          </Stack>
        </Paper>
      </Container>
    </Box>
  );
}
