package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.TableRentalDtos;
import com.ttclub.backend.booking.service.TableCreditCalculatorService;
import com.ttclub.backend.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/my/table-credits")
public class MyTableCreditsController {

    private final TableCreditCalculatorService calc;

    public MyTableCreditsController(TableCreditCalculatorService calc) {
        this.calc = calc;
    }

    @GetMapping("/summary")
    public TableRentalDtos.CreditSummaryDto summary(@AuthenticationPrincipal User user) {
        BigDecimal v = calc.hoursAvailableForUser(user.getId());
        TableRentalDtos.CreditSummaryDto d = new TableRentalDtos.CreditSummaryDto();
        d.hoursRemaining = v.doubleValue();
        return d;
    }
}
