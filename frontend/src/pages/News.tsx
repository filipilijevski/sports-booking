import { useEffect, useState } from 'react';
import {
  Container, Box, Typography, Card, CardActionArea, CardContent, CardMedia,
  Button, Stack, Pagination
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { fetchPublicPosts, type BlogPostCard, type Paged } from '../lib/blog';
import { resolveImgUrl } from '../lib/assets';
import DOMPurify from 'dompurify';

/* Page background */
const INFO_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

/* Per-request: two variables to control card content colors */
const CARD_TEXT_COLOR = '#FFFFFF';
const CARD_BG_COLOR   = 'rgba(40, 13, 44, 0.4)'; // like the purple-tinted pill surface

export default function News() {
  const [page, setPage] = useState(1); // 1-based for MUI Pagination
  const [data, setData] = useState<Paged<BlogPostCard> | null>(null);
  const nav = useNavigate();

  const load = async (p: number) => {
    const res = await fetchPublicPosts(p - 1, 10);
    if (res) setData(res);
  };

  useEffect(() => { load(page); }, [page]);

  return (
    <Box sx={{ width: '100%', minHeight: '100vh', background: INFO_GRADIENT }}>
      <Container sx={{ py: 4 }}>
        <Typography variant="h3" align="center" sx={{ fontWeight: 700, mb: 3, color: 'common.white' }}>
          News & Events
        </Typography>

        <Typography variant="h6" align="center" sx={{ fontWeight: 500, mb: 3, color: 'common.white' }}>
          Check out tournaments, results and everything going on at the Club!
        </Typography>

        <Stack spacing={2}>
          {(data?.content ?? []).map(post => (
            <Card
              key={post.id}
              variant="outlined"
              sx={{
                borderRadius: 6,
                overflow: 'hidden',
                borderColor: 'rgba(255, 255, 255, 0.31)',
                background: 'transparent', // image shows true colors; content block sets its own bg
              }}
            >
              <CardActionArea onClick={() => nav(`/news/${post.id}`)}>
                {post.mainImageUrl && (
                  <CardMedia
                    component="img"
                    height="220"
                    image={resolveImgUrl(post.mainImageUrl)}
                    alt={post.title}
                    sx={{ objectFit: 'cover' }}
                  />
                )}

                {/* Content block uses configurable surface + text color */}
                <CardContent
                  sx={{
                    backgroundColor: CARD_BG_COLOR,
                    color: CARD_TEXT_COLOR,
                  }}
                >
                  <Typography variant="h4" sx={{ fontWeight: 700 }}>
                    {post.title}
                  </Typography>

                  {post.subtitle && (
                    <Typography variant="subtitle1" sx={{ mb: 1, opacity: 0.9 }}>
                      {post.subtitle}
                    </Typography>
                  )}

                  {/* Excerpt may contain HTML so sanitize and render, inherits color */}
                  {post.excerpt && (
                    // eslint-disable-next-line react/no-danger
                    <Box
                      sx={{ mb: 1 }}
                      dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(post.excerpt) }}
                    />
                  )}

                  <Button
                    size="small"
                    variant="outlined"
                    sx={{
                      color: CARD_TEXT_COLOR,
                      borderColor: 'rgba(255,255,255,0.35)',
                      borderRadius: '999px',
                      textTransform: 'none',
                      px: 2,
                      '&:hover': { borderColor: 'rgba(255,255,255,0.6)', backgroundColor: 'rgba(255,255,255,0.08)' },
                    }}
                  >
                    Read More
                  </Button>
                </CardContent>
              </CardActionArea>
            </Card>
          ))}
        </Stack>

        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
          {data && (
            <Pagination
              page={page}
              onChange={(_e, p) => setPage(p)}
              count={data.totalPages}
              showFirstButton
              showLastButton
            />
          )}
        </Box>
      </Container>
    </Box>
  );
}
