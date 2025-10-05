package com.ttclub.backend.repository;

import com.ttclub.backend.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> { }