import { useEffect, useMemo, useRef, useState } from 'react';
import Grid from '@mui/material/Grid'; // default import to fix TS Grid overload errors
import {
  Container, Typography, Box, Stack, Card, CardContent, CardActions, Button,
  TextField, Switch, FormControlLabel, Divider, IconButton, Tooltip, Pagination,
  Dialog, DialogTitle, DialogContent, DialogActions, Paper
} from '@mui/material';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import DeleteIcon from '@mui/icons-material/Delete';
import RestoreFromTrashIcon from '@mui/icons-material/RestoreFromTrash';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import AddPhotoAlternateIcon from '@mui/icons-material/AddPhotoAlternate';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import SaveIcon from '@mui/icons-material/Save';
import {
  adminListPosts, adminCreatePost, adminUpdatePost, adminDeletePost, adminRestorePost,
  adminUploadMainImage, adminAddSecondaryImage, adminDeleteImage, adminReorderImages,
  type AdminBlogPost, type Paged, type BlogImage
} from '../lib/blog';
import { resolveImgUrl } from '../lib/assets';
import MarkdownToolbar from '../components/MarkdownToolbar';

/* Page background */
const INFO_GRADIENT = 'linear-gradient(0deg,rgba(50, 100, 207, 1) 100%,rgba(50, 100, 207, 1) 100%)';

type Direction = -1 | 1;

/** Default: visible = false (admins publish explicitly) */
const emptyDraft = () => ({
  title: '',
  subtitle: '',
  bodyMarkdown: '',
  visible: false,             // default invisibility for new posts
  sortOrder: 0,
  mainImageAlt: '',
});

