/* src/main/java/com/ttclub/backend/service/ProductService.java */
package com.ttclub.backend.service;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.mapper.ProductMapper;
import com.ttclub.backend.model.*;
import com.ttclub.backend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository   products;
    private final CategoryRepository  categories;
    private final ProductMapper       mapper;
    private final FileStorageService  storage;
    private final ProductAuditService audit;

    @PersistenceContext
    private EntityManager em;

    public ProductService(ProductRepository   products,
                          CategoryRepository  categories,
                          ProductMapper       mapper,
                          FileStorageService  storage,
                          ProductAuditService audit) {
        this.products   = products;
        this.categories = categories;
        this.mapper     = mapper;
        this.storage    = storage;
        this.audit      = audit;
    }

    /* Read API */

    public Page<ProductDto> search(String q,
                                   Long categoryId,
                                   int page, int size) {

        Pageable pg = PageRequest.of(page, size);

        if (!StringUtils.hasText(q) && categoryId == null)
            return products.findAll(pg).map(mapper::toDto);

        List<Product> hits = products.fullTextSearch(q == null ? "" : q);

        if (categoryId != null) {
            hits.removeIf(p ->
                    !categoryId.equals(
                            p.getCategory() == null ? null : p.getCategory().getId()
                    )
            );
        }

        int from = Math.min(page * size, hits.size());
        int to   = Math.min(from + size, hits.size());

        return new PageImpl<>(
                mapper.toDtoList(hits.subList(from, to)),
                pg,
                hits.size()
        );
    }

    /* Create or Update */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ProductDto create(UpsertProductDto dto) {
        Product p = mapper.toEntity(dto);
        applyCategory(p, dto.getCategoryId());
        applyInlineImages(p, dto.getImages());
        ensureDefaults(p);

        Product saved = products.save(p);
        audit.logCreate(saved, dto.getImages() != null && !dto.getImages().isEmpty());
        return mapper.toDto(saved);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ProductDto update(Long id, UpsertProductDto dto) {
        Product p = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // snapshot before for audit diff
        Product before = new Product();
        before.setId(p.getId());
        before.setSku(p.getSku());
        before.setName(p.getName());
        before.setDescription(p.getDescription());
        before.setPrice(p.getPrice());
        before.setInventoryQty(p.getInventoryQty());
        before.setBrand(p.getBrand());
        before.setGrams(p.getGrams());
        before.setCategory(p.getCategory());

        p.setSku(dto.getSku());
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setInventoryQty(dto.getInventoryQty());
        p.setBrand(dto.getBrand());
        p.setGrams(dto.getGrams());
        p.setIsActive(dto.getActive());

        applyCategory(p, dto.getCategoryId());
        boolean imagesModified = dto.getImages() != null;
        applyInlineImages(p, dto.getImages());

        audit.logUpdate(before, p, imagesModified);
        return mapper.toDto(p);
    }

    /* Image upload */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ProductDto addImage(Long id,
                               MultipartFile file,
                               String altText,
                               boolean primary) {

        Product p = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        String url = storage.store(file);

        if (primary) {
            p.getImages().forEach(i -> i.setPrimary(false));
        }

        ProductImage img = new ProductImage();
        img.setUrl(url);
        img.setAltText(altText);
        img.setPrimary(primary);
        img.setSortOrder((short) p.getImages().size());

        p.addImage(img);

        audit.logImageChange(p);
        return mapper.toDto(p);
    }

    /* Adjust stock */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ProductDto changeStock(Long id, int qty) {
        Product p = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        p.setInventoryQty(qty);
        audit.logStockChange(p, qty);
        return mapper.toDto(p);
    }

    /* Delete */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public void delete(Long id) {
        Product p = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Detach any order-items to preserve historical orders
        em.createQuery("update OrderItem oi set oi.product = null where oi.product.id = :pid")
                .setParameter("pid", id)
                .executeUpdate();

        // Remove binaries (best effort)
        p.getImages().forEach(img -> storage.delete(img.getUrl()));

        // Log delete with snapshot
        audit.logDelete(p);

        products.delete(p);
    }

    private void applyCategory(Product p, Long catId) {
        if (catId == null) { p.setCategory(null); return; }
        Category c = categories.findById(catId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        p.setCategory(c);
    }

    /** sets safe defaults for nullable DTO fields */
    private void ensureDefaults(Product p) {
        if (p.getInventoryQty() == null) p.setInventoryQty(0);
        if (p.getIsActive()     == null) p.setIsActive(Boolean.TRUE);
    }

    /** Replace gallery based on DTO. */
    private void applyInlineImages(Product p, List<ProductImageDto> imgs) {
        if (imgs == null) return;

        p.getImages().forEach(old -> storage.delete(old.getUrl()));
        p.getImages().clear();

        imgs.forEach(d -> {
            ProductImage i = new ProductImage();
            i.setUrl(d.getUrl());
            i.setAltText(d.getAltText());
            i.setPrimary(d.isPrimary());
            i.setSortOrder(d.getSortOrder());
            p.addImage(i);
        });

        if (p.getImages().stream().noneMatch(ProductImage::isPrimary) && !p.getImages().isEmpty()) {
            p.getImages().get(0).setPrimary(true);
        }
    }
}
