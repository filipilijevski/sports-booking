package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.TableRentalDtos;
import com.ttclub.backend.booking.model.TableRentalPackage;
import com.ttclub.backend.booking.repository.TableRentalPackageRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/table-credits/packages")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminTableCreditPackagesController {

    private final TableRentalPackageRepository repo;

    public AdminTableCreditPackagesController(TableRentalPackageRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<TableRentalDtos.PackageDto> list() {
        return repo.findAllOrderBySortAndPrice().stream().map(this::toDto).toList();
    }

    public record UpsertReq(String name, Double hours, Double priceCad, Boolean active, Integer sortOrder) {}

    @PostMapping
    @Transactional
    public TableRentalDtos.PackageDto create(@RequestBody UpsertReq req) {
        TableRentalPackage p = new TableRentalPackage();
        apply(p, req);
        repo.save(p);
        return toDto(p);
    }

    @PutMapping("/{id}")
    @Transactional
    public TableRentalDtos.PackageDto update(@PathVariable Long id, @RequestBody UpsertReq req) {
        TableRentalPackage p = repo.findById(id).orElseThrow();
        apply(p, req);
        repo.save(p);
        return toDto(p);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }

    /* helpers  */

    private void apply(TableRentalPackage p, UpsertReq r) {
        if (r.name() != null) p.setName(r.name().trim());
        if (r.hours() != null) p.setHours(BigDecimal.valueOf(r.hours()));
        if (r.priceCad() != null) p.setPriceCad(BigDecimal.valueOf(r.priceCad()));
        if (r.active() != null) p.setActive(r.active());
        if (r.sortOrder() != null) p.setSortOrder(r.sortOrder());
    }

    private TableRentalDtos.PackageDto toDto(TableRentalPackage p) {
        TableRentalDtos.PackageDto d = new TableRentalDtos.PackageDto();
        d.id = p.getId();
        d.name = p.getName();
        d.hours = p.getHours().doubleValue();
        d.priceCad = p.getPriceCad().doubleValue();
        d.active = p.getActive();
        d.sortOrder = p.getSortOrder();
        return d;
    }
}
