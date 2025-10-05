package com.ttclub.backend.repository;

import com.ttclub.backend.model.BlogImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogImageRepository extends JpaRepository<BlogImage, Long> {
    List<BlogImage> findByPost_IdOrderBySortOrderAscIdAsc(Long postId);
    long countByPost_Id(Long postId);
}
