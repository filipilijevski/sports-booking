package com.ttclub.backend.service;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.mapper.BlogMapper;
import com.ttclub.backend.model.*;
import com.ttclub.backend.repository.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class BlogService {

    private static final int MAX_SECONDARY_IMAGES = 10;

    private final BlogPostRepository posts;
    private final BlogImageRepository images;
    private final FileStorageService storage;
    private final BlogMapper mapper;

    public BlogService(BlogPostRepository posts,
                       BlogImageRepository images,
                       FileStorageService storage,
                       BlogMapper mapper) {
        this.posts   = posts;
        this.images  = images;
        this.storage = storage;
        this.mapper  = mapper;
    }

    /* Public */

    public Page<BlogPostCardDto> listPublic(int page, int size) {
        PageRequest pr = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 50),
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("createdAt"))
        );
        Page<BlogPost> p = posts.findPublic(pr);
        return p.map(bp -> {
            BlogPostCardDto dto = mapper.toCardDto(bp);
            return new BlogPostCardDto(
                    dto.id(),
                    dto.title(),
                    dto.subtitle(),
                    dto.mainImageUrl(),
                    dto.createdAt(),
                    firstParagraph(bp.getBodyMarkdown())
            );
        });
    }

    public BlogPostDetailDto getPublic(long id) {
        BlogPost p = posts.findActiveById(id)
                .filter(BlogPost::isVisible)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        return mapper.toDetailDto(p);
    }

    /* Admin */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public Page<AdminBlogPostDto> adminList(int page, int size) {
        PageRequest pr = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 50),
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("createdAt"))
        );
        return posts.findAll(pr).map(mapper::toAdminDto);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public AdminBlogPostDto create(@Valid BlogPostUpsertDto dto) {
        BlogPost p = new BlogPost();
        applyUpsert(p, dto); // honors provided sortOrder (default 0)
        posts.save(p);
        return mapper.toAdminDto(p);
    }

    /**
     * Update a post. If sortOrder is provided and changes, we reposition the post among
     * active (non-deleted) posts and reassign contiguous sortOrder values starting at 0.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public AdminBlogPostDto update(long id, @Valid BlogPostUpsertDto dto) {
        BlogPost p = posts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        boolean wantsReorder = dto.sortOrder() != null && !dto.sortOrder().equals(p.getSortOrder());

        if (wantsReorder) {
            // Apply other fields except sortOrder
            applyUpsertExceptSortOrder(p, dto);

            // Target position ( >= 0)
            int targetPos = Math.max(0, dto.sortOrder());

            // Current active list ordered by sortOrder asc, createdAt desc
            List<BlogPost> ordered = posts.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("createdAt")));
            List<BlogPost> active = new ArrayList<>();
            for (BlogPost bp : ordered) {
                if (bp.getDeletedAt() == null) active.add(bp);
            }

            // Find current index among active
            int curIndex = -1;
            for (int i = 0; i < active.size(); i++) {
                if (Objects.equals(active.get(i).getId(), p.getId())) {
                    curIndex = i; break;
                }
            }
            if (curIndex == -1) {
                // If not found (shouldn't happen hopefully), treat as append
                active.add(p);
                curIndex = active.size() - 1;
            }

            int maxIndex = Math.max(0, active.size() - 1);
            int newIndex = Math.min(targetPos, maxIndex);

            // If the element is removed before where it needs to go, the index shifts by -1
            active.remove(curIndex);
            if (newIndex > curIndex) newIndex -= 1;
            active.add(newIndex, p);

            // Reassign contiguous sort orders starting at 0
            for (int i = 0; i < active.size(); i++) {
                active.get(i).setSortOrder((short) i);
            }

            p.setUpdatedAt(Instant.now());
            return mapper.toAdminDto(p);
        } else {
            applyUpsert(p, dto);
            return mapper.toAdminDto(p);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public void deleteSoft(long id) {
        BlogPost p = posts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        if (p.getDeletedAt() == null) {
            p.setDeletedAt(Instant.now());
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public void restore(long id) {
        BlogPost p = posts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        p.setDeletedAt(null);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public AdminBlogPostDto setMainImage(long id, MultipartFile file, String altText) {
        BlogPost p = posts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        // delete previous file best-effort
        storage.delete(p.getMainImageUrl());

        String url = storage.store(file);
        p.setMainImageUrl(url);
        if (altText != null) p.setMainImageAlt(altText);
        p.setUpdatedAt(Instant.now());
        return mapper.toAdminDto(p);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public AdminBlogPostDto addSecondaryImage(long id, MultipartFile file, String altText, Short sortOrder) {
        BlogPost p = posts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        long count = images.countByPost_Id(id);
        if (count >= MAX_SECONDARY_IMAGES) {
            throw new IllegalArgumentException("Maximum of " + MAX_SECONDARY_IMAGES + " secondary images reached.");
        }

        String url = storage.store(file);
        BlogImage img = new BlogImage();
        img.setUrl(url);
        img.setAltText(altText);
        img.setSortOrder(sortOrder != null ? sortOrder : (short) count);
        p.addImage(img); // cascade persist
        p.setUpdatedAt(Instant.now());
        return mapper.toAdminDto(p);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public AdminBlogPostDto reorderImages(long id, List<BlogImageDto> newOrder) {
        BlogPost p = posts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        // update existing sortOrder; unknown ids are ignored
        for (BlogImageDto dto : newOrder) {
            if (dto.id() == null) continue;
            for (BlogImage img : p.getImages()) {
                if (Objects.equals(img.getId(), dto.id())) {
                    img.setSortOrder(dto.sortOrder());
                }
            }
        }
        p.setUpdatedAt(Instant.now());
        return mapper.toAdminDto(p);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public AdminBlogPostDto deleteImage(long postId, long imageId) {
        BlogPost p = posts.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        BlogImage target = images.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        if (!Objects.equals(target.getPost().getId(), postId)) {
            throw new IllegalArgumentException("Image does not belong to the specified post");
        }

        storage.delete(target.getUrl()); // best-effort
        p.removeImage(target);           // orphanRemoval = true
        p.setUpdatedAt(Instant.now());
        return mapper.toAdminDto(p);
    }

    /* helpers  */

    private void applyUpsert(BlogPost p, BlogPostUpsertDto dto) {
        if (dto.title() != null) p.setTitle(dto.title().trim());
        if (dto.subtitle() != null) p.setSubtitle(dto.subtitle().trim());
        if (dto.bodyMarkdown() != null) p.setBodyMarkdown(dto.bodyMarkdown());

        if (dto.visible() != null) p.setVisible(dto.visible());
        if (dto.sortOrder() != null) p.setSortOrder((short) Math.max(0, dto.sortOrder()));

        if (dto.mainImageAlt() != null) p.setMainImageAlt(dto.mainImageAlt().trim());

        if (p.getCreatedAt() == null) p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
    }

    /** Same as applyUpsert but ignores sortOrder (used when performing a full reorder) */
    private void applyUpsertExceptSortOrder(BlogPost p, BlogPostUpsertDto dto) {
        if (dto.title() != null) p.setTitle(dto.title().trim());
        if (dto.subtitle() != null) p.setSubtitle(dto.subtitle().trim());
        if (dto.bodyMarkdown() != null) p.setBodyMarkdown(dto.bodyMarkdown());

        if (dto.visible() != null) p.setVisible(dto.visible());
        // sortOrder intentionally ignored here

        if (dto.mainImageAlt() != null) p.setMainImageAlt(dto.mainImageAlt().trim());

        if (p.getCreatedAt() == null) p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
    }

    /** markdown to first paragraph helper for list cards */
    static String firstParagraph(String md) {
        if (md == null || md.isBlank()) return "";
        String[] lines = md.replace("\r", "").split("\n");
        StringBuilder b = new StringBuilder();
        for (String ln : lines) {
            String s = ln.trim();
            if (s.isEmpty()) {
                if (!b.isEmpty()) break; // end of first paragraph
                else continue;
            }
            // strip a few common markdown markers for a plain-text preview
            s = s.replaceAll("^#{1,6}\\s*", "");   // headings
            s = s.replace("**", "").replace("__", "");
            s = s.replace("*", "").replace("_", "");
            s = s.replace("`", "");
            s = s.replaceAll("^[-+*]\\s+", "");    // list bullets
            if (!b.isEmpty()) b.append(' ');
            b.append(s);
        }
        return b.toString().trim();
    }
}
