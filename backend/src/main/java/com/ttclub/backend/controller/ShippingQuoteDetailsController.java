package com.ttclub.backend.controller;

import com.ttclub.backend.model.OrderItem;
import com.ttclub.backend.model.Product;
import com.ttclub.backend.model.ShippingAddress;
import com.ttclub.backend.model.ShippingMethod;
import com.ttclub.backend.repository.ProductRepository;
import com.ttclub.backend.service.CanadaPostRateProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "/api/shipping/quote", produces = MediaType.APPLICATION_JSON_VALUE)
public class ShippingQuoteDetailsController {

    private final CanadaPostRateProvider cp;
    private final ProductRepository products;

    public ShippingQuoteDetailsController(CanadaPostRateProvider cp,
                                          ProductRepository products) {
        this.cp = cp;
        this.products = products;
    }

    /* DTOs */
    public record ItemReq(Long productId, int quantity) {}
    public record QuoteReq(ShippingAddress shippingAddress, List<ItemReq> items) {}

    public record ParcelDto(
            long grams,
            BigDecimal weightKg,
            Integer lengthCm,
            Integer widthCm,
            Integer heightCm
    ) {}

    public record QuoteDetailsResp(
            BigDecimal regular,
            BigDecimal express,
            ParcelDto parcel
    ) {}

    @PostMapping("/details")
    public QuoteDetailsResp quoteWithParcel(@RequestBody QuoteReq req) {

        // Build transient OrderItems (not persisted)
        List<OrderItem> lines = new ArrayList<>();
        for (ItemReq i : req.items()) {
            Product p = products.findById(i.productId())
                    .orElseThrow(() -> new IllegalArgumentException("Product " + i.productId() + " not found"));
            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setQuantity(i.quantity());
            lines.add(oi);
        }

        // Ask provider for rate + parcel
        CanadaPostRateProvider.QuoteResult reg =
                cp.rateWithInfo(ShippingMethod.REGULAR, lines, req.shippingAddress());
        CanadaPostRateProvider.QuoteResult exp =
                cp.rateWithInfo(ShippingMethod.EXPRESS, lines, req.shippingAddress());

        // Parcel is identical across methods - pick REGULAR’s description
        CanadaPostRateProvider.ParcelInfo par = reg.parcel();
        ParcelDto parcelDto = new ParcelDto(
                par.grams(),
                par.weightKg(),
                par.lengthCm(),
                par.widthCm(),
                par.heightCm()
        );

        return new QuoteDetailsResp(reg.rate(), exp.rate(), parcelDto);
    }
}
