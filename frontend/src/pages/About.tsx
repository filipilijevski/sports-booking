import {
  Box,
  Container,
  Paper,
  Typography,
  Stack,
  CardMedia,
  Divider,
} from '@mui/material';


const PAGE_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

/* Reusable “pill” style - visually consistent with TableRentals price chip */
const pillSx = {
  display: 'inline-flex',
  alignItems: 'center',
  backgroundColor: 'rgba(255,255,255,0.12)',
  border: '1px solid rgba(255,255,255,0.18)',
  px: 3,
  py: 1,
  borderRadius: 999,
  color: 'common.white',
  maxWidth: '100%',
};

interface Coach {
  name:  string;
  role:  string;
  photo: string;
  bio:   string; 
}

const coaches: Coach[] = [
  { name: 'Filip',   
    role: 'High Performance Coach', 
    photo: './src/assets/filipili.webp',   
    bio: "Level 2 Provincial Coach and Instructor." 
  },
  { name: 'Maria',     
    role: 'Skills and Drills Coach', 
    photo: './src/assets/sabrinachen.webp', 
    bio: "Level 1 Certified Coach with a strong athletic background and a lifelong passion."
  },
  { name: 'Stephanie', 
    role: 'NCCP Certified Competition Introduction Coach', 
    photo: './src/assets/stephane.webp', 
    bio: "Over 30 years of competitive experience as an advanced player."
  },
  { name: 'Jules',    
    role: 'NCCP Certified Competition Introduction Coach', 
    photo: './src/assets/juliacharb.webp', 
    bio: "Over 20 years of competitive as an advanced player, playing local and international tournaments."
  },
  { name: 'Pablo',      
    role: 'Level 2 Certified Provincial Table Tennis Coach.', 
    photo: './src/assets/paulo.webp',     
    bio: "Pablo is proud to be the new owner of this Sports Club."
  },
];

/* Components   */

function NameRolePills({ name, role }: { name: string; role: string }) {
  return (
    <Stack
      direction="row"
      spacing={1.5}
      alignItems="center"
      useFlexGap
      flexWrap="wrap"
      sx={{ mb: 1, maxWidth: '100%' }}
    >
      <Box sx={pillSx} aria-label="Coach name">
        <Typography
          variant="h6"
          sx={{ fontWeight: 800, lineHeight: 1.1, overflowWrap: 'anywhere' }}
        >
          {name}
        </Typography>
      </Box>
      <Box sx={pillSx} aria-label="Coach role">
        <Typography
          variant="subtitle1"
          sx={{ fontWeight: 700, lineHeight: 1.2, overflowWrap: 'anywhere' }}
        >
          {role}
        </Typography>
      </Box>
    </Stack>
  );
}

/* Page  */

export default function About() {
  return (
    <Box sx={{ width: '100%', background: PAGE_GRADIENT, py: { xs: 6, md: 10 } }}>
      <Container maxWidth="xl">
        {/* Page title */}
        <Typography
          variant="h2"
          align="center"
          gutterBottom
          sx={{ fontWeight: 800, color: 'common.white' }}
        >
          About Our Sports Club
        </Typography>

        {/* Intro / History - styled like TableRentals cards */}
        <Paper
          elevation={3}
          sx={{
            p: { xs: 2.5, sm: 3.5 },
            borderRadius: 3,
            backgroundColor: 'rgba(0,0,0,0.65)',
            color: 'common.white',
            mb: 6,
            overflow: 'hidden',
          }}
        >
          <Box
            sx={{
              display: 'grid',
              gap: 3,
              alignItems: 'center',
              gridTemplateColumns: {
                xs: '1fr',
                md: '1fr auto', // image to the right on md+
              },
            }}
          >
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="h4" align='center' sx={{ fontWeight: 800, mb: 1 }}>
                TT Club History
              </Typography>
              <Divider sx={{ borderColor: 'rgba(255,255,255,0.18)', my: 1 }} />
              <Typography
                variant="body1"
                sx={{
                  lineHeight: 1.6,
                  whiteSpace: 'pre-line',
                  opacity: 1,
                  wordBreak: 'break-word',
                  overflowWrap: 'anywhere',
                }}
              >
                {`\n Insert bio for your club here.`}
              </Typography>
            </Box>

            <Box sx={{ justifySelf: { md: 'end' }, minWidth: 0, maxWidth: '100%' }}>
              <CardMedia
                component="img"
                image="./src/assets/marianndomonkos.webp"
                alt="Sports-Club history"
                sx={{
                  width: { xs: '100%', sm: 420 },
                  height: { xs: 240, sm: 260 },
                  objectFit: 'cover',
                  borderRadius: 2,
                }}
              />
            </Box>
          </Box>
        </Paper>

        {/* Coaches */}
        <Typography
          variant="h3"
          align="center"
          gutterBottom
          sx={{ fontWeight: 800, color: 'common.white', mb: 3 }}
        >
          Meet Our Coaches
        </Typography>

        <Stack spacing={4}>
          {coaches.map((c) => (
            <Paper
              key={c.name}
              elevation={3}
              sx={{
                p: { xs: 2.5, sm: 3.5 },
                borderRadius: 3,
                backgroundColor: 'rgba(0,0,0,0.65)',
                color: 'common.white',
                overflow: 'hidden',
              }}
            >
              <Box
                sx={{
                  display: 'grid',
                  gap: 3,
                  alignItems: 'flex-start',
                  gridTemplateColumns: {
                    xs: '1fr',
                    md: 'minmax(250px, 250px) 1fr',  // image stays on the left; text column can grow
                    xl: 'minmax(250px, 250px) 1fr',
                  },
                }}
              >
                {/* Photo (fixed, responsive) */}
                <Box
                  sx={{
                    justifySelf: { md: 'start' },
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    maxWidth: '100%',
                  }}
                >
                  <CardMedia
                    component="img"
                    image={c.photo}
                    alt={c.name}
                    sx={{
                      width: { xs: 240, sm: 260, md: '100%', xl: '100%' },
                      height: { xs: 240, sm: 260, md: 320, xl: 360 },
                      objectFit: 'cover',
                      borderRadius: 2,
                      borderColor: 'rgba(255,255,255,0.98)'
                    }}
                  />
                </Box>

                {/* Content (guaranteed to stay within the card) */}
                <Box sx={{ minWidth: 0, maxWidth: '100%' }}>
                  <NameRolePills name={c.name} role={c.role} />
                  <Divider sx={{ borderColor: 'rgba(255,255,255,0.18)', my: 1 }} />
                  <Typography
                    variant="body1"
                    sx={{
                      lineHeight: 1.6,
                      whiteSpace: 'pre-line',
                      opacity: 1,
                      wordBreak: 'break-word',
                      overflowWrap: 'anywhere',
                      fontWeight: 500
                    }}
                  >
                    {c.bio}
                  </Typography>
                </Box>
              </Box>
            </Paper>
          ))}
        </Stack>
      </Container>
    </Box>
  );
}
