import { Box, Container, Paper, Typography } from '@mui/material';
import GroupsPanel from '../components/admin/GroupsPanel';

const BG = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

export default function AdminMembershipGroups() {
  return (
    <Box sx={{ width:'100%', minHeight:'100vh', background:BG, py:6 }}>
      <Container maxWidth="lg">
        <Paper sx={{ p:2, borderRadius:3, mb:2, background:'rgba(255,255,255,0.9)' }}>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            Membership Groups
          </Typography>
        </Paper>

        <GroupsPanel />
      </Container>
    </Box>
  );
}
