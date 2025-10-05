package com.ttclub.backend.repository;

import com.ttclub.backend.model.BlogPost;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    /** Public listing: visible & not soft-deleted. Sorting applied by Pageable. */
    @Query("""
           select p from BlogPost p
           where p.visible = true
             and p.deletedAt is null
           """)
    Page<BlogPost> findPublic(Pageable pageable);

    @Query("""
           select p from BlogPost p
           where p.id = :id
             and p.deletedAt is null
           """)
    Optional<BlogPost> findActiveById(@Param("id") Long id);
}
