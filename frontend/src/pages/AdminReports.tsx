import * as React from 'react';
import { Box, Paper, Typography } from '@mui/material';

export default function AdminReports() {
  return (
    <Box>
      <Paper elevation={3} sx={{ p: 3, borderRadius: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 800, mb: 1 }}>
          Reports
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Analytics & export tools will appear here later. This placeholder keeps the navigation visually complete.
        </Typography>
      </Paper>
    </Box>
  );
}
