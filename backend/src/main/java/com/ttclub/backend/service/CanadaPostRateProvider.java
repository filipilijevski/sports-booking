package com.ttclub.backend.service;

import com.ttclub.backend.model.OrderItem;
import com.ttclub.backend.model.ShippingAddress;
import com.ttclub.backend.model.ShippingMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Canada Post “Get Rates” REST v4.
 *
 * <pre>
 * canadapost:
 *   username: YOUR_USER
 *   password: YOUR_PASS
 *   customer: ""              # optional - contracts only
 *
 * store:
 *   origin.postal: H0H0H0   # no spaces
 *
 * # Optional: simple packaging heuristics to improve quote accuracy
 * shipping:
 *   dims:
 *     small:  28,21,4    # L,W,H in cm (padded envelope)
 *     medium: 33,25,10   # small box
 *     large:  45,35,20   # bigger box
 *   thresholds:
 *     maxSmallItems: 2   # if <= and total weight < 0.5 kg - small
 *     maxSmallKg:    0.5
 *     maxMediumKg:   5.0
 * </pre>
 *
 * • If any CP credential is missing => flat fee (10/20 CAD).
 * • If dims are configured, we add dimensions to the request.
 */
@Service
public class CanadaPostRateProvider implements ShippingRateProvider {

    private static final Logger log = LoggerFactory.getLogger(CanadaPostRateProvider.class);

    private static final String URL        = "https://ct.soa-gw.canadapost.ca/rs/ship/price";
    private static final String MEDIA_TYPE = "application/vnd.cpc.ship.rate-v4+xml";
    private static final BigDecimal MIN_WEIGHT_KG = new BigDecimal("0.001");   // >= 1 g

    private final RestTemplate rest = new RestTemplate();

    private final boolean liveCreds;
    private final String  authHeader;      // "Basic"
    private final String  customerNumber;  // may be ""
    private final String  originPostal;    // store’s origin PC (no spaces)

    // optional packaging config
    private final int smallL, smallW, smallH;
    private final int medL, medW, medH;
    private final int largeL, largeW, largeH;
    private final int  maxSmallItems;
    private final BigDecimal maxSmallKg;
    private final BigDecimal maxMediumKg;
    private final boolean haveDims;

    public CanadaPostRateProvider(
            @Value("${canadapost.username:}")             String user,
            @Value("${canadapost.password:}")             String pass,
            @Value("${canadapost.customer:}")             String custNo,
            @Value("${store.origin.postal:K1V1J5}")       String originPostal,

            // Optional dimension heuristics (safe defaults)
            @Value("${shipping.dims.small:28,21,4}")      String smallDims,
            @Value("${shipping.dims.medium:33,25,10}")    String mediumDims,
            @Value("${shipping.dims.large:45,35,20}")     String largeDims,
            @Value("${shipping.thresholds.maxSmallItems:2}") int maxSmallItems,
            @Value("${shipping.thresholds.maxSmallKg:0.5}")  BigDecimal maxSmallKg,
            @Value("${shipping.thresholds.maxMediumKg:5.0}") BigDecimal maxMediumKg
    ) {

        this.liveCreds     = !user.isBlank() && !pass.isBlank();
        this.authHeader    = liveCreds
                ? "Basic " + Base64.getEncoder()
                .encodeToString((user + ':' + pass).getBytes())
                : "";
        this.customerNumber = custNo == null ? "" : custNo.trim();
        this.originPostal   = originPostal.replaceAll("\\s+", "");

        // parse dimensions
        int[] s = parseDims(smallDims);
        int[] m = parseDims(mediumDims);
        int[] l = parseDims(largeDims);

        this.smallL = s[0]; this.smallW = s[1]; this.smallH = s[2];
        this.medL   = m[0]; this.medW   = m[1]; this.medH   = m[2];
        this.largeL = l[0]; this.largeW = l[1]; this.largeH = l[2];

        this.maxSmallItems = maxSmallItems;
        this.maxSmallKg    = maxSmallKg;
        this.maxMediumKg   = maxMediumKg;
        this.haveDims      = (smallL > 0 && smallW > 0 && smallH > 0);
    }

    private static int[] parseDims(String csv) {
        try {
            String[] p = csv.split(",");
            return new int[] { Integer.parseInt(p[0].trim()),
                    Integer.parseInt(p[1].trim()),
                    Integer.parseInt(p[2].trim()) };
        } catch (Exception e) {
            return new int[] { 0, 0, 0 };
        }
    }

    /*  Diagnostics DTOs as records */

    /** Immutable description of the parcel Canada Post will rate. */
    public record ParcelInfo(
            long grams,
            BigDecimal weightKg,
            Integer lengthCm,
            Integer widthCm,
            Integer heightCm
    ) {}

    /** Rate + the exact parcel that was used to compute it. */
    public record QuoteResult(
            BigDecimal rate,
            ParcelInfo parcel
    ) {}

    /* Public API (ShippingRateProvider) */

    @Override
    public BigDecimal rateFor(ShippingMethod method,
                              List<OrderItem> items,
                              ShippingAddress to) {
        ParcelInfo parcel = parcelFor(items);
        return rateFor(method, parcel, to);
    }

    /** Return both rate and the parcel details (for UI/diagnostics). */
    public QuoteResult rateWithInfo(ShippingMethod method,
                                    List<OrderItem> items,
                                    ShippingAddress to) {
        ParcelInfo parcel = parcelFor(items);
        BigDecimal rate   = rateFor(method, parcel, to);
        return new QuoteResult(rate, parcel);
    }

