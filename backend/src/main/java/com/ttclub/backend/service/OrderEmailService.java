/* src/main/java/com/ttclub/backend/service/OrderEmailService.java */
package com.ttclub.backend.service;

import com.ttclub.backend.model.Order;
import com.ttclub.backend.model.OrderItem;
import com.ttclub.backend.model.Product;
import com.ttclub.backend.model.ShippingAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class OrderEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailService.class);

    private final JavaMailSender mail;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final String bccInternal;

    public OrderEmailService(JavaMailSender mail,
                             @Value("${ttclub.mail.enabled:true}") boolean mailEnabled,
                             @Value("${ttclub.mail.from:no-reply@ttclub.local}") String fromAddress,
                             @Value("${ttclub.mail.bcc-internal:}") String bccInternal) {
        this.mail = mail;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
        this.bccInternal = bccInternal;
    }

    /** Sends a simple, itemized receipt to the buyer when the order is PAID. */
    public void sendOrderConfirmation(Order order) {
        String to = resolveRecipient(order);
        if (!StringUtils.hasText(to)) {
            log.info("Order {}: no recipient email available; skipping confirmation.", order.getId());
            return;
        }

        String subject = "Order #" + order.getId() + " - Payment Confirmation";
        String body = renderBody(order);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        if (StringUtils.hasText(bccInternal)) {
            msg.setBcc(bccInternal);
        }
        msg.setSubject(subject);
        msg.setText(body);

        if (!mailEnabled) {
            log.info("[DEV-MAIL] would send to {}: \n{}\n{}", to, subject, body);
            return;
        }
        try {
            mail.send(msg);
        } catch (MailException ex) {
            // do not fail the webhook or checkout flow due to mail problems
            log.error("Failed to send order confirmation for order {}", order.getId(), ex);
        }
    }

    private String resolveRecipient(Order o) {
        if (o.getUser() != null && StringUtils.hasText(o.getUser().getEmail())) {
            return o.getUser().getEmail();
        }
        ShippingAddress a = o.getShippingAddress();
        return (a != null && StringUtils.hasText(a.getEmail())) ? a.getEmail() : null;
    }

    private String renderBody(Order order) {
        NumberFormat c = NumberFormat.getCurrencyInstance(Locale.CANADA);

        StringBuilder body = new StringBuilder(1024);
        body.append("Thank you! Your payment was received.\n\n")
                .append("Order #: ").append(order.getId()).append('\n');

        ShippingAddress a = order.getShippingAddress();
        if (a != null) {
            String name = value(a.getFullName());
            body.append("Name: ").append(name).append('\n');
            if (StringUtils.hasText(a.getPhone()))  body.append("Phone: ").append(a.getPhone()).append('\n');
            if (StringUtils.hasText(a.getEmail()))  body.append("Email: ").append(a.getEmail()).append('\n');

            boolean hasShip = StringUtils.hasText(a.getLine1()) ||
                    StringUtils.hasText(a.getCity()) ||
                    StringUtils.hasText(a.getProvince()) ||
                    StringUtils.hasText(a.getPostalCode()) ||
                    StringUtils.hasText(a.getCountry());
            if (hasShip) {
                body.append("\nShipping Address:\n");
                if (StringUtils.hasText(a.getLine1()))       body.append(a.getLine1()).append('\n');
                if (StringUtils.hasText(a.getLine2()))       body.append(a.getLine2()).append('\n');
                if (StringUtils.hasText(a.getCity()) ||
                        StringUtils.hasText(a.getProvince()) ||
                        StringUtils.hasText(a.getPostalCode())) {
                    body.append(value(a.getCity()));
                    if (StringUtils.hasText(a.getProvince()))    body.append(", ").append(a.getProvince());
                    if (StringUtils.hasText(a.getPostalCode())) body.append("  ").append(a.getPostalCode());
                    body.append('\n');
                }
                if (StringUtils.hasText(a.getCountry()))     body.append(a.getCountry()).append('\n');
            }
        }

        body.append("\nItems:\n");
        for (OrderItem i : order.getItems()) {
            Product p = i.getProduct();
            String name = p != null && StringUtils.hasText(p.getName())
                    ? p.getName()
                    : "Product#" + (p == null ? "deleted" : p.getId());
            body.append("- ").append(name)
                    .append(" × ").append(i.getQuantity())
                    .append(" @ ").append(c.format(i.getUnitPrice()))
                    .append(" = ").append(c.format(i.getTotalPrice()))
                    .append('\n');
        }
        body.append('\n');

        body.append("Subtotal: ").append(c.format(order.getSubtotalAmount())).append('\n');
        body.append("Shipping: ").append(c.format(order.getShippingAmount())).append('\n');
        body.append("Tax: ").append(c.format(order.getTaxAmount())).append('\n');
        if (order.getDiscountAmount() != null &&
                order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            body.append("Discount: -").append(c.format(order.getDiscountAmount())).append('\n');
        }
        body.append("Total: ").append(c.format(order.getTotalAmount())).append("\n\n");

        body.append("If you have any questions, reply to this email.\n");
        return body.toString();
    }

    private static String value(String s) { return s == null ? "" : s; }
}
