import { useEffect, useMemo, useState, Suspense } from 'react';
import {
  Box, Container, Paper, Stack, Typography, Button, IconButton,
  CircularProgress, Alert, Chip, FormControlLabel, Checkbox, Tooltip, Dialog,
  DialogTitle, DialogContent, DialogActions, Divider, TextField, MenuItem, Select, InputLabel, FormControl, List, ListItem, ListItemText
} from '@mui/material';
import { alpha, styled } from '@mui/material/styles';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import TodayIcon from '@mui/icons-material/Today';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { useRole } from '../context/RoleContext';
import {
  adminListPrograms,
  fetchPublicOccurrences,
  type ProgramDto,
  type ProgramOccurrenceDto,
} from '../lib/booking';
import {
  adminFetchAttendance,
  adminMarkAttendance,
  type AttendanceEligibleUser,
  type AttendanceListResp,
} from '../lib/attendance';
import AdminTableCreditsPane from '../components/AdminTableCreditsPane';


/* Styling / Theme alignment */
const BG = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

/* Visible window (saves vertical space) */
const VISIBLE_START_HOUR = 6;   // 6am
const VISIBLE_END_HOUR   = 23;  // 11pm
const VISIBLE_HOURS      = VISIBLE_END_HOUR - VISIBLE_START_HOUR; // 17 hours

/* Layout constants */
const PX_PER_HOUR = 48; // height per hour block
const COLUMN_HEIGHT = PX_PER_HOUR * VISIBLE_HOURS;
const HOURS_TICKS = Array.from({ length: VISIBLE_HOURS + 1 }, (_, i) => VISIBLE_START_HOUR + i);
const DAY_NAMES = ['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'];

