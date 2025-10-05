package com.ttclub.backend.repository;

import com.ttclub.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Product repository with a PostgreSQL full-text / trigram search.
 *
 * <p>The native query behaves as follows:</p>
 * <ul>
 *   <li>If <code>:q</code> is an empty string, it returns <em>all</em> products
 *       ordered by name - a convenient fall-back when the caller only
 *       filters by category.</li>
 *   <li>Otherwise it ranks matches by <code>ts_rank</code>
 *       in descending order.</li>
 * </ul>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    /**
     * Full-text search over product name + description.
     * @param q the raw user query (can be blank).
     */
    @Query(value = """
        SELECT *
        FROM products
        WHERE (:q = '' OR
               to_tsvector('simple', name || ' ' || coalesce(description, ''))
                   @@ plainto_tsquery('simple', :q))
        ORDER BY
            CASE WHEN :q = '' THEN name END ASC,
            ts_rank(
              to_tsvector('simple', name || ' ' || coalesce(description, '')),
              plainto_tsquery('simple', :q)
            ) DESC
        """, nativeQuery = true)
    List<Product> fullTextSearch(@Param("q") String q);
}
