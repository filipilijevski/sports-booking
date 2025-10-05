/* src/main/java/com/ttclub/backend/controller/CategoryController.java */
package com.ttclub.backend.controller;

import com.ttclub.backend.model.Category;
import com.ttclub.backend.repository.CategoryRepository;
import com.ttclub.backend.service.CategoryService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository repo;
    private final CategoryService svc;

    public CategoryController(CategoryRepository repo, CategoryService svc) {
        this.repo = repo;
        this.svc  = svc;
    }

    /* Public Read */

    @GetMapping
    public List<Category> list() {
        return repo.findAll(Sort.by("name").ascending());
    }

    /* Admin or Owner Crud */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public Category create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        boolean exists = repo.findAll().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name.trim()));
        if (exists) throw new IllegalArgumentException("Category already exists");
        return repo.save(new Category(name.trim()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public void delete(@PathVariable Long id) {
        svc.deleteCategory(id);
    }
}