    /** Compute parcel once (grams, kg, chosen dims). */
    public ParcelInfo parcelFor(List<OrderItem> items) {
        // 1) grams -> weight kg (3dp, >= 1 g)
        long grams = items.stream()
                .mapToLong(i -> {
                    Integer g = i.getProduct().getGrams();
                    long perItemGrams = (g != null && g > 0 ? g : 1);
                    return perItemGrams * (long) i.getQuantity();
                })
                .sum();

        BigDecimal weightKg = BigDecimal.valueOf(grams)
                .divide(BigDecimal.valueOf(1_000), 3, RoundingMode.UP)
                .max(MIN_WEIGHT_KG);

        // 2) choose box (optional)
        int[] dims = chooseBox(items.size(), weightKg);
        Integer l = (dims == null ? null : dims[0]);
        Integer w = (dims == null ? null : dims[1]);
        Integer h = (dims == null ? null : dims[2]);

        return new ParcelInfo(grams, weightKg, l, w, h);
    }

    /* Internal helpers used by both old & new paths */

    /** Core that builds the XML using a precomputed parcel. */
    public BigDecimal rateFor(ShippingMethod method,
                              ParcelInfo parcel,
                              ShippingAddress to) {

        /* flat-fee fallback when creds absent */
        if (!liveCreds) return fallback(method);

        /*  XML payload (counter quote) */
        String destPc = to.getPostalCode().replaceAll("\\s+", "");
        StringBuilder xml = new StringBuilder(320)
                .append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                .append("<mailing-scenario xmlns=\"http://www.canadapost.ca/ws/ship/rate-v4\">");
        if (!customerNumber.isEmpty()) {
            xml.append("<customer-number>").append(customerNumber).append("</customer-number>");
        }
        xml.append("<quote-type>counter</quote-type>")
                .append("<parcel-characteristics><weight>")
                .append(parcel.weightKg().stripTrailingZeros().toPlainString())
                .append("</weight>");

        // add dimensions if configured
        if (parcel.lengthCm() != null && parcel.widthCm() != null && parcel.heightCm() != null) {
            xml.append("<dimensions>")
                    .append("<length>").append(parcel.lengthCm()).append("</length>")
                    .append("<width>").append(parcel.widthCm()).append("</width>")
                    .append("<height>").append(parcel.heightCm()).append("</height>")
                    .append("</dimensions>");
        }

        xml.append("</parcel-characteristics>")
                .append("<origin-postal-code>").append(originPostal).append("</origin-postal-code>")
                .append("<destination><domestic><postal-code>")
                .append(destPc)
                .append("</postal-code></domestic></destination>")
                .append("</mailing-scenario>");

        /*  HTTP call */
        HttpHeaders h = new HttpHeaders();
        h.set(HttpHeaders.AUTHORIZATION, authHeader);
        h.setAccept(List.of(MediaType.parseMediaType(MEDIA_TYPE)));
        h.setContentType(MediaType.parseMediaType(MEDIA_TYPE));
        h.setAcceptLanguageAsLocales(List.of(Locale.CANADA));

        try {
            ResponseEntity<String> resp = rest.exchange(
                    URL, HttpMethod.POST, new HttpEntity<>(xml.toString(), h), String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("Canada Post rating call non-OK: {}", resp.getStatusCode());
                return fallback(method);
            }
            return extractRate(resp.getBody(), method);

        } catch (Exception ex) {
            log.error("Canada Post rating call failed - using flat fee", ex);
            return fallback(method);
        }
    }

    /** Pick a box by simple heuristics; return null if dimensions disabled. */
    private int[] chooseBox(int itemCount, BigDecimal weightKg) {
        if (!haveDims) return null;

        if (itemCount <= maxSmallItems && weightKg.compareTo(maxSmallKg) <= 0) {
            return new int[] { smallL, smallW, smallH };
        }
        if (weightKg.compareTo(maxMediumKg) <= 0) {
            return new int[] { medL, medW, medH };
        }
        return new int[] { largeL, largeW, largeH };
    }

    private static BigDecimal fallback(ShippingMethod m) {
        return m == ShippingMethod.REGULAR
                ? new BigDecimal("10.00")
                : new BigDecimal("20.00");
    }

    /** Pull due for DOM.RP/DOM.XP out of the XML answer. */
    private static BigDecimal extractRate(String xml, ShippingMethod m) {
        String svc = (m == ShippingMethod.REGULAR) ? "DOM.RP" : "DOM.XP";
        try {
            var db = DocumentBuilderFactory.newInstance();
            db.setNamespaceAware(true);
            var doc = db.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            NodeList quotes = doc.getElementsByTagNameNS("*", "price-quote");
            for (int i = 0; i < quotes.getLength(); i++) {
                Element q = (Element) quotes.item(i);
                if (svc.equalsIgnoreCase(getChildText(q, "service-code"))) {
                    Element priceDetails =
                            (Element) q.getElementsByTagNameNS("*", "price-details").item(0);
                    return new BigDecimal(getChildText(priceDetails, "due").trim());
                }
            }
            // If exact service not found, fall back (keeps UX responsive)
            log.warn("Service {} not found in CP response - XML:\n{}", svc, xml);
        } catch (Exception ex) {
            log.error("Failed parsing CP response - XML:\n{}", xml, ex);
        }
        return fallback(m);
    }

    private static String getChildText(Element parent, String local) {
        NodeList nl = parent.getElementsByTagNameNS("*", local);
        return nl.getLength() == 0 ? "" : nl.item(0).getTextContent();
    }
}
