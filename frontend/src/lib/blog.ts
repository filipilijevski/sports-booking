import { api, apiPublic } from './api';

/* Shared types */
export interface BlogImage {
  id: number;
  url: string;
  altText?: string | null;
  sortOrder: number;
}

export interface BlogPostCard {
  id: number;
  title: string;
  subtitle?: string | null;
  mainImageUrl?: string | null;
  createdAt: string;
  excerpt?: string | null;
}

export interface BlogPostDetail {
  id: number;
  title: string;
  subtitle?: string | null;
  bodyMarkdown: string;
  mainImageUrl?: string | null;
  mainImageAlt?: string | null;
  images: BlogImage[];
  createdAt: string;
  updatedAt: string;
}

export interface AdminBlogPost extends BlogPostDetail {
  visible: boolean;
  sortOrder: number;
  deletedAt?: string | null;
}

/** Generic page wrapper aligned to backend Page<T> */
export interface Paged<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // zero-based page index
  size: number;
}

/* Public  */
export const fetchPublicPosts = (page = 0, size = 10) =>
  apiPublic<Paged<BlogPostCard>>(`/blog/posts?page=${page}&size=${size}`);

export const fetchPublicPost = (id: number) =>
  apiPublic<BlogPostDetail>(`/blog/posts/${id}`);

/* Admin */
export const adminListPosts = (page = 0, size = 5) =>
  api<Paged<AdminBlogPost>>(`/admin/blog/posts?page=${page}&size=${size}`);

export const adminCreatePost = (payload: {
  title: string;
  subtitle?: string;
  bodyMarkdown: string;
  visible?: boolean;
  sortOrder?: number;
  mainImageAlt?: string;
}) =>
  api<AdminBlogPost>('/admin/blog/posts', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const adminUpdatePost = (id: number, payload: Partial<{
  title: string;
  subtitle: string;
  bodyMarkdown: string;
  visible: boolean;
  sortOrder: number;
  mainImageAlt: string;
}>) =>
  api<AdminBlogPost>(`/admin/blog/posts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });

export const adminDeletePost = (id: number) =>
  api<void>(`/admin/blog/posts/${id}`, { method: 'DELETE' });

export const adminRestorePost = (id: number) =>
  api<void>(`/admin/blog/posts/${id}/restore`, { method: 'POST' });

export const adminUploadMainImage = async (id: number, file: File, altText?: string) => {
  const fd = new FormData();
  fd.append('file', file);
  if (altText) fd.append('altText', altText);
  return api<AdminBlogPost>(`/admin/blog/posts/${id}/main-image`, { method: 'POST', body: fd });
};

export const adminAddSecondaryImage = async (id: number, file: File, altText?: string, sortOrder?: number) => {
  const fd = new FormData();
  fd.append('file', file);
  if (altText) fd.append('altText', altText);
  if (typeof sortOrder === 'number') fd.append('sortOrder', String(sortOrder));
  return api<AdminBlogPost>(`/admin/blog/posts/${id}/images`, { method: 'POST', body: fd });
};

export const adminReorderImages = (id: number, items: { id: number; sortOrder: number }[]) =>
  api<AdminBlogPost>(`/admin/blog/posts/${id}/images/reorder`, {
    method: 'PUT',
    body: JSON.stringify(items),
  });

export const adminDeleteImage = (postId: number, imageId: number) =>
  api<AdminBlogPost>(`/admin/blog/posts/${postId}/images/${imageId}`, { method: 'DELETE' });
