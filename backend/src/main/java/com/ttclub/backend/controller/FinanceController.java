package com.ttclub.backend.controller;

import com.ttclub.backend.dto.FinanceRowDto;
import com.ttclub.backend.service.FinanceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/admin/finance")
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class FinanceController {

    private final FinanceService svc;

    public FinanceController(FinanceService svc) { this.svc = svc; }

    /* GET /api/admin/finance/csv?from=2025-01-01&to=2025-01-31 */
    @GetMapping("/csv")
    public void csv(@RequestParam LocalDate from,
                    @RequestParam LocalDate to,
                    HttpServletResponse res) throws Exception {

        res.setHeader(HttpHeaders.CONTENT_TYPE, "text/csv");
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"finance_%s_%s.csv\""
                        .formatted(from, to));

        List<FinanceRowDto> rows = svc.fetchBetween(
                Timestamp.valueOf(from.atStartOfDay()),
                Timestamp.valueOf(to.plusDays(1).atStartOfDay().minusSeconds(1)));

        try (PrintWriter pw = res.getWriter()) {
            pw.println("id,date,action,subtotal,shipping,tax,total,status");
            for (FinanceRowDto r : rows) {
                pw.printf("%d,%s,%s,%.2f,%.2f,%.2f,%.2f,%s%n",
                        r.id(), r.ts(), r.action(),
                        r.subtotal(), r.shipping(), r.tax(), r.total(), r.status());
            }
        }
    }
}
