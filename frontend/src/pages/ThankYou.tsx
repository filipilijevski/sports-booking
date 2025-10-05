import { Box, Container, Typography, Button } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

/* same gradient used everywhere for visual continuity */
const BG_GRADIENT =
  'linear-gradient(270deg,rgba(0,54,126,1) 0%,rgba(181,94,231,1) 100%)';

export default function ThankYou() {
  return (
    <Box sx={{ width:'100%', minHeight:'100vh', background:BG_GRADIENT, pt:10, pb:10 }}>
      <Container sx={{ textAlign:'center' }}>
        <Typography variant="h1" align="center" color="common.white" gutterBottom>
          Pro Shop
        </Typography>
        <Typography variant="h6" align="center" color="common.white" sx={{ mb: 4 }}>
          Hand-picked gear to unleash your best game. Curated by our national-level coaches.
        </Typography>

        <Typography variant="h3" gutterBottom color="common.white">
          Thank You!
        </Typography>

        <Typography variant="h6" sx={{ mb: 4 }} color="common.white">
          Your payment was successful and we're packing your order.
        </Typography>

        <Button variant="contained" component={RouterLink} to="/shop">
          Continue Shopping
        </Button>
      </Container>
    </Box>
  );
}