function AccessDenied() {
  return (
    <Box sx={{ minHeight: '100dvh', background: BG, display: 'flex', alignItems: 'center' }}>
      <Container>
        <Paper sx={{ p: 4, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.95)' }}>
          <Typography variant="h5" sx={{ fontWeight: 700, mb: 1 }}>Access denied</Typography>
          <Typography>You must be signed in with an ADMIN or OWNER account to view this page.</Typography>
        </Paper>
      </Container>
    </Box>
  );
}

/* helpers */
function startOfWeek(d: Date): Date {
  const day = d.getDay(); // 0=Sun...6=Sat
  const mondayDelta = (day === 0 ? -6 : 1 - day); // shift to Monday
  const res = new Date(d);
  res.setHours(0,0,0,0);
  res.setDate(res.getDate() + mondayDelta);
  return res;
}

function addDays(d: Date, n: number): Date {
  const res = new Date(d);
  res.setDate(res.getDate() + n);
  return res;
}

function fmtTime(dt: Date): string {
  return new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(dt);
}

function fmtRange(s: Date, e: Date): string {
  return `${fmtTime(s)}-${fmtTime(e)}`;
}

/** Convert a Date to Y-offset in the visible window (6:00 -> 0px). */
function yFromDate(d: Date) {
  const h = d.getHours() + d.getMinutes() / 60;
  return (h - VISIBLE_START_HOUR) * PX_PER_HOUR;
}

function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

function clamp(n: number, min: number, max: number) { return Math.max(min, Math.min(max, n)); }

/** Deterministic color per program id */
function programColor(id: number | null | undefined): string {
  const palette = [
    '#4F46E5', '#0EA5E9', '#10B981', '#F59E0B', '#EF4444',
    '#8B5CF6', '#14B8A6', '#DC2626', '#2563EB', '#D946EF',
    '#502f9cff', '#108f80ff', '#966a6aff', '#0a2664a6', '#c918e4ff',
    '#10ca0aff', '#7a9155ff', '#0051ffff', '#00a00dff', '#782485ff',
  ];
  const k = typeof id === 'number' ? id : 0;
  return palette[Math.abs(k) % palette.length];
}

type CalendarEvent = {
  id: string;            // synthetic key
  occurrenceId?: number; // real backend ID when available
  programId: number | null;
  title: string;
  coach: string | null | undefined;
  start: Date;           // local time
  end: Date;             // local time
  active: boolean;       // program active?
};

/** Lay out a day's events with simple overlap columns. */
function layoutDay(events: CalendarEvent[]) {
  const sorted = [...events].sort((a, b) => a.start.getTime() - b.start.getTime());
  type Item = CalendarEvent & { col?: number; cols?: number };
  const items: Item[] = sorted.map(e => ({ ...e }));
  let cluster: Item[] = [];
  let clusterEnd = -1;

  const flushCluster = (list: Item[]) => {
    if (!list.length) return;
    const colsEnd: number[] = [];
    for (const it of list) {
      let placed = false;
      for (let c = 0; c < colsEnd.length; c++) {
        if (colsEnd[c] <= it.start.getTime()) {
          it.col = c;
          colsEnd[c] = it.end.getTime();
          placed = true;
          break;
        }
      }
      if (!placed) {
        it.col = colsEnd.length;
        colsEnd.push(it.end.getTime());
      }
    }
    const totalCols = colsEnd.length || 1;
    for (const it of list) it.cols = totalCols;
  };

  for (const it of items) {
    const st = it.start.getTime();
    if (cluster.length === 0 || st < clusterEnd) {
      cluster.push(it);
      clusterEnd = Math.max(clusterEnd, it.end.getTime());
    } else {
      flushCluster(cluster);
      cluster = [it];
      clusterEnd = it.end.getTime();
    }
  }
  flushCluster(cluster);

  return items.map(it => {
    const top = yFromDate(it.start);
    const bottom = yFromDate(it.end);
    const rawHeight = bottom - top;
    const height = Math.max(18, rawHeight);
    const col = it.col ?? 0;
    const cols = it.cols ?? 1;

    const gapX = 4; // px
    const widthPct = 100 / cols;
    const leftPct = widthPct * col;
    return { ...it, top, height, leftPct, widthPct, gapX };
  });
}

const Column = styled('div')(({ theme }) => ({
  position: 'relative',
  height: COLUMN_HEIGHT,
  borderLeft: `1px solid ${alpha(theme.palette.common.black, 0.12)}`,
}));

const HourLine = styled('div')(({ theme }) => ({
  position: 'absolute',
  left: 0,
  right: 0,
  height: 1,
  background: alpha(theme.palette.common.black, 0.08),
}));

/* Page */

export default function ManageProgramSchedules() {
  const { role } = useRole();
  const authedAdmin = role === 'OWNER' || role === 'ADMIN';
  if (!authedAdmin) return <AccessDenied />;

  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);

  const [anchor, setAnchor]   = useState<Date>(() => startOfWeek(new Date()));
  const [showInactive, setShowInactive] = useState(true);
  const [coachFilter, setCoachFilter] = useState('');
  const [programFilter, setProgramFilter] = useState<number | 'ALL'>('ALL');

  const [programs, setPrograms] = useState<ProgramDto[]>([]);
  const [events, setEvents]     = useState<CalendarEvent[]>([]);

  const [selected, setSelected] = useState<CalendarEvent | null>(null);

  // Attendance modal state
  const [attLoading, setAttLoading] = useState(false);
  const [attError, setAttError] = useState<string | null>(null);
  const [attUsers, setAttUsers] = useState<AttendanceEligibleUser[]>([]);
  const [attSearch, setAttSearch] = useState('');

  const weekStart = useMemo(() => startOfWeek(anchor), [anchor]);
  const weekDays  = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)), [weekStart]);
  const weekLabel = useMemo(() => {
    const end = addDays(weekStart, 6);
    const fmt = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
    return `${fmt.format(weekStart)} - ${fmt.format(end)}`;
  }, [weekStart]);

  const programMap = useMemo(() => {
    const m = new Map<number, ProgramDto>();
    programs.forEach(p => m.set(p.id, p));
    return m;
  }, [programs]);

  // pull data for +/- 12 weeks once
  useEffect(() => {
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const now = new Date();
        const from = new Date(now); from.setDate(from.getDate() - 7 * 12);
        const to   = new Date(now); to.setDate(to.getDate() + 7 * 12);

        const toIsoDate = (d: Date) => d.toISOString().slice(0, 10); // 'YYYY-MM-DD'

        const [occ, progs] = await Promise.all([
          fetchPublicOccurrences(toIsoDate(from), toIsoDate(to)).then(r => r ?? []),
          adminListPrograms().then(r => r ?? []),
        ]);

        setPrograms(progs);

        const mapped: CalendarEvent[] = (occ as ProgramOccurrenceDto[]).map((o, idx) => {
          const start = new Date(o.start);
          const end   = new Date(o.end);
          const active = typeof o.programId === 'number'
            ? (progs.find(pp => pp.id === o.programId)?.active ?? true)
            : true;

          // id is new server field - cast via any to avoid typing churn
          const occurrenceId = (o as any)?.id as number | undefined;

          return {
            id: `${o.programId ?? 'na'}-${idx}-${o.start}`,
            occurrenceId,
            programId: o.programId ?? null,
            title: o.title ?? 'Program',
            coach: o.coachName,
            start, end, active,
          };
        });

        setEvents(mapped);
      } catch (e: any) {
        setError(e?.message ?? 'Failed to load schedule.');
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Filters for current week
  const weekEventsByDay = useMemo(() => {
    const map: Record<number, CalendarEvent[]> = { 0:[],1:[],2:[],3:[],4:[],5:[],6:[] };
    const matchesCoach = (ev: CalendarEvent) =>
      !coachFilter.trim() || (ev.coach ?? '').toLowerCase().includes(coachFilter.trim().toLowerCase());
    const matchesProgram = (ev: CalendarEvent) =>
      programFilter === 'ALL' || ev.programId === programFilter;

    for (const ev of events) {
      if (!showInactive && !ev.active) continue;
      if (!matchesCoach(ev)) continue;
      if (!matchesProgram(ev)) continue;

      for (let i = 0; i < 7; i++) {
        const dayDate = weekDays[i];
        if (sameDay(ev.start, dayDate)) {
          map[i].push(ev);
          break;
        }
      }
    }
    return map;
  }, [events, weekDays, showInactive, coachFilter, programFilter]);

  // Attendance modal helpers
  const loadAttendance = async (ev: CalendarEvent) => {
    if (!ev) return;
    setAttLoading(true); setAttError(null);
    try {
      // Prefer occurrenceId (new backend). Fallback to program/date.
      let resp: AttendanceListResp | undefined;
      if (ev.occurrenceId) {
        resp = await adminFetchAttendance({ occurrenceId: ev.occurrenceId }) as AttendanceListResp;
      } else if (ev.programId) {
        const d = ev.start.toISOString().slice(0,10);
        resp = await adminFetchAttendance({ programId: ev.programId, date: d }) as AttendanceListResp;
      }
      setAttUsers(resp?.users ?? []);
    } catch (e: any) {
      setAttError(e?.message ?? 'Failed to load attendance.');
    } finally {
      setAttLoading(false);
    }
  };

  const markToggle = async (user: AttendanceEligibleUser, present: boolean) => {
    if (!selected?.occurrenceId) return;
    // simple confirm to prevent accidental consumption
    const action = present ? 'consume 1 session' : 'undo';
    if (!window.confirm(`Are you sure you want to ${action} for ${user.name ?? user.email}?`)) return;

    try {
      await adminMarkAttendance({ occurrenceId: selected.occurrenceId, userId: user.userId, present });
      // optimistic local update
      setAttUsers(prev => prev.map(u => {
        if (u.userId !== user.userId) return u;
        if (present) {
          const nextRemaining = Math.max(0, (u.sessionsRemaining ?? 0) - 1);
          return { ...u, present: true, sessionsRemaining: nextRemaining };
        } else {
          return { ...u, present: false, sessionsRemaining: (u.sessionsRemaining ?? 0) + 1 };
        }
      }));
    } catch (e: any) {
      alert(e?.message ?? 'Failed to update attendance.');
    }
  };

  const filteredAttUsers = useMemo(() => {
    const q = attSearch.trim().toLowerCase();
    if (!q) return attUsers;
    return attUsers.filter(u =>
      (u.name ?? '').toLowerCase().includes(q) ||
      (u.email ?? '').toLowerCase().includes(q)
    );
  }, [attUsers, attSearch]);

  return (
    <Box sx={{ width:'100%', minHeight:'100vh', background:BG, py:6 }}>
      <Container maxWidth="xl">
        <Typography variant="h3" align="center" sx={{ fontWeight: 700, color: 'common.white', py: 3 }}>Programs, Rentals & Attendance</Typography>
        <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.9)', mb:2 }}>
          <Stack direction={{ xs:'column', md:'row' }} alignItems={{ md:'center' }} spacing={2}>
            <Typography variant="h5" sx={{ fontWeight: 800, flex: 1 }}>
              Program Calendar
            </Typography>

            <Stack direction="row" spacing={1} alignItems="center">
              <Tooltip title="Previous week">
                <IconButton onClick={() => setAnchor(addDays(weekStart, -7))}><ChevronLeftIcon /></IconButton>
              </Tooltip>
              <Tooltip title="This week">
                <IconButton onClick={() => setAnchor(startOfWeek(new Date()))}><TodayIcon /></IconButton>
              </Tooltip>
              <Tooltip title="Next week">
                <IconButton onClick={() => setAnchor(addDays(weekStart, 7))}><ChevronRightIcon /></IconButton>
              </Tooltip>
            </Stack>

            <Typography variant="subtitle1" sx={{ fontWeight: 700, minWidth: 240, textAlign: { xs:'left', md:'center' } }}>
              {weekLabel}
            </Typography>

            <FormControlLabel
              control={<Checkbox checked={showInactive} onChange={(_, v)=>setShowInactive(v)} />}
              label="Include inactive programs"
            />
          </Stack>

          <Stack direction={{ xs:'column', md:'row' }} spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Filter by coach"
              size="small"
              value={coachFilter}
              onChange={e=>setCoachFilter(e.target.value)}
              sx={{ width: { xs:'100%', md: 240 } }}
            />
            <FormControl size="small" sx={{ width: { xs:'100%', md: 260 } }}>
              <InputLabel>Filter by program</InputLabel>
              <Select
                label="Filter by program"
                value={programFilter}
                onChange={e => setProgramFilter(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))}
              >
                <MenuItem value="ALL">All programs</MenuItem>
                {programs.map(p => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.title} {p.active ? '' : '(inactive)'}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </Paper>

        {error && (
          <Paper sx={{ p:2, borderRadius:3, background:'rgba(255,255,255,0.95)', mb:2 }}>
            <Alert severity="error">{error}</Alert>
          </Paper>
        )}

        {loading ? (
          <Box sx={{ textAlign:'center', py:8, color:'white' }}><CircularProgress /></Box>
        ) : (
          <Paper sx={{ p:0, borderRadius:3, background:'rgba(255,255,255,0.98)' }}>
            {/* Header row: times + weekdays */}
            <Box
              sx={{
                display:'grid',
                gridTemplateColumns: '72px repeat(7, 1fr)',
                borderBottom: theme => `1px solid ${alpha(theme.palette.common.black,0.12)}`
              }}
            >
              <Box />
              {weekDays.map((d, i) => (
                <Box
                  key={i}
                  sx={{
                    p:1,
                    borderLeft: theme => `1px solid ${alpha(theme.palette.common.black,0.12)}`,
                    display: 'grid',
                    placeItems: 'center',
                    textAlign: 'center',
                  }}
                >
                  <Stack spacing={0.5} alignItems="center">
                    <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
                      {DAY_NAMES[i]}
                    </Typography>
                    <Chip
                      size="small"
                      variant="outlined"
                      label={new Intl.DateTimeFormat(undefined, { month:'short', day:'numeric' }).format(d)}
                    />
                  </Stack>
                </Box>
              ))}
            </Box>

            {/* Grid body */}
            <Box sx={{ display:'grid', gridTemplateColumns: '72px repeat(7, 1fr)', height: COLUMN_HEIGHT + 40, overflow: 'auto' }}>
              {/* time gutter */}
              <Box
                sx={{
                  position:'relative',
                  height: COLUMN_HEIGHT,
                  borderRight: theme => `1px solid ${alpha(theme.palette.common.black,0.12)}`
                }}
              >
                {HOURS_TICKS.map(h => (
                  <Box
                    key={h}
                    sx={{
                      position:'absolute',
                      top: (h - VISIBLE_START_HOUR) * PX_PER_HOUR + 2,
                      right: 4,
                      fontSize: 11,
                      color: 'text.secondary'
                    }}
                  >
                    {`${h}:00`}
                  </Box>
                ))}
              </Box>

              {/* day columns */}
              {weekDays.map((_, dayIdx) => {
                const laid = layoutDay(weekEventsByDay[dayIdx] ?? []);
                return (
                  <Column key={dayIdx}>
                    {/* hour lines */}
                    {HOURS_TICKS.map(h => (
                      <HourLine key={h} style={{ top: (h - VISIBLE_START_HOUR) * PX_PER_HOUR }} />
                    ))}

                    {/* events */}
                    {laid.map(ev => {
                      const color = programColor(ev.programId);
                      const bg = alpha(color, ev.active ? 0.22 : 0.10);
                      const border = alpha(color, ev.active ? 0.9 : 0.5);
                      const fg = ev.active ? color : alpha(color, 0.8);
                      const title = ev.title || 'Program';
                      const coach = ev.coach ? ` • ${ev.coach}` : '';

                      const topPx = clamp((ev as any).top, 0, COLUMN_HEIGHT - 18);
                      const bottomPx = clamp((ev as any).top + (ev as any).height, 0, COLUMN_HEIGHT);
                      const heightPx = Math.max(18, bottomPx - topPx);

                      if (heightPx <= 0) return null;

                      return (
                        <Box
                          key={ev.id}
                          onClick={() => {
                            setSelected(ev);
                            loadAttendance(ev);
                          }}
                          sx={{
                            position:'absolute',
                            top: topPx,
                            left: `calc(${(ev as any).leftPct}% + 2px)`,
                            width: `calc(${(ev as any).widthPct}% - ${(ev as any).gapX + 4}px)`,
                            height: heightPx,
                            bgcolor: bg,
                            border: `1px solid ${border}`,
                            borderRadius: 1.5,
                            overflow: 'hidden',
                            cursor: 'pointer',
                            boxShadow: 1,
                          }}
                        >
                          <Box sx={{ px: 1, py: 0.5 }}>
                            <Typography variant="caption" sx={{ display:'block', fontWeight: 700, color: fg, lineHeight: 1.2 }}>
                              {title}
                            </Typography>
                            <Typography variant="caption" sx={{ display:'block', lineHeight: 1.2 }}>
                              {fmtRange(ev.start, ev.end)}{coach}
                            </Typography>
                            <Stack direction="row" spacing={0.5} sx={{ mt: 0.5 }}>
                              <Chip size="small" label={ev.active ? 'Active' : 'Inactive'} sx={{ height: 18 }} />
                            </Stack>
                          </Box>
                        </Box>
                      );
                    })}
                  </Column>
                );
              })}
            </Box>
          </Paper>
        )}

        {/* Legend */}
        {!loading && programs.length > 0 && (
          <Paper sx={{ p:2, mt:2, borderRadius:3, background:'rgba(255,255,255,0.9)' }}>
            <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" alignItems="center">
              <Typography variant="subtitle2" sx={{ fontWeight:700, mr:1 }}>Programs</Typography>
              {programs.map(p => (
                <Chip
                  key={p.id}
                  variant="outlined"
                  label={`${p.title}${p.active ? '' : ' (inactive)'}`}
                  icon={<InfoOutlinedIcon />}
                  sx={{
                    borderColor: alpha(programColor(p.id), 0.5),
                    color: programColor(p.id),
                    '& .MuiChip-icon': { color: programColor(p.id) },
                  }}
                />
              ))}
            </Stack>
          </Paper>
        )}

        {/* Admin Table Credits Pane (embedded, lazy-loaded, safe no-op if missing) */}
        <Paper sx={{ p:2, mt:2, borderRadius:3, background:'rgba(255,255,255,0.9)' }}>
          <Typography variant="h6" sx={{ fontWeight: 800, mb: 1 }}>
            Table Rental Credits
          </Typography>
          <Suspense fallback={<Typography variant="body2">Loading credits…</Typography>}>
            <AdminTableCreditsPane />
          </Suspense>
        </Paper>
      </Container>

      {/* Attendance modal */}
      <Dialog open={!!selected} onClose={() => setSelected(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Program Occurrence</DialogTitle>
        <DialogContent dividers>
          {selected && (
            <Stack spacing={1.5}>
              <Typography variant="h6" sx={{ fontWeight: 800 }}>
                {selected.title}{selected.active ? '' : ' (inactive)'}
              </Typography>
              <Typography variant="body2">{fmtRange(selected.start, selected.end)}</Typography>
              {selected.coach && <Typography variant="body2">Coach: {selected.coach}</Typography>}

              <Divider sx={{ my: 1 }} />

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, flex: 1 }}>Eligible attendees</Typography>
                <TextField
                  size="small"
                  placeholder="Filter by name/email"
                  value={attSearch}
                  onChange={e => setAttSearch(e.target.value)}
                />
              </Stack>

              {attError && <Alert severity="error">{attError}</Alert>}
              {attLoading ? (
                <Box sx={{ textAlign: 'center', py: 2 }}><CircularProgress /></Box>
              ) : (
                <List dense disablePadding>
                  {filteredAttUsers.map(u => (
                    <ListItem
                      key={u.userId}
                      secondaryAction={
                        u.present ? (
                          <Button size="small" variant="outlined" onClick={() => markToggle(u, false)}>
                            Undo
                          </Button>
                        ) : (
                          <Button size="small" variant="contained" onClick={() => markToggle(u, true)}>
                            Mark as Present
                          </Button>
                        )
                      }
                    >
                      <ListItemText
                        primary={u.name ?? u.email}
                        secondary={`Sessions remaining: ${u.sessionsRemaining ?? 0}`}
                      />
                    </ListItem>
                  ))}
                  {filteredAttUsers.length === 0 && (
                    <Typography variant="body2" color="text.secondary" sx={{ py: 1 }}>
                      No eligible users found.
                    </Typography>
                  )}
                </List>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelected(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
