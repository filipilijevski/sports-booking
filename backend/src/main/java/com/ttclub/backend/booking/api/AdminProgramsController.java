package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.ProgramDto;
import com.ttclub.backend.booking.dto.ProgramPackageCreateReq;
import com.ttclub.backend.booking.dto.ProgramPackageDto;
import com.ttclub.backend.booking.dto.ProgramPackageUpdateReq;
import com.ttclub.backend.booking.dto.ProgramSlotCreateReq;
import com.ttclub.backend.booking.dto.ProgramSlotDto;
import com.ttclub.backend.booking.dto.ProgramSlotUpdateReq;
import com.ttclub.backend.booking.dto.ProgramUpdateReq;
import com.ttclub.backend.booking.dto.ProgramCreateReq;
import com.ttclub.backend.booking.service.AdminProgramService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/programs")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminProgramsController {

    private final AdminProgramService svc;

    public AdminProgramsController(AdminProgramService svc) { this.svc = svc; }

    /* Programs */
    @PostMapping
    public ProgramDto create(@RequestBody ProgramCreateReq req) { return svc.createProgram(req); }

    @GetMapping
    public List<ProgramDto> list() { return svc.listPrograms(); }

    @GetMapping("/{id}")
    public ProgramDto get(@PathVariable Long id) { return svc.getProgram(id); }

    @PutMapping("/{id}")
    public ProgramDto update(@PathVariable Long id, @RequestBody ProgramUpdateReq req) { return svc.updateProgram(id, req); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { svc.deleteProgram(id); return ResponseEntity.noContent().build(); }

    /* Packages */
    @PostMapping("/{id}/packages")
    public ProgramPackageDto addPackage(@PathVariable Long id, @RequestBody ProgramPackageCreateReq req) {
        return svc.addPackage(id, req);
    }

    @PutMapping("/packages/{packageId}")
    public ProgramPackageDto updatePackage(@PathVariable Long packageId, @RequestBody ProgramPackageUpdateReq req) {
        return svc.updatePackage(packageId, req);
    }

    @DeleteMapping("/packages/{packageId}")
    public ResponseEntity<Void> deletePackage(@PathVariable Long packageId) {
        svc.deletePackage(packageId);
        return ResponseEntity.noContent().build();
    }

    /* Slots */
    @PostMapping("/{id}/slots")
    public ProgramSlotDto addSlot(@PathVariable Long id, @RequestBody ProgramSlotCreateReq req) {
        return svc.addSlot(id, req);
    }

    @PutMapping("/slots/{slotId}")
    public ProgramSlotDto updateSlot(@PathVariable Long slotId, @RequestBody ProgramSlotUpdateReq req) {
        return svc.updateSlot(slotId, req);
    }

    @DeleteMapping("/slots/{slotId}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long slotId) {
        svc.deleteSlot(slotId);
        return ResponseEntity.noContent().build();
    }
}
