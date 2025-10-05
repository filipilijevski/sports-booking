package com.ttclub.backend.controller;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.service.BlogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/blog/posts")
public class AdminBlogController {

    private final BlogService svc;

    public AdminBlogController(BlogService svc) {
        this.svc = svc;
    }

    @GetMapping
    public Page<AdminBlogPostDto> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "5") int size) {
        return svc.adminList(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminBlogPostDto create(@RequestBody @Valid BlogPostUpsertDto dto) {
        return svc.create(dto);
    }

    @PutMapping("/{id}")
    public AdminBlogPostDto update(@PathVariable long id,
                                   @RequestBody @Valid BlogPostUpsertDto dto) {
        return svc.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSoft(@PathVariable long id) {
        svc.deleteSoft(id);
    }

    @PostMapping("/{id}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable long id) {
        svc.restore(id);
    }

    /* images */

    @PostMapping("/{id}/main-image")
    public AdminBlogPostDto uploadMain(@PathVariable long id,
                                       @RequestPart("file") MultipartFile file,
                                       @RequestParam(required = false) String altText) {
        return svc.setMainImage(id, file, altText);
    }

    @PostMapping("/{id}/images")
    public AdminBlogPostDto addImage(@PathVariable long id,
                                     @RequestPart("file") MultipartFile file,
                                     @RequestParam(required = false) String altText,
                                     @RequestParam(required = false) Short sortOrder) {
        return svc.addSecondaryImage(id, file, altText, sortOrder);
    }

    @PutMapping("/{id}/images/reorder")
    public AdminBlogPostDto reorder(@PathVariable long id,
                                    @RequestBody List<BlogImageDto> newOrder) {
        return svc.reorderImages(id, newOrder);
    }

    @DeleteMapping("/{postId}/images/{imageId}")
    public AdminBlogPostDto removeImage(@PathVariable long postId,
                                        @PathVariable long imageId) {
        return svc.deleteImage(postId, imageId);
    }
}
