import * as React from 'react';
import { Box, Grid, Paper, Stack, Typography, Skeleton } from '@mui/material';

/** Visual-only dashboard placeholders until analytics are ready */
export default function AdminOverview() {
  return (
    <Box>
      <Grid container spacing={2}>
        {[1,2,3,4].map((i) => (
          <Grid item xs={12} sm={6} lg={3} key={i}>
            <Paper elevation={3} sx={{ p: 2, borderRadius: 3 }}>
              <Typography variant="subtitle2" color="text.secondary">Metric #{i}</Typography>
              <Typography variant="h5" sx={{ fontWeight: 800, mt: 0.5 }}>
                <Skeleton width={120} />
              </Typography>
              <Skeleton height={36} />
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2} sx={{ mt: 1 }}>
        <Grid item xs={12} md={8}>
          <Paper elevation={3} sx={{ p: 2, borderRadius: 3, minHeight: 280 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1 }}>
              Sales Trend (coming soon)
            </Typography>
            <Skeleton variant="rectangular" height={220} />
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper elevation={3} sx={{ p: 2, borderRadius: 3, minHeight: 280 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1 }}>
              Activity (coming soon)
            </Typography>
            <Stack spacing={1.2}>
              {[...Array(5)].map((_, idx) => <Skeleton key={idx} height={28} />)}
            </Stack>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
