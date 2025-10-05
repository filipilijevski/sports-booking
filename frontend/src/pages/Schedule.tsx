import { useEffect, useState } from 'react';
import {
  Box,
  Container,
  Typography,
  List,
  ListItem,
  ListItemText,
  Paper,
  Alert,
  CircularProgress,
  Button,
} from '@mui/material';
import { api } from '../lib/api';
import { logout } from '../lib/auth';
import { useNavigate } from 'react-router-dom';

interface Template {
  id: number;
  name: string;
  weekday: string;
  startTime: string;
  endTime: string;
}

export default function Schedule() {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const [hello, setHello] = useState('');
  const nav = useNavigate();

  useEffect(() => {
    api<Template[]>('/api/coach/schedule/templates')
      .then(setTemplates)
      .catch(e => setErr((e as Error).message))
      .finally(() => setLoading(false));
  }, []);

  async function testHello() {
    try {
      const text = await api<string>('/hello');
      setHello(text);
    } catch (e) {
      setHello((e as Error).message);
    }
  }

  function handleLogout() {
    logout();
    nav('/login');
  }

  return (
    <Container maxWidth="md" sx={{ mt: 6 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h4" gutterBottom>
            Schedule Templates
          </Typography>
          <Button onClick={handleLogout} color="secondary">
            Log out
          </Button>
        </Box>

        {loading && <CircularProgress />}

        {err && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {err}
          </Alert>
        )}

        {!loading && !err && templates.length === 0 && (
          <Typography>No templates yet.</Typography>
        )}

        <List>
          {templates.map(t => (
            <ListItem key={t.id} divider>
              <ListItemText
                primary={t.name}
                secondary={`${t.weekday}  ${t.startTime}-${t.endTime}`}
              />
            </ListItem>
          ))}
        </List>

        <Box mt={4}>
          <Button variant="contained" onClick={testHello}>
            Debug /hello
          </Button>
          {hello && (
            <Alert severity="info" sx={{ mt: 2 }}>
              {hello}
            </Alert>
          )}
        </Box>
      </Paper>
    </Container>
  );
}
