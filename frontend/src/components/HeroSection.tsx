import { Box, Button, Container, Typography } from '@mui/material';
import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

export default function HeroSection() {
  const nav = useNavigate();

  return (
    <Box
      sx={{
        minHeight: '70vh',
        width: '100%',
        background:
          'linear-gradient(270deg, rgba(230, 134, 86, 0.45), rgba(7, 97, 119, 0.49)), url(/malong_heroimage.jpg)',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        display: 'flex',
        alignItems: 'center',
        color: '#fff',
      }}
    >
      <Container maxWidth={false} sx={{ px: { xs: 3, md: 8 } }}>
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9 }}
        >
          <Typography variant="h2" component="h1" gutterBottom>
            Welcome to the Newest and Biggest Sports Club
          </Typography>

          <Typography variant="h6" sx={{ mb: 4, maxWidth: 700 }}>
            Coaching, equipment and community for players of all levels.
          </Typography>

          {/* brand-coloured, filled buttons */}
          <Button
            variant="contained"
            size="large"
            sx={{ mr: 3, backgroundColor: '#702d74ff', ':hover': { backgroundColor: '#F49F0A' } }}
            onClick={() => nav('/coaching')}
          >
            Book a Lesson
          </Button>
          <Button
            variant="contained"
            size="large"
            sx={{ backgroundColor: '#702d74ff', ':hover': { backgroundColor: '#F49F0A' } }}
            onClick={() => nav('/shop')}
          >
            Shop Gear
          </Button>
        </motion.div>
      </Container>
    </Box>
  );
}