export default function AdminBlog() {
  const [page, setPage] = useState(1);
  const [data, setData] = useState<Paged<AdminBlogPost> | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AdminBlogPost | null>(null);
  const [draft, setDraft] = useState<any>(emptyDraft());

  const bodyRef = useRef<HTMLTextAreaElement | null>(null);

  const load = async (p: number) => {
    const res = await adminListPosts(p - 1, 5);
    if (res) setData(res);
  };

  useEffect(() => { load(page); }, [page]);

  const openCreate = () => {
    setEditing(null);
    setDraft(emptyDraft());
    setOpen(true);
  };

  const openEdit = (p: AdminBlogPost) => {
    setEditing(p);
    setDraft({
      title: p.title,
      subtitle: p.subtitle ?? '',
      bodyMarkdown: p.bodyMarkdown,
      visible: p.visible,
      sortOrder: p.sortOrder,
      mainImageAlt: p.mainImageAlt ?? '',
    });
    setOpen(true);
  };

  const save = async () => {
    // clamp sortOrder to >= 0 in the payload (UI guard + server guard)
    const normalized = {
      ...draft,
      sortOrder: Math.max(0, Number(draft.sortOrder) || 0),
    };

    if (editing) {
      const orderChanged = normalized.sortOrder !== editing.sortOrder;
      if (orderChanged) {
        const ok = window.confirm(
          `Change order from ${editing.sortOrder} to ${normalized.sortOrder}? ` +
          `Other posts will be shifted automatically.`
        );
        if (!ok) return;
      }
      const updated = await adminUpdatePost(editing.id, normalized);
      if (updated) {
        setOpen(false);
        await load(page);
      }
    } else {
      const created = await adminCreatePost(normalized);
      if (created) {
        setOpen(false);
        setPage(1);
        await load(1);
      }
    }
  };

  const del = async (p: AdminBlogPost) => {
    await adminDeletePost(p.id);
    await load(page);
  };

  const restore = async (p: AdminBlogPost) => {
    await adminRestorePost(p.id);
    await load(page);
  };

  const uploadMain = async (postId: number, file?: File) => {
    if (!file) return;
    await adminUploadMainImage(postId, file, draft.mainImageAlt || undefined);
    await load(page);
  };

  const addSecondary = async (postId: number, file?: File) => {
    if (!file) return;
    await adminAddSecondaryImage(postId, file);
    await load(page);
  };

  const removeImage = async (postId: number, imageId: number) => {
    await adminDeleteImage(postId, imageId);
    await load(page);
  };

  const reorder = async (postId: number, imgs: BlogImage[], index: number, dir: Direction) => {
    const next = index + dir;
    if (next < 0 || next >= imgs.length) return;

    const copy: BlogImage[] = imgs.map(x => ({ ...x }));
    // swap sortOrder values
    const a = copy[index], b = copy[next];
    const so = a.sortOrder; a.sortOrder = b.sortOrder; b.sortOrder = so;

    await adminReorderImages(
      postId,
      copy.map((img: BlogImage) => ({ id: img.id, sortOrder: img.sortOrder }))
    );
    await load(page);
  };

  const previewHtml = useMemo(() => {
    const raw = draft.bodyMarkdown ?? '';
    const unsafe = marked.parse(raw) as string; // force sync usage
    return DOMPurify.sanitize(unsafe);
  }, [draft.bodyMarkdown]);

  return (
    <Box sx={{ width: '100%', minHeight: '100vh', background: INFO_GRADIENT }}>
      <Container sx={{ py: 4 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
          <Typography variant="h3" sx={{ fontWeight: 700, color: 'common.white' }}>Manage Blog Posts</Typography>
          <Button variant="contained" onClick={openCreate}>New Post</Button>
        </Stack>

        <Stack spacing={2}>
          {(data?.content ?? []).map(p => (
            <Card key={p.id} variant="outlined">
              <CardContent>
                <Stack direction="row" spacing={2}>
                  <Box sx={{ width: 180, height: 120, bgcolor: 'grey.100', borderRadius: 1, overflow: 'hidden' }}>
                    {p.mainImageUrl ? (
                      <img src={resolveImgUrl(p.mainImageUrl)} alt={p.title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    ) : (
                      <Box sx={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'text.secondary' }}>
                        No image
                      </Box>
                    )}
                  </Box>
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography variant="h6" sx={{ fontWeight: 700 }}>{p.title}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Created: {new Date(p.createdAt).toLocaleString()} • Last edited: {new Date(p.updatedAt).toLocaleString()}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Order: {p.sortOrder} • Visible: {p.visible ? 'Yes' : 'No'} {p.deletedAt ? '• (Deleted)' : ''}
                    </Typography>
                  </Box>
                </Stack>

                <Divider sx={{ my: 2 }} />

                {/* Secondary images mini gallery with controls */}
                <Stack direction="row" spacing={1} sx={{ overflowX: 'auto', pb: 1 }}>
                  {p.images.map((img, idx) => (
                    <Paper key={img.id} variant="outlined" sx={{ p: 1, minWidth: 200 }}>
                      <Box sx={{ width: 180, height: 100, overflow: 'hidden', borderRadius: 1, mb: 1 }}>
                        <img src={resolveImgUrl(img.url)} alt={img.altText || ''} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      </Box>
                      <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
                        <Stack direction="row" spacing={0.5}>
                          <Tooltip title="Move up">
                            <span>
                              <IconButton size="small" onClick={() => reorder(p.id, p.images, idx, -1)} disabled={idx === 0}>
                                <ArrowUpwardIcon fontSize="small" />
                              </IconButton>
                            </span>
                          </Tooltip>
                          <Tooltip title="Move down">
                            <span>
                              <IconButton size="small" onClick={() => reorder(p.id, p.images, idx, 1)} disabled={idx === p.images.length - 1}>
                                <ArrowDownwardIcon fontSize="small" />
                              </IconButton>
                            </span>
                          </Tooltip>
                        </Stack>
                        <Tooltip title="Remove image">
                          <IconButton size="small" onClick={() => removeImage(p.id, img.id)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </Paper>
                  ))}

                  {/* Add image button */}
                  <Paper variant="outlined" sx={{ p: 1, minWidth: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Button component="label" startIcon={<AddPhotoAlternateIcon />}>
                      Add image
                      <input type="file" hidden accept="image/*" onChange={e => addSecondary(p.id, e.target.files?.[0])} />
                    </Button>
                  </Paper>
                </Stack>
              </CardContent>

              <CardActions>
                <Button variant="outlined" onClick={() => openEdit(p)}>Edit</Button>

                <Button component="label" startIcon={<UploadFileIcon />}>
                  Upload primary image
                  <input type="file" hidden accept="image/*" onChange={e => uploadMain(p.id, e.target.files?.[0])} />
                </Button>

                <Box sx={{ flexGrow: 1 }} />
                {p.deletedAt ? (
                  <Button color="secondary" startIcon={<RestoreFromTrashIcon />} onClick={() => restore(p)}>
                    Restore
                  </Button>
                ) : (
                  <Button color="error" startIcon={<DeleteIcon />} onClick={() => del(p)}>
                    Delete
                  </Button>
                )}
              </CardActions>
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

        {/* Editor dialog */}
        <Dialog open={open} onClose={() => setOpen(false)} maxWidth="lg" fullWidth>
          <DialogTitle>{editing ? 'Edit Post' : 'Create Post'}</DialogTitle>
          <DialogContent dividers>
            <Grid container spacing={3}>
              {/* Title / Subtitle */}
              <Grid item xs={12} md={6}>
                <TextField
                  label="Title"
                  fullWidth
                  margin="dense"
                  value={draft.title}
                  onChange={e => setDraft((d: any) => ({ ...d, title: e.target.value }))}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <TextField
                  label="Subtitle"
                  fullWidth
                  margin="dense"
                  value={draft.subtitle}
                  onChange={e => setDraft((d: any) => ({ ...d, subtitle: e.target.value }))}
                />
              </Grid>

              {/* Order + Primary Image Alt (avoid overlap; visibility moved to bottom) */}
              <Grid item xs={12} sm={6} md={4}>
                <TextField
                  label="Order"
                  type="number"
                  fullWidth
                  margin="dense"
                  value={draft.sortOrder}
                  inputProps={{ min: 0, step: 1 }}
                  onChange={e => {
                    const n = Math.max(0, Number(e.target.value) || 0);
                    setDraft((d: any) => ({ ...d, sortOrder: n }));
                  }}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={8}>
                <TextField
                  label="Primary Image Alt Text"
                  fullWidth
                  margin="dense"
                  value={draft.mainImageAlt}
                  onChange={e => setDraft((d: any) => ({ ...d, mainImageAlt: e.target.value }))}
                />
              </Grid>

              {/* Body (Markdown) - full width with toolbar and textarea */}
              <Grid item xs={12}>
                <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Body (Markdown)</Typography>
                <MarkdownToolbar
                  value={draft.bodyMarkdown}
                  onChange={(next) => setDraft((d: any) => ({ ...d, bodyMarkdown: next }))}
                  textareaRef={bodyRef}
                />
                <TextField
                  multiline
                  minRows={12}
                  fullWidth
                  inputRef={bodyRef}
                  value={draft.bodyMarkdown}
                  onChange={e => setDraft((d: any) => ({ ...d, bodyMarkdown: e.target.value }))}
                  sx={{ width: '100%' }}
                />
              </Grid>

              {/* Live Preview (below body) */}
              <Grid item xs={12}>
                <Typography variant="subtitle2" sx={{ mb: 0.5 }}>Live Preview</Typography>
                <Paper variant="outlined" sx={{ p: 2, maxHeight: 420, overflow: 'auto', width: '100%' }}>
                  {/* eslint-disable-next-line react/no-danger */}
                  <div dangerouslySetInnerHTML={{ __html: previewHtml }} />
                </Paper>
              </Grid>

              {/* Visibility toggle - last input before actions; defaults to OFF */}
              <Grid item xs={12}>
                <FormControlLabel
                  sx={{ mt: 1 }}
                  control={
                    <Switch
                      checked={!!draft.visible}
                      onChange={e => setDraft((d: any) => ({ ...d, visible: e.target.checked }))}
                    />
                  }
                  label="Visible to public"
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button startIcon={<SaveIcon />} onClick={save} variant="contained">Save</Button>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
          </DialogActions>
        </Dialog>
      </Container>
    </Box>
  );
}
