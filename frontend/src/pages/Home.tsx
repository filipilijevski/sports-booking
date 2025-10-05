import HeroSection from '../components/HeroSection';
import {
  Box,
  Container,
  Paper,
  Typography,
  Stack,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';

const INFO_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

/* marketing cards */
const cards = [
  {
    title: 'World-class Coaching',
    img:   './src/assets/tabletennismalong.jpg',
    text:  'From fundamentals to elite tactics, our certified coaches tailor sessions to your goals.',
    href:  '/coaching',
  },
  {
    title: 'Premium Pro Shop',
    img:   './src/assets/proshopimage.jpg',
    text:  'Hand-picked equipment. Try before you buy at our in-house lab.',
    href:  '/shop',
  },
  {
    title: 'Community & Leagues',
    img:   './src/assets/tabletennisantoine.jpg',
    text:  'Weekly round-robins, sanctioned tournaments and social events for every age group.',
    href:  '/about',
  },
  {
    title: 'Something For Everybody',
    img:   './src/assets/adultacademy.jpeg',
    text:  "Whether you're chasing rankings or just having fun with friends, we've got you covered!",
    href:  '/about',
  },
];

/* hours data */
const HOURS: { day: string; hours: string }[] = [
  { day: 'Monday',    hours: '9:00am - 9:00pm' },
  { day: 'Tuesday',   hours: '9:00am - 9:00pm' },
  { day: 'Wednesday', hours: '9:00am - 9:00pm' },
  { day: 'Thursday',  hours: '9:00am - 9:00pm' },
  { day: 'Friday',    hours: '9:00am - 9:00pm' },
  { day: 'Saturday',  hours: '9:00am - 9:00pm' },
  { day: 'Sunday',    hours: '9:00am - 9:00pm' },
];

export default function Home() {
  const nav = useNavigate();

  return (
    <>
      <HeroSection />

      <Box sx={{ width: '100%', background: INFO_GRADIENT, py: 8 }}>
        {/* Use xl so four cards comfortably fit; columns are driven by breakpoints below */}
        <Container maxWidth="xl">
          {/* CSS Grid: 1 col on phones, 2 cols >=600px (sm), 4 cols >=1200px (lg) */}
          <Box
            sx={{
              display: 'grid',
              gap: 3,
              gridTemplateColumns: {
                xs: '1fr',
                sm: 'repeat(2, minmax(0, 1fr))',
                lg: 'repeat(4, minmax(0, 1fr))',
              },
              alignItems: 'stretch',
            }}
          >
            {cards.map(({ title, img, text, href }) => (
              <Paper
                key={title}
                elevation={3}
                onClick={() => nav(href)}
                sx={{
                  p: 4,
                  display: 'flex',
                  flexDirection: 'column',
                  textAlign: 'center',
                  backgroundColor: 'rgba(0,0,0,0.65)',
                  color: 'common.white',
                  borderRadius: 3,
                  cursor: 'pointer',
                  transition: 'box-shadow .2s ease, transform .2s ease',
                  '&:hover': { boxShadow: 10, transform: 'translateY(-2px)' },
                }}
              >
                <Box
                  component="img"
                  src={img}
                  alt={title}
                  loading="lazy"
                  sx={{
                    width: '100%',
                    height: 180,
                    objectFit: 'cover',
                    borderRadius: 3,
                    mb: 2,
                  }}
                />
                <Typography variant="h5" gutterBottom>
                  {title}
                </Typography>
                <Typography sx={{ flexGrow: 1 }}>{text}</Typography>
              </Paper>
            ))}
          </Box>
        </Container>
      </Box>

      {/* About section */}
      <Box sx={{ width: '100%', background: INFO_GRADIENT, py: { xs: 2, md: 4 } }}>
        <Container>
          <Typography
            variant="h3"
            align="center"
            gutterBottom
            sx={{ fontWeight: 700, color: 'common.white' }}
          >
            About Our Sports Club
          </Typography>

          <Paper
            elevation={3}
            sx={{
              p: { xs: 2, sm: 3, md: 4 },
              mt: 2,
              borderRadius: 3,
              backgroundColor: 'rgba(0,0,0,0.65)',
              color: 'common.white',
            }}
          >
            {/* Two fixed columns on sm+; single column on xs */}
            <Box
              sx={{
                display: 'grid',
                gap: { xs: 3, md: 6 },
                alignItems: 'center',
                gridTemplateColumns: {
                  xs: '1fr',         // stacked on phones
                  sm: '5fr 7fr',     // image left, text right on sm+
                },
              }}
            >
              {/* Left column: logo */}
              <Box sx={{ display: 'flex', justifyContent: { xs: 'center', sm: 'flex-start' } }}>
                <Box
                  component="img"
                  src="./src/assets/Logo.png"   // replace with final logo path when ready
                  alt="TT Club logo"
                  loading="lazy"
                  sx={{
                    width: '100%',
                    maxWidth: 320,
                    height: 'auto',
                    borderRadius: 2,
                    boxShadow: 2,
                    backgroundColor: 'white',
                    p: 1.5,
                    objectFit: 'contain',
                  }}
                />
              </Box>

              {/* Right column: mission */}
              <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                <Typography variant="h5" gutterBottom sx={{ fontWeight: 700 }}>
                  Our Mission
                </Typography>
                <Typography variant="body1" sx={{ lineHeight: 1.8 }}>
                  Your mission here.
                </Typography>
              </Box>
            </Box>
          </Paper>
        </Container>
      </Box>

      {/* Hours of operation */}
      <Box sx={{ width: '100%', background: INFO_GRADIENT, pb: { xs: 4, md: 8 } }}>
        <Container>
          <Typography
            variant="h4"
            align="center"
            gutterBottom
            sx={{ fontWeight: 700, color: 'common.white' }}
          >
            Hours of Operation
          </Typography>

          <Paper
            elevation={3}
            sx={{
              maxWidth: 640,
              mx: 'auto',
              mt: 2,
              p: { xs: 2, sm: 3 },
              borderRadius: 3,
              backgroundColor: 'rgba(0,0,0,0.65)',
              color: 'common.white',
            }}
          >
            <Stack spacing={1}>
              {HOURS.map(({ day, hours }, idx) => (
                <Stack
                  key={day}
                  direction="row"
                  justifyContent="space-between"
                  alignItems="center"
                  sx={{
                    py: 1,
                    borderBottom:
                      idx < HOURS.length - 1 ? '1px solid rgba(255,255,255,0.12)' : 'none',
                  }}
                >
                  <Typography sx={{ fontWeight: 600 }}>{day}</Typography>
                  <Typography>{hours}</Typography>
                </Stack>
              ))}
            </Stack>
          </Paper>
        </Container>
      </Box>

      {/* Map and location */}
      <Box sx={{ width: '100%', background: INFO_GRADIENT, pb: { xs: 8, md: 12 } }}>
        <Container>
          <Typography
            variant="h4"
            align="center"
            gutterBottom
            sx={{ fontWeight: 700, color: 'common.white' }}
          >
            Find Us
          </Typography>
          <Typography align="center" sx={{ mb: 2, color: 'common.white' }}>
            260 Somerset Street West, Ottawa, ON K2P 0J5, Canada
          </Typography>

          <Paper
            elevation={3}
            sx={{
              borderRadius: 3,
              overflow: 'hidden',
              backgroundColor: 'rgba(0,0,0,0.65)',
            }}
          >
            {/* Responsive 16:9 map */}
            <Box sx={{ position: 'relative', pt: '56.25%' }}>
              <iframe
                title="Sports Club Location"
                src={`https://www.google.com/maps?q=${encodeURIComponent(
                  '260 Somerset Street West, Ottawa, ON K2P 0J5, Canada'
                )}&output=embed`}
                style={{
                  position: 'absolute',
                  inset: 0,
                  border: 0,
                  width: '100%',
                  height: '100%',
                }}
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
                allowFullScreen
              />
            </Box>
          </Paper>
        </Container>
      </Box>
    </>
  );
}
