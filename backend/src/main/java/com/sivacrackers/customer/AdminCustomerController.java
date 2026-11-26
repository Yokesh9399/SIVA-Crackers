package com.sivacrackers.customer;

import com.sivacrackers.audit.AuditService;
import com.sivacrackers.catalog.product.ProductSpecifications;
import com.sivacrackers.common.Params;
import com.sivacrackers.common.exception.ConflictException;
import com.sivacrackers.common.exception.InvalidRequestException;
import com.sivacrackers.common.exception.NotFoundException;
import com.sivacrackers.security.CurrentUser;
import com.sivacrackers.user.Identifiers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shop customer contacts. Look-up and add (e.g. while billing): OWNER, MANAGER, CASHIER. Edit / delete: OWNER and MANAGER.
 * Purchase history comes from finalized in-store bills (staff only, never public).
 */
@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
public class AdminCustomerController {

    static final String PHONE = "^$|^(\\+?91[\\s-]?|0)?[6-9][0-9]{4}[\\s-]?[0-9]{5}$";

    public record CustomerRequest(
            @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters") String fullName,
            @Pattern(regexp = PHONE, message = "must be a 10-digit Indian mobile number") String phone,
            @Email(message = "must be a valid email address") @Size(max = 254, message = "is too long") String email,
            @Size(max = 300, message = "must be at most 300 characters") String address,
            @Size(max = 500, message = "must be at most 500 characters") String notes) {
    }

    public record CustomerDto(Long id, String fullName, String phone, String email, String address, String notes,
                              Instant createdAt, Instant updatedAt) {

        static CustomerDto of(ShopCustomer c) {
            return new CustomerDto(c.getId(), c.getFullName(), c.getPhone(), c.getEmail(), c.getAddress(), c.getNotes(),
                    c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    private final ShopCustomerRepository customers;
    private final AuditService audit;
    private final com.sivacrackers.billing.BillingService billing;

    public AdminCustomerController(ShopCustomerRepository customers, AuditService audit,
                                   com.sivacrackers.billing.BillingService billing) {
        this.customers = customers;
        this.audit = audit;
        this.billing = billing;
    }

    /**
     * In-store purchase history: the customer's bills (newest first) with the total of FINALIZED bills
     * ({@code finalizedTotal}); voided bills are listed but never counted.
     */
    @GetMapping("/{id}/bills")
    public com.sivacrackers.billing.BillingDtos.BillPage bills(@PathVariable Long id,
                                                              @RequestParam(required = false) Integer page) {
        find(id);
        return billing.list(new com.sivacrackers.billing.BillingService.Filter(null, null, null, null, null, id),
                Params.page(page), 25);
    }

    /** Up to 200 contacts matching {@code ?search=} (name, phone or email). */
    @GetMapping
    @Transactional(readOnly = true)
    public List<CustomerDto> list(@RequestParam(required = false) String search) {
        String q = Params.search(search);
        String pattern = q == null || q.isEmpty() ? null : "%" + ProductSpecifications.escapeLike(q) + "%";
        return customers.search(pattern, PageRequest.of(0, 200)).stream().map(CustomerDto::of).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CustomerDto get(@PathVariable Long id) {
        return CustomerDto.of(find(id));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CustomerDto> create(@Valid @RequestBody CustomerRequest body) {
        ShopCustomer c = new ShopCustomer(body.fullName().trim(), CurrentUser.idOrNull());
        apply(c, body, null);
        ShopCustomer saved = customers.saveAndFlush(c);
        audit.record("CUSTOMER_CREATED", "CUSTOMER", saved.getId(), saved.getFullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerDto.of(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerRequest body) {
        ShopCustomer c = find(id);
        c.setFullName(body.fullName().trim());
        apply(c, body, id);
        ShopCustomer saved = customers.saveAndFlush(c);
        audit.record("CUSTOMER_UPDATED", "CUSTOMER", id, saved.getFullName());
        return CustomerDto.of(saved);
    }

    /** Removes the contact record completely (e.g. when the customer asks for it). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ShopCustomer c = find(id);
        customers.delete(c);
        audit.record("CUSTOMER_DELETED", "CUSTOMER", id, c.getFullName());
        return ResponseEntity.noContent().build();
    }

    private void apply(ShopCustomer c, CustomerRequest body, Long id) {
        String phone = Identifiers.phone(body.phone());
        String email = body.email() == null || body.email().isBlank() ? null : Identifiers.email(body.email());
        if (phone == null && email == null) {
            throw new InvalidRequestException("phone", "or email is required");
        }
        if (phone != null && (id == null ? customers.existsByPhone(phone) : customers.existsByPhoneAndIdNot(phone, id))) {
            throw new ConflictException("phone", "is already saved for another customer");
        }
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(blankToNull(body.address()));
        c.setNotes(blankToNull(body.notes()));
    }

    private ShopCustomer find(Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer not found."));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
