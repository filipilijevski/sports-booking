import { Box, Typography } from '@mui/material';
import AdminCouponsPanel from '../components/AdminCouponsPanel';

export default function AdminCoupons() {
  return (
    <Box>
      <Typography variant="h3" align="center" sx={{ fontWeight: 700, py: 3, color: 'common.white' }}>
        Manage Coupons
      </Typography>
      <AdminCouponsPanel />
    </Box>
  );
}
