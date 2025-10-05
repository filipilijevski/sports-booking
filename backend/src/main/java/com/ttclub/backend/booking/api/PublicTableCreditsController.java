package com.ttclub.backend.booking.api;

import com.stripe.exception.StripeException;
import com.ttclub.backend.booking.dto.TableRentalDtos;
import com.ttclub.backend.booking.model.TableRentalPackage;
import com.ttclub.backend.booking.repository.TableRentalPackageRepository;
import com.ttclub.backend.booking.service.TableRentalCheckoutService;
import com.ttclub.backend.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/table-credits")
public class PublicTableCreditsController {

    private final TableRentalPackageRepository packs;
    private final TableRentalCheckoutService checkout;

    public PublicTableCreditsController(TableRentalPackageRepository packs,
                                        TableRentalCheckoutService checkout) {
        this.packs = packs;
        this.checkout = checkout;
    }

    @GetMapping("/packages")
    public List<TableRentalDtos.PackageDto> list() {
        return packs.listPublicActive().stream().map(p -> {
            TableRentalDtos.PackageDto d = new TableRentalDtos.PackageDto();
            d.id = p.getId();
            d.name = p.getName();
            d.hours = p.getHours().doubleValue();
            d.priceCad = p.getPriceCad().doubleValue();
            d.active = p.getActive();
            d.sortOrder = p.getSortOrder();
            return d;
        }).toList();
    }

    @GetMapping("/packages/{id}/quote")
    public TableRentalDtos.QuoteDto quote(@PathVariable Long id) {
        return checkout.quote(id);
    }

    @PostMapping("/packages/{id}/payment-intent")
    @ResponseStatus(HttpStatus.CREATED)
    public TableRentalDtos.StartResp start(@PathVariable Long id,
                                           @AuthenticationPrincipal User user) throws StripeException {
        return checkout.start(user, id);
    }

    public record FinalizeReq(String paymentIntentId, Long bookingId) {}

    @PostMapping("/finalize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finalizePurchase(@RequestBody FinalizeReq req,
                                 @AuthenticationPrincipal User user) throws StripeException {
        checkout.finalizeAfterClientConfirmation(req.paymentIntentId(), req.bookingId(), user);
    }
}
