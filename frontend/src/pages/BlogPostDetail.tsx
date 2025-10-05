import { useEffect, useMemo, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchPublicPost, type BlogPostDetail } from '../lib/blog';
import {
  Container, Box, Typography, Paper, Button, Divider,
  Dialog, DialogContent, IconButton
} from '@mui/material';
import { resolveImgUrl } from '../lib/assets';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import CloseIcon from '@mui/icons-material/Close';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';

/* Page background (keep consistent with site) */
const INFO_GRADIENT =
  'linear-gradient(270deg,rgba(0, 54, 126, 1) 0%,rgba(181, 94, 231, 1) 100%)';

/* Detail content surface */
const DETAIL_TEXT_COLOR = '#FFFFFF';
const DETAIL_BG_COLOR   = 'rgba(40, 13, 44, 0.4)'; 

export default function BlogPostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const nav = useNavigate();
  const [post, setPost] = useState<BlogPostDetail | null>(null);

  /* Lightbox state */
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [currentIdx, setCurrentIdx] = useState(0);

  useEffect(() => {
    const load = async () => {
      if (!id) return;
      const p = await fetchPublicPost(Number(id));
      if (p) setPost(p);
    };
    load();
  }, [id]);

  const html = useMemo(() => {
    const raw = post?.bodyMarkdown ?? '';
    const unsafe = marked.parse(raw) as string; // sync usage for DOMPurify
    return DOMPurify.sanitize(unsafe);
  }, [post?.bodyMarkdown]);

  const openLightbox = useCallback((idx: number) => {
    if (!post?.images?.length) return;
    setCurrentIdx(idx);
    setLightboxOpen(true);
  }, [post?.images]);

  const closeLightbox = useCallback(() => setLightboxOpen(false), []);
  const prev = useCallback(() => {
    if (!post?.images?.length) return;
    setCurrentIdx(i => (i - 1 + post.images.length) % post.images.length);
  }, [post?.images]);
  const next = useCallback(() => {
    if (!post?.images?.length) return;
    setCurrentIdx(i => (i + 1) % post.images.length);
  }, [post?.images]);

  /* Keyboard navigation inside lightbox */
  useEffect(() => {
    if (!lightboxOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeLightbox();
      if (e.key === 'ArrowLeft') prev();
      if (e.key === 'ArrowRight') next();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [lightboxOpen, closeLightbox, prev, next]);

  if (!post) return null;

  return (
    <Box sx={{ width: '100%', minHeight: '100vh', background: INFO_GRADIENT }}>
      <Container sx={{ py: 4 }}>
        <Button
          variant="outlined"
          onClick={() => nav('/news')}
          sx={{
            mb: 2,
            color: '#fff',
            borderColor: '#fff',
            borderRadius: '999px',
            textTransform: 'none',
            px: 2,
            '&:hover': { borderColor: '#fff', backgroundColor: 'rgba(255,255,255,0.12)' },
          }}
        >
          ← Return to posts
        </Button>

        <Typography variant="h3" sx={{ fontWeight: 700, mb: 1, color: 'common.white' }}>
          {post.title}
        </Typography>
        {post.subtitle && (
          <Typography variant="h5" sx={{ mb: 2, color: 'rgba(255,255,255,0.9)' }}>
            {post.subtitle}
          </Typography>
        )}

        {post.mainImageUrl && (
          <Box
            component="img"
            src={resolveImgUrl(post.mainImageUrl)}
            alt={post.mainImageAlt || post.title}
            sx={{
              width: '100%',
              maxHeight: 420,
              objectFit: 'cover',
              borderRadius: 2,
              mb: 3,
            }}
          />
        )}

        <Paper
          elevation={3}
          sx={{
            p: { xs: 1.5, md: 2.5 },
            borderRadius: 3,
            backgroundColor: DETAIL_BG_COLOR,
            color: DETAIL_TEXT_COLOR,
            border: '1px solid rgba(255,255,255,0.18)',
          }}
        >
          {/* eslint-disable-next-line react/no-danger */}
          <div dangerouslySetInnerHTML={{ __html: html }} />
        </Paper>

        {post.images?.length ? (
          <>
            <Divider sx={{ my: 3, borderColor: 'rgba(255,255,255,0.25)' }} />
            <Typography variant="h6" sx={{ mb: 1, fontWeight: 700, color: 'common.white' }}>
              Gallery
            </Typography>

            <Box sx={{ overflowX: 'auto', display: 'flex', gap: 1, pb: 1 }}>
              {post.images.map((img, idx) => (
                <Box
                  key={img.id}
                  component="img"
                  src={resolveImgUrl(img.url)}
                  alt={img.altText || ''}
                  role="button"
                  tabIndex={0}
                  onClick={() => openLightbox(idx)}
                  onKeyDown={(e) => { if (e.key === 'Enter') openLightbox(idx); }}
                  sx={{
                    width: 180,
                    height: 120,
                    objectFit: 'cover',
                    borderRadius: 1,
                    flex: '0 0 auto',
                    cursor: 'pointer',
                    outline: 'none',
                    '&:focus': { boxShadow: theme => `0 0 0 2px ${theme.palette.primary.main}` },
                  }}
                />
              ))}
            </Box>
          </>
        ) : null}
      </Container>

      {/* Lightbox dialog */}
      <Dialog open={lightboxOpen} onClose={closeLightbox} maxWidth="lg" fullWidth>
        <DialogContent sx={{ position: 'relative', p: 0, bgcolor: 'common.black' }}>
          {/* Close */}
          <IconButton
            aria-label="Close"
            onClick={closeLightbox}
            sx={{
              position: 'absolute', top: 8, right: 8, zIndex: 2,
              color: 'common.white', backgroundColor: 'rgba(0,0,0,0.3)',
              '&:hover': { backgroundColor: 'rgba(0,0,0,0.5)' },
            }}
          >
            <CloseIcon />
          </IconButton>

          {/* Prev */}
          <IconButton
            aria-label="Previous image"
            onClick={prev}
            disabled={!post?.images?.length || post.images.length < 2}
            sx={{
              position: 'absolute', top: '50%', left: 8, transform: 'translateY(-50%)',
              zIndex: 2, color: 'common.white', backgroundColor: 'rgba(0,0,0,0.3)',
              '&:hover': { backgroundColor: 'rgba(0,0,0,0.5)' },
            }}
          >
            <ChevronLeftIcon />
          </IconButton>

          {/* Next */}
          <IconButton
            aria-label="Next image"
            onClick={next}
            disabled={!post?.images?.length || post.images.length < 2}
            sx={{
              position: 'absolute', top: '50%', right: 8, transform: 'translateY(-50%)',
              zIndex: 2, color: 'common.white', backgroundColor: 'rgba(0,0,0,0.3)',
              '&:hover': { backgroundColor: 'rgba(0,0,0,0.5)' },
            }}
          >
            <ChevronRightIcon />
          </IconButton>

          {/* Image */}
          {post?.images?.length ? (
            <Box
              component="img"
              src={resolveImgUrl(post.images[currentIdx]?.url)}
              alt={post.images[currentIdx]?.altText || ''}
              sx={{
                display: 'block',
                width: '100%',
                maxHeight: '80vh',
                objectFit: 'contain',
                backgroundColor: 'common.black',
                mx: 'auto',
              }}
            />
          ) : null}
        </DialogContent>
      </Dialog>
    </Box>
  );
}
