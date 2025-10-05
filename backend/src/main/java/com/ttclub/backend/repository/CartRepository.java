//  Adds a *fetch-join* helper so controllers can access items
//  outside the repository method without triggering LAZY errors.

package com.ttclub.backend.repository;

import com.ttclub.backend.model.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /** Existing helper (kept for compatibility) */
    Optional<Cart> findByUserId(Long userId);

    /* eagerly fetch items and products in one round-trip
     so we’re safe outside TX boundaries (in controllers) */
    @EntityGraph(attributePaths = { "items", "items.product" })
    @Query("select c from Cart c where c.user.id = :uid")
    Optional<Cart> findByUserIdFetchAll(@Param("uid") Long userId);
}
