package com.ttclub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secure")
public class HelloController {

    /** Accessible to any authenticated user (no extra role check) */
    @GetMapping("/hello")
    public ResponseEntity<String> hello(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok("Hello, " + principal.toString() + "\n");
    }
}
