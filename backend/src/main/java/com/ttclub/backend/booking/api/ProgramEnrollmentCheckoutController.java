package com.ttclub.backend.booking.api;

import com.stripe.exception.StripeException;
import com.ttclub.backend.booking.dto.ProgramCheckoutDtos.CheckoutReq;
import com.ttclub.backend.booking.dto.ProgramCheckoutDtos.CheckoutResp;
import com.ttclub.backend.booking.service.ProgramEnrollmentCheckoutService;
import com.ttclub.backend.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/booking/checkout/program")
public class ProgramEnrollmentCheckoutController {

    private final ProgramEnrollmentCheckoutService svc;

    public ProgramEnrollmentCheckoutController(ProgramEnrollmentCheckoutService svc) {
        this.svc = svc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResp start(@Valid @RequestBody CheckoutReq req,
                              @AuthenticationPrincipal User user,
                              HttpServletRequest http) throws StripeException {
        if (req == null || req.packageId == null) {
            throw new IllegalArgumentException("packageId is required");
        }
        // Build absolute return URLs reliably from Origin
        String origin = Optional.ofNullable(http.getHeader("Origin"))
                .orElseGet(() -> http.getRequestURL().toString().replace(http.getRequestURI(), ""));
        String success = origin + "/coaching?enrolled=1";
        String cancel  = origin + "/coaching?canceled=1";
        return svc.start(user, req.packageId, success, cancel);
    }
}
