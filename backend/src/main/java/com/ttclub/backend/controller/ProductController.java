package com.ttclub.backend.controller;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService svc;

    public ProductController(ProductService svc) { this.svc = svc; }

    /* Public Search */

    @GetMapping
    public Page<ProductDto> search(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) Long category,
                                   @RequestParam(defaultValue = "0")  int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return svc.search(q, category, page, size);
    }

    /* CRUD for owner and admin */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@RequestBody UpsertProductDto dto) {
        return svc.create(dto);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id,
                             @RequestBody UpsertProductDto dto) {
        return svc.update(id, dto);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    @PatchMapping("/{id}/stock")
    public ProductDto changeStock(@PathVariable Long id,
                                  @RequestParam int quantity) {
        return svc.changeStock(id, quantity);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { svc.delete(id); }

    /* image upload */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    @PostMapping(
            path     = "/{id}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ProductDto upload(@PathVariable Long id,
                             @RequestPart("file") MultipartFile file,
                             @RequestParam(value = "altText",  required = false) String altText,
                             @RequestParam(value = "primary", defaultValue = "false") boolean primary) {

        return svc.addImage(id, file, altText, primary);
    }
}
