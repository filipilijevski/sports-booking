/* src/main/java/com/ttclub/backend/service/CategoryService.java */
package com.ttclub.backend.service;

import com.ttclub.backend.model.Category;
import com.ttclub.backend.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categories;

    @PersistenceContext
    private EntityManager em;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    /** Deletes a category and sets category=NULL for products referencing it. */
    public void deleteCategory(Long id) {
        if (id == null) return;
        // best-effort nullify references
        em.createQuery("update Product p set p.category = null where p.category.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        categories.deleteById(id);
    }
}
