package com.ttclub.backend.controller;

import com.ttclub.backend.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final InventoryService inventory;

    public StockController(InventoryService inventory) {
        this.inventory = inventory;
    }

    public record AvailabilityRequest(List<Long> productIds) {}
    public record AvailabilityResponse(Map<Long, Integer> available) {}

    /** Bulk availability: returns { productId -> availableQty }. */
    @PostMapping("/availability")
    public AvailabilityResponse availability(@RequestBody AvailabilityRequest req) {
        Map<Long, Integer> map = inventory.availabilityFor(req.productIds());
        return new AvailabilityResponse(map);
    }
}
