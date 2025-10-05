package com.ttclub.backend.controller;

import com.ttclub.backend.dto.BlogPostCardDto;
import com.ttclub.backend.dto.BlogPostDetailDto;
import com.ttclub.backend.service.BlogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blog/posts")
public class BlogController {

    private final BlogService svc;

    public BlogController(BlogService svc) {
        this.svc = svc;
    }

    /** Public list (visible + not deleted), ordered by sortOrder asc, created desc. */
    @GetMapping
    public Page<BlogPostCardDto> list(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return svc.listPublic(page, size);
    }

    /** Public detail, only visible posts. */
    @GetMapping("/{id}")
    public BlogPostDetailDto get(@PathVariable long id) {
        return svc.getPublic(id);
    }
}
