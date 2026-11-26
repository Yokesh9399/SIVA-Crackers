package com.sivacrackers.billing;

import com.sivacrackers.billing.BillingDtos.BillDto;
import com.sivacrackers.billing.BillingDtos.BillPage;
import com.sivacrackers.billing.BillingDtos.BillRequest;
import com.sivacrackers.billing.BillingDtos.PaymentMethodDto;
import com.sivacrackers.billing.BillingDtos.PaymentMethodRequest;
import com.sivacrackers.billing.BillingDtos.PaymentUpdateRequest;
import com.sivacrackers.billing.BillingDtos.VoidRequest;
import com.sivacrackers.common.Params;
import com.sivacrackers.common.ShopTime;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * In-store billing – staff only, inside the admin dashboard. All staff can bill, see bills and print invoices;
 * only OWNER and MANAGER can void a bill; only the OWNER manages payment methods.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
public class AdminBillingController {

    private final BillingService billing;
    private final PdfDocuments pdf;
    private final Clock clock;

    public AdminBillingController(BillingService billing, PdfDocuments pdf, Clock clock) {
        this.billing = billing;
        this.pdf = pdf;
        this.clock = clock;
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodDto> paymentMethods() {
        return billing.paymentMethods();
    }

    @PostMapping("/payment-methods")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PaymentMethodDto> createMethod(@Valid @RequestBody PaymentMethodRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billing.createMethod(body));
    }

    @PutMapping("/payment-methods/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public PaymentMethodDto updateMethod(@PathVariable Long id, @Valid @RequestBody PaymentMethodRequest body) {
        return billing.updateMethod(id, body);
    }

    /** {@code ?from=&to=&search=(bill no., customer name or phone)&paymentMethod=CASH&paymentStatus=PAID|PENDING&status=FINALIZED|VOIDED&customerId=&page=&size=25|50|100} */
    @GetMapping("/bills")
    public BillPage list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(required = false) String search,
                         @RequestParam(required = false) String paymentMethod,
                         @RequestParam(required = false) String paymentStatus,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) Integer page,
                         @RequestParam(required = false) Integer size) {
        ShopTime.Range range = from == null && to == null ? null : ShopTime.Range.of(from, to, clock);
        return billing.list(new BillingService.Filter(range, Params.search(search), paymentMethod,
                        Params.enumOrNull(Bill.PaymentStatus.class, "paymentStatus", paymentStatus),
                        Params.enumOrNull(Bill.Status.class, "status", status), customerId),
                Params.page(page), Params.size(size, List.of(25, 50, 100), 25));
    }

    @GetMapping("/bills/{id}")
    public BillDto get(@PathVariable Long id) {
        return billing.get(id);
    }

    /** Finalizes a bill: 201 for a new bill, 200 with {@code replayed: true} if this requestKey was already billed. */
    @PostMapping("/bills")
    public ResponseEntity<BillDto> create(@Valid @RequestBody BillRequest body) {
        BillDto bill = billing.create(body);
        return ResponseEntity.status(bill.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(bill);
    }

    @PostMapping("/bills/{id}/void")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public BillDto voidBill(@PathVariable Long id, @Valid @RequestBody VoidRequest body) {
        return billing.voidBill(id, body.reason());
    }

    /** Records that a PENDING bill has now been paid at the shop. */
    @PatchMapping("/bills/{id}/payment")
    public BillDto markPaid(@PathVariable Long id, @Valid @RequestBody PaymentUpdateRequest body) {
        return billing.markPaid(id, body.paymentMethod(), body.amountReceived());
    }

    /** Printable PDF invoice of one bill. */
    @GetMapping(value = "/bills/{id}/invoice.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> invoice(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean download) {
        Bill bill = billing.entity(id);
        ContentDisposition cd = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("bill-" + bill.getBillNumber() + ".pdf").build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf.invoice(bill));
    }
}
