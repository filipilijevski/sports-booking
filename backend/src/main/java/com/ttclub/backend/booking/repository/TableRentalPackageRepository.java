package com.ttclub.backend.booking.repository;

import com.ttclub.backend.booking.model.TableRentalPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TableRentalPackageRepository extends JpaRepository<TableRentalPackage, Long> {

    @Query("select p from TableRentalPackage p order by coalesce(p.sortOrder, 0), p.priceCad asc, p.id asc")
    List<TableRentalPackage> findAllOrderBySortAndPrice();

    @Query("select p from TableRentalPackage p where p.active = true order by coalesce(p.sortOrder, 0), p.priceCad asc, p.id asc")
    List<TableRentalPackage> listPublicActive();
}
